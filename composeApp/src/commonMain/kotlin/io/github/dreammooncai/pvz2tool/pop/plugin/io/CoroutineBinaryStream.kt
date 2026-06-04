// CoroutineBinaryStream.kt
package io.github.dreammooncai.pvz2tool.pop.plugin.io

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.EOFException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import java.nio.channels.FileChannel
import java.util.Objects
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

private data class StreamDomain(
    @Volatile var baseOffset: Long = 0L,
    @Volatile var readPos: Long = 0L,
    @Volatile var writePos: Long = 0L,
    @Volatile var length: Long = 0L
)

// ====================== 【核心新增】共享资源管理器（引用计数） ======================
// 所有根流/子流共享同一个资源，计数为0时自动关闭后端
private class StreamResource(
    val backend: StorageBackend,
    var fileBinary: File?
) {
    // 原子引用计数：初始为1（当前实例持有）
    private val refCount = AtomicInteger(1)
    private val closed = AtomicBoolean(false)

    // 创建子流时：增加引用
    fun acquire() {
        if (closed.get()) throw IllegalStateException("流资源已关闭，无法创建子流")
        refCount.incrementAndGet()
    }

    // 关闭流时：减少引用，返回是否真正释放了资源
    fun release(): Boolean {
        if (closed.get()) return false
        val count = refCount.decrementAndGet()
        if (count == 0) {
            closed.set(true)
            backend.close()
            return true
        }
        return false
    }
}

private const val INITIALIZE_THE_BLOCK_AREA_SIZE = 128

/**
 * 协程优化版二进制流（Hybrid 双后端自动切换）
 * 小文件使用内存 ByteBuffer，大文件自动切换至临时或绑定的文件系统 (FileChannel + MMap)
 * 彻底解决大块内存分配导致的 OOM 问题
 * ✅ 新增：引用计数关闭，所有子流关闭后自动释放资源
 */
class CoroutineBinaryStream private constructor(
    private val resource: StreamResource,
    private val domain: StreamDomain,
) : AbstractCoroutineBinaryStream() {

    private var isClosed = false
    private val writeMutex = Mutex()
    private val currentDomain: StreamDomain get() = domain

    // ====================== 快捷访问（兼容原有代码） ======================
    private val backend: StorageBackend get() = resource.backend
    private var fileBinary: File?
        get() = resource.fileBinary
        set(value) { resource.fileBinary = value }

    override var endian: Endian
        get() = super.endian
        set(value) {
            backend.endian = value
            super.endian = value
        }

    fun <T> array(): T? = backend.array()

    // ====================== 构造函数 ======================

    private constructor(backend: StorageBackend, fileBinary: File? = null, domain: StreamDomain = StreamDomain()) : this(
        StreamResource(backend, fileBinary), domain
    )

    // 用于 Slice / Domain 的私有构造（子流：共享资源，引用+1）
    private constructor(
        parentResource: StreamResource,
        domain: StreamDomain,
        encode: String,
        endian: Endian,
        stringEndian: Endian
    ) : this(parentResource.also { it.acquire() }, domain) {
        this.encode = encode
        this.endian = endian
        this.stringEndian = stringEndian
    }

    // ====================== 实现基类抽象属性 ======================
    override var readPosition: Long
        get() = currentDomain.readPos
        set(value) {
            require(value in 0..currentDomain.length) { "读指针越界" }
            currentDomain.readPos = value
        }

    override val length: Long get() = currentDomain.length
    val writePosition: Long get() = currentDomain.writePos

    // ====================== 容量管理 ======================
    suspend fun setLength(newLength: Long, isPreciseExpansion: Boolean = false) = writeMutex.withLock {
        require(newLength >= 0) { "长度不能为负数" }
        if (newLength > currentDomain.length) {
            backend.ensureCapacity(currentDomain.baseOffset + newLength, currentDomain.length, isPreciseExpansion)
        }
        currentDomain.length = newLength
        if (currentDomain.writePos > newLength) currentDomain.writePos = newLength
    }

    private suspend fun seekWrite(pos: Long) {
        require(pos >= 0) { "写指针不能为负数" }
        if (pos > currentDomain.length) setLength(pos)
        writeMutex.withLock { currentDomain.writePos = pos }
    }

    suspend fun setWritePosition(value: Long) = seekWrite(value)
    suspend fun setReadWritePosition(position: Long) {
        readPosition = position
        setWritePosition(position)
    }

    // ====================== 实现基类核心原语 ======================
    override fun coreRead(dst: ByteArray, offset: Int, count: Int) {
        val readPos = currentDomain.readPos
        val absPos = currentDomain.baseOffset + readPos
        backend.read(absPos, dst, offset, count)
        currentDomain.readPos = readPos + count
    }

    override fun coreRead(dst: IntArray, offset: Int, count: Int, endian: Endian) {
        val readPos = currentDomain.readPos
        val absPos = currentDomain.baseOffset + readPos
        backend.read(absPos, dst, offset, count,endian)
        currentDomain.readPos = readPos + count * 4
    }

    override suspend fun coreWrite(src: ByteArray, offset: Int, count: Int) {
        val writePos = currentDomain.writePos
        val end = writePos + count
        if (end > currentDomain.length) setLength(end)

        val absPos = currentDomain.baseOffset + writePos
        backend.write(absPos, src, offset, count)
        currentDomain.writePos = writePos + count
    }

    override suspend fun coreWrite(src: ByteBuffer) {
        val count = src.remaining()
        if (count == 0) return

        val writePos = currentDomain.writePos
        val end = writePos + count
        if (end > currentDomain.length) setLength(end)

        val absPos = currentDomain.baseOffset + writePos
        backend.write(absPos, src)
        currentDomain.writePos = writePos + count
    }

    override suspend fun coreWrite(src: IntArray, offset: Int, count: Int, endian: Endian) {
        val writePos = currentDomain.writePos
        val end = writePos + count * 4
        if (end > currentDomain.length) setLength(end)

        val absPos = currentDomain.baseOffset + writePos
        backend.write(absPos, src, offset, count,endian)
        currentDomain.writePos = writePos + count * 4
    }

    override suspend fun coreWrite(src: IntBuffer, endian: Endian) {
        val count = src.remaining()
        if (count == 0) return

        val writePos = currentDomain.writePos
        val end = writePos + count * 4
        if (end > currentDomain.length) setLength(end)

        val absPos = currentDomain.baseOffset + writePos
        backend.write(absPos, src,endian)
        currentDomain.writePos = writePos + count * 4
    }

    // ====================== 子类特有功能 (Slice/File) ======================
    fun slice(offset: Long, length: Long): CoroutineBinaryStream {
        require(offset >= 0 && length >= 0) { "参数不能为负数" }
        require(offset + length <= currentDomain.length) { "切片超出流范围" }

        val newDomain = StreamDomain(
            baseOffset = currentDomain.baseOffset + offset,
            length = length
        )

        // 子流：共享资源
        return CoroutineBinaryStream(
            parentResource = resource,
            domain = newDomain,
            encode = encode,
            endian = endian,
            stringEndian = stringEndian
        )
    }

    fun slice(length: Long): CoroutineBinaryStream = slice(readPosition, length)

    fun createDomain(
        readPos: Long = 0,
        writePos: Long = 0,
        length: Long = currentDomain.length
    ): CoroutineBinaryStream {
        val newDomain = StreamDomain(
            baseOffset = currentDomain.baseOffset,
            readPos = readPos,
            writePos = writePos,
            length = length
        )
        // 子域：共享资源
        return CoroutineBinaryStream(resource, newDomain, encode, endian, stringEndian)
    }

    fun readSafeAt(offset: Long, count: Int): ByteArray {
        if (offset < 0 || offset + count > currentDomain.length) throw EOFException("读取越界")
        val absPos = currentDomain.baseOffset + offset
        val bytes = ByteArray(count)
        backend.read(absPos, bytes, 0, count)
        return bytes
    }

    suspend fun writeAt(offset: Long, bytes: ByteArray) {
        require(offset >= 0)
        val end = offset + bytes.size
        if (end > currentDomain.length) setLength(end)
        val absPos = currentDomain.baseOffset + offset
        backend.write(absPos, bytes, 0, bytes.size)
    }

    override fun toByteArray(): ByteArray {
        val bytes = ByteArray((currentDomain.length - currentDomain.readPos).toInt())
        backend.read(currentDomain.baseOffset + currentDomain.readPos, bytes, 0, bytes.size)
        return bytes
    }

    override fun toBuffer(): ByteBuffer {
        val absPos = currentDomain.baseOffset + currentDomain.readPos
        val len = (currentDomain.length - currentDomain.readPos).toInt()
        return backend.toByteBuffer(absPos, len)
    }

    fun inputStream(): InputStream {
        // 核心：用 slice() 创建独立视图，从当前 readPosition 开始，不影响原流
        val viewStream = slice(readPosition, length - readPosition)

        return object : InputStream() {
            private val stream = viewStream
            private var closed = false

            private fun ensureOpen() {
                if (closed) throw IOException("Stream closed")
            }

            override fun read(): Int {
                ensureOpen()
                if (stream.readPosition >= stream.length) return -1
                return stream.readByte().toInt() and 0xFF
            }

            override fun read(b: ByteArray, off: Int, len: Int): Int {
                ensureOpen()
                Objects.checkFromIndexSize(off, len, b.size)
                if (len == 0) return 0

                val remaining = stream.length - stream.readPosition
                if (remaining <= 0) return -1

                val toRead = minOf(len.toLong(), remaining).toInt()
                stream.read(b, off, toRead)
                return toRead
            }

            override fun skip(n: Long): Long {
                ensureOpen()
                if (n < 0) throw IllegalArgumentException("negative skip length")

                val remaining = stream.length - stream.readPosition
                if (remaining <= 0) return 0

                val toSkip = minOf(n, remaining)
                stream.readPosition += toSkip
                return toSkip
            }

            override fun available(): Int {
                ensureOpen()
                val remaining = stream.length - stream.readPosition
                return if (remaining > Int.MAX_VALUE) Int.MAX_VALUE else remaining.toInt()
            }

            override fun markSupported(): Boolean = false

            override fun close() {
                if (closed) return
                closed = true
                stream.close() // 关闭子流（引用计数-1，不影响原流）
            }
        }
    }

    fun inputStreamAndClose(): InputStream = use { it.inputStream() }

    // ====================== 保存文件（保留你的移动优化） ======================
    suspend fun saveFile(
        targetFile: File? = this.fileBinary,
        moveIfPossible: Boolean = true
    ) = withContext(Dispatchers.IO) {
        val file = targetFile ?: return@withContext
        file.parentFile?.mkdirs()

        if (moveIfPossible && (resource.backend as? HybridBackend)?.isFileBackend == true && currentDomain.baseOffset == 0L) {
            val sourceFile = resource.backend.currentFile ?: return@withContext
            try {
                val actualDataSize = currentDomain.length
                val tempFileSize = sourceFile.length()

                if (tempFileSize > actualDataSize) {
                    backend.truncate(actualDataSize)
                }

                if (file.exists()) file.delete()
                val moveSuccess = sourceFile.renameTo(file)
                if (moveSuccess) {
                    fileBinary = file
                    resource.backend.fileBackend?.close()
                    resource.backend.fileBackend = FileBackend(file, isTemp = false)
                    return@withContext
                }
            } catch (e: Exception) {}
        }

        backend.copyTo(file, currentDomain.baseOffset, currentDomain.length)
    }

    suspend fun saveFile(path: String?, moveIfPossible: Boolean = false) =
        saveFile(if (path == null) null else File(path), moveIfPossible)

    fun copyTo(file: File) {
        file.parentFile?.mkdirs()
        backend.copyTo(file, currentDomain.baseOffset, currentDomain.length)
    }

    fun copyTo(filePath: String) = copyTo(File(filePath))

    suspend fun copyTo(s: CoroutineBinaryStream) = copyTo(s, currentDomain.length)
    suspend fun copyTo(s: CoroutineBinaryStream, length: Long) {
        val actualLength = minOf(this.length - readPosition, length).toInt()
        if (actualLength <= 0) return
        val buffer = toBuffer().apply { limit(position() + actualLength) }
        s.write(buffer)
    }

    // ====================== 【核心重构】关闭逻辑 ======================
    override fun close() {
        if (isClosed) return
        isClosed = true
        resource.release()
    }

    // ====================== 伴生对象 ======================
    companion object {
        /**
         * 【纯流式只读】从可重开的 InputStream 创建流
         * 要求：必须传入 reopenStream 函数以支持随机 seek
         */
        fun openStream(
            length: Long?,
            reopenStream: () -> InputStream
        ): CoroutineBinaryStream {
            val backend = InputStreamBackend(reopenStream)
            val length = length ?: backend.length.toLong()
            backend.length = length.toInt()
            return CoroutineBinaryStream(
                backend,
                domain = StreamDomain(length = length)
            )
        }

        fun create(filePath: String?, size: Int = INITIALIZE_THE_BLOCK_AREA_SIZE): CoroutineBinaryStream = create(filePath?.let { File(it) }, size)

        fun open(filePath: String): CoroutineBinaryStream = open(File(filePath))

        fun modify(filePath: String): CoroutineBinaryStream = modify(File(filePath))

        /**
         * 【只读打开】
         * 内部使用 FileBackend("r")。
         * 如果你只是读，它不产生任何临时文件，不占堆内存。
         * 如果你调用了 alignFourK()，它会自动拷贝到临时文件，原文件绝对不会变长。
         */
        fun open(file: File) = CoroutineBinaryStream(
            HybridBackend(file, readOnly = true),
            file,
            StreamDomain(length = file.length())
        )

        /**
         * 【创建模式】。
         * 初始为空，数据在内存或临时文件中，saveFile(file) 时才写入磁盘。
         */
        fun create(fileBinary: File? = null, size: Int = fileBinary?.length()?.toInt()?.coerceAtLeast(INITIALIZE_THE_BLOCK_AREA_SIZE) ?: INITIALIZE_THE_BLOCK_AREA_SIZE): CoroutineBinaryStream {
            val stream = allocateBytes(size) // 默认内存后端
            stream.fileBinary = fileBinary
            return stream
        }

        /**
         * 【修改模式】最危险但也最快。
         * 直接对原文件进行读写。alignFourK 会立即改变磁盘文件大小。
         */
        fun modify(file: File) = CoroutineBinaryStream(
            HybridBackend(file, readOnly = false),
            file,
            StreamDomain(length = file.length())
        )

        fun allocateBytes(size: Int = INITIALIZE_THE_BLOCK_AREA_SIZE): CoroutineBinaryStream = CoroutineBinaryStream(HybridBackend.allocateBytes(size))

        fun allocateInts(size: Int = INITIALIZE_THE_BLOCK_AREA_SIZE): CoroutineBinaryStream = CoroutineBinaryStream(HybridBackend.allocateInts(size))

        fun wrap(array: ByteArray): CoroutineBinaryStream = CoroutineBinaryStream(HybridBackend.wrap(array), domain = StreamDomain(writePos = array.size.toLong(), length = array.size.toLong()))

        fun wrap(array: IntArray): CoroutineBinaryStream = CoroutineBinaryStream(HybridBackend.wrap(array), domain = StreamDomain(writePos = array.size.toLong() * 4, length = array.size.toLong() * 4))
    }
}

// ====================== 存储后端抽象与实现 (无修改) ======================
private interface StorageBackend : AutoCloseable {
    var endian:Endian

    fun read(absPos: Long, dst: ByteArray, offset: Int, count: Int)
    fun read(absPos: Long, dst: IntArray, offset: Int, count: Int,endian: Endian)
    fun write(absPos: Long, src: ByteArray, offset: Int, count: Int)
    fun write(absPos: Long, src: IntArray, offset: Int, count: Int,endian: Endian)
    fun write(absPos: Long, src: ByteBuffer)
    fun write(absPos: Long, src: IntBuffer,endian: Endian)
    fun ensureCapacity(required: Long, usedLength: Long, isPrecise: Boolean)
    fun truncate(length: Long)
    fun toByteBuffer(absPos: Long, length: Int): ByteBuffer
    fun copyTo(target: File, offset: Long, length: Long)

    fun <T> array(): T?
}

private class MemoryBackend(
    var buffer: Buffer
) : StorageBackend {

    override var endian: Endian = Endian.Small
        set(value) {
            (buffer as? ByteBuffer)?.order(value.byteOrder)
            field = value
        }

    private var currentCapacity = buffer.capacity().toLong() * when (buffer) {
        is ByteBuffer -> 1
        is IntBuffer -> 4 // IntBuffer 容量单位是 Int，这里统一按字节记录
        else -> 1
    }

    init {
        (buffer as? ByteBuffer)?.order(endian.byteOrder)
    }


    override fun read(absPos: Long, dst: ByteArray, offset: Int, count: Int) {
        when (val dup = buffer.duplicateCompat()) {
            is ByteBuffer -> {
                dup.position(absPos.toInt())
                dup.get(dst, offset, count)
            }
            is IntBuffer -> {
                val intStartPos = (absPos / 4).toInt()
                val intCount = count / 4
                val intArray = IntArray(intCount)

                // 先读取int数组
                dup.position(intStartPos)
                dup.get(intArray, 0, intCount)

                // 按IntBuffer自身的固定端序，转换为ByteArray
                val bufferOrder = dup.order()
                intArray.forEachIndexed { index, intValue ->
                    val byteOffset = offset + index * 4
                    if (bufferOrder == ByteOrder.LITTLE_ENDIAN) {
                        // 小端序：低位字节在前
                        dst[byteOffset] = (intValue and 0xFF).toByte()
                        dst[byteOffset + 1] = (intValue shr 8 and 0xFF).toByte()
                        dst[byteOffset + 2] = (intValue shr 16 and 0xFF).toByte()
                        dst[byteOffset + 3] = (intValue shr 24 and 0xFF).toByte()
                    } else {
                        // 大端序：高位字节在前
                        dst[byteOffset] = (intValue shr 24 and 0xFF).toByte()
                        dst[byteOffset + 1] = (intValue shr 16 and 0xFF).toByte()
                        dst[byteOffset + 2] = (intValue shr 8 and 0xFF).toByte()
                        dst[byteOffset + 3] = (intValue and 0xFF).toByte()
                    }
                }
            }
            else -> error("Unsupported Buffer implementation: ${dup::class.simpleName}")
        }
    }

    override fun read(absPos: Long, dst: IntArray, offset: Int, count: Int, endian: Endian) {
        val targetOrder = if (endian.isNull) this.endian.byteOrder else endian.byteOrder
        when (val dup = buffer.duplicateCompat()) {
            is ByteBuffer -> {
                dup.order(targetOrder)
                dup.position(absPos.toInt())
                dup.asIntBuffer().get(dst, offset, count)
            }
            is IntBuffer -> {
                // IntBuffer端序固定，仅当目标端序与自身一致时可直接读取
                val bufferOrder = dup.order()
                val intStartPos = (absPos / 4).toInt()
                dup.position(intStartPos)

                if (bufferOrder == targetOrder) {
                    // 端序一致，直接批量读取
                    dup.get(dst, offset, count)
                } else {
                    // 端序不一致，逐个读取并转换字节序
                    for (i in 0 until count) {
                        val rawInt = dup.get(intStartPos + i)
                        dst[offset + i] = rawInt.reverseBytes()
                    }
                }
            }
            else -> error("Unsupported Buffer implementation: ${dup::class.simpleName}")
        }
    }

    override fun write(absPos: Long, src: ByteArray, offset: Int, count: Int) {
        when (val dup = buffer.duplicateCompat()) {
            is ByteBuffer -> {
                dup.position(absPos.toInt())
                dup.put(src, offset, count)
            }
            is IntBuffer -> {
                val intStartPos = (absPos / 4).toInt()
                val intCount = count / 4
                val intArray = IntArray(intCount)
                val bufferOrder = dup.order()

                // 按IntBuffer自身的固定端序，将ByteArray转换为IntArray
                for (i in 0 until intCount) {
                    val byteOffset = offset + i * 4
                    intArray[i] = if (bufferOrder == ByteOrder.LITTLE_ENDIAN) {
                        // 小端序转换
                        (src[byteOffset].toInt() and 0xFF) or
                                (src[byteOffset + 1].toInt() and 0xFF shl 8) or
                                (src[byteOffset + 2].toInt() and 0xFF shl 16) or
                                (src[byteOffset + 3].toInt() and 0xFF shl 24)
                    } else {
                        // 大端序转换
                        (src[byteOffset].toInt() and 0xFF shl 24) or
                                (src[byteOffset + 1].toInt() and 0xFF shl 16) or
                                (src[byteOffset + 2].toInt() and 0xFF shl 8) or
                                (src[byteOffset + 3].toInt() and 0xFF)
                    }
                }

                // 写入IntBuffer
                dup.position(intStartPos)
                dup.put(intArray, 0, intCount)
            }
            else -> error("Unsupported Buffer implementation: ${dup::class.simpleName}")
        }
    }

    override fun write(absPos: Long, src: ByteBuffer) {
        when (val dup = buffer.duplicateCompat()) {
            is ByteBuffer -> {
                dup.position(absPos.toInt())
                dup.put(src)
            }
            is IntBuffer -> {
                if (src.hasArray())
                    write(absPos, src.array(), 0, src.remaining())
                else {
                    val count = src.remaining()
                    val tempBytes = ByteArray(count)
                    src.duplicateCompat().get(tempBytes) // 安全拷贝
                    write(absPos, tempBytes, 0, count)
                }
            }
            else -> error("Unsupported Buffer implementation: ${dup::class.simpleName}")
        }
    }

    override fun write(absPos: Long, src: IntArray, offset: Int, count: Int, endian: Endian) {
        val targetOrder = if (endian.isNull) this.endian.byteOrder else endian.byteOrder
        when (val dup = buffer.duplicateCompat()) {
            is ByteBuffer -> {
                dup.order(targetOrder)
                dup.position(absPos.toInt())
                dup.asIntBuffer().put(src, offset, count)
            }
            is IntBuffer -> {
                val bufferOrder = dup.order()
                val intStartPos = (absPos / 4).toInt()
                dup.position(intStartPos)

                if (bufferOrder == targetOrder) {
                    // 端序一致，直接批量写入
                    dup.put(src, offset, count)
                } else {
                    // 端序不一致，逐个转换字节序后写入
                    for (i in 0 until count) {
                        val rawInt = src[offset + i]
                        dup.put(intStartPos + i, rawInt.reverseBytes())
                    }
                }
            }
            else -> error("Unsupported Buffer implementation: ${dup::class.simpleName}")
        }
    }

    override fun write(absPos: Long, src: IntBuffer, endian: Endian) {
        val targetOrder = if (endian.isNull) this.endian.byteOrder else endian.byteOrder
        when (val dup = buffer.duplicateCompat()) {
            is ByteBuffer -> {
                dup.order(targetOrder)
                dup.position(absPos.toInt())
                dup.asIntBuffer().put(src)
            }
            is IntBuffer -> {
                val count = src.remaining()
                val bufferOrder = dup.order()
                val intStartPos = (absPos / 4).toInt()
                dup.position(intStartPos)

                if (bufferOrder == targetOrder && src.order() == bufferOrder) {
                    // 端序完全一致，直接批量写入
                    dup.put(src)
                }  else {
                    // 端序不一致，逐个读取转换后写入
                    val srcCopy = src.duplicateCompat()
                    for (i in 0 until count) {
                        val rawInt = srcCopy.get()
                        val finalInt = if (src.order() != bufferOrder) rawInt.reverseBytes() else rawInt
                        dup.put(intStartPos + i, finalInt)
                    }
                }
            }
            else -> error("Unsupported Buffer implementation: ${dup::class.simpleName}")
        }
    }

    override fun ensureCapacity(required: Long, usedLength: Long, isPrecise: Boolean) {
        val oldCapacity = currentCapacity
        if (required <= oldCapacity) return

        // 扩容算法与你原有逻辑完全一致
        val newCapacity = if (isPrecise) required else {
            if (oldCapacity < 1024 * 1024) maxOf(oldCapacity * 2, required)
            else maxOf(oldCapacity + (oldCapacity shr 1), required)
        }

        // 区分Buffer类型，计算元素级容量
        val bufOldUsedLen = when (buffer) {
            is ByteBuffer -> usedLength.toInt()
            is IntBuffer -> (usedLength / 4).toInt()
            else -> error("Unsupported Buffer implementation")
        }
        val bufNewCapacity = when (buffer) {
            is ByteBuffer -> newCapacity.toInt()
            is IntBuffer -> (newCapacity / 4).toInt()
            else -> error("Unsupported Buffer implementation")
        }


        // 复制原有数据
        val src = buffer.duplicateCompat().apply {
            position(0)
            limit(bufOldUsedLen)
        }

        // 创建新Buffer，保持端序一致性
        val newBuf = when (src) {
            is ByteBuffer -> ByteBuffer.allocate(bufNewCapacity).order(endian.byteOrder).put(src)
            is IntBuffer -> IntBuffer.allocate(bufNewCapacity).put(src)
            else -> error("Unsupported Buffer implementation")
        }
        // 重置position为0，方便后续操作
        newBuf.clear()

        // 更新全局变量
        buffer = newBuf
        currentCapacity = newCapacity
    }

    override fun toByteBuffer(absPos: Long, length: Int): ByteBuffer {
        return when (val buf = buffer) {
            is ByteBuffer -> {
                buf.duplicateCompat().apply {
                    position(absPos.toInt())
                    limit(absPos.toInt() + length)
                }.slice().order(endian.byteOrder)
            }
            is IntBuffer -> {
                // 读取对应范围的字节数据，写入新的ByteBuffer
                val byteArray = ByteArray(length)
                read(absPos, byteArray, 0, length)
                // 新ByteBuffer使用IntBuffer自身的固定端序
                ByteBuffer.wrap(byteArray).order(endian.byteOrder)
            }
            else -> error("Unsupported Buffer implementation: ${buf::class.simpleName}")
        }
    }

    override fun truncate(length: Long) {
        if (length >= currentCapacity) return

        // 计算截断后的元素级长度
        val bufNewLen = when (buffer) {
            is ByteBuffer -> length.toInt()
            is IntBuffer -> (length / 4).toInt()
            else -> error("Unsupported Buffer implementation")
        }
        // 复制有效数据
        val src = buffer.duplicateCompat().apply {
            position(0)
            limit(bufNewLen)
        }

        // 创建截断后的新Buffer
        val newBuf = when (src) {
            is ByteBuffer -> ByteBuffer.allocate(bufNewLen).order(endian.byteOrder).put(src)
            is IntBuffer -> IntBuffer.allocate(bufNewLen).put(src)
            else -> error("Unsupported Buffer implementation")
        }
        newBuf.clear()

        // 更新全局变量
        buffer = newBuf
        currentCapacity = length
    }

    override fun copyTo(target: File, offset: Long, length: Long) {
        FileOutputStream(target).channel.use { out ->
            val src = toByteBuffer(offset, length.toInt())
            while (src.hasRemaining()) out.write(src)
        }
    }

    override fun <T> array(): T? = when (val buffer = buffer) {
        is ByteBuffer -> buffer.array()
        is IntBuffer -> buffer.array()
        else -> error("Unsupported Buffer implementation: ${buffer::class.simpleName}")
    } as? T?

    override fun close() {
        currentCapacity = 0
    }

    private fun Int.reverseBytes(): Int {
        return (this and 0xFF shl 24) or
                (this and 0xFF00 shl 8) or
                (this and 0xFF0000 shr 8) or
                (this ushr 24 and 0xFF)
    }
}

private class FileBackend(
    val file: File,
    private val isTemp: Boolean,
    val isReadOnly: Boolean = false,
) : StorageBackend {

    private val raf = RandomAccessFile(file, if (isReadOnly) "r" else "rw")
    private val channel = raf.channel
    private var map: Lazy<ByteBuffer>? = null

    override var endian: Endian = Endian.Small
        set(value) {
            map?.value?.order(value.byteOrder)
            field = value
        }

    private var currentCapacity = raf.length()

    private fun getMap(endian: Endian): ByteBuffer {
        if (map == null) {
            remap(currentCapacity)
        }
        return map!!.value.duplicateCompat().order(if (endian.isNull) this.endian.byteOrder else endian.byteOrder)
    }

    private fun remap(size: Long) {
        map = lazy {
            channel.map(
                if (isReadOnly) FileChannel.MapMode.READ_ONLY else FileChannel.MapMode.READ_WRITE,
                0, size
            ).order(endian.byteOrder)
        }
    }

    // ====================== 读取 ======================
    override fun read(absPos: Long, dst: ByteArray, offset: Int, count: Int) {
        val buf = getMap(endian)
        buf.position(absPos.toInt())
        buf.get(dst, offset, count)
    }

    override fun read(absPos: Long, dst: IntArray, offset: Int, count: Int,endian: Endian) {
        val buf = getMap(endian)
        buf.position(absPos.toInt())
        buf.asIntBuffer().get(dst, offset, count) // 字节序已正确
    }

    // ====================== 写入 ======================
    override fun write(absPos: Long, src: ByteArray, offset: Int, count: Int) {
        if (isReadOnly) throw IllegalStateException("只读模式无法写入")
        val buf = getMap(endian)
        buf.position(absPos.toInt())
        buf.put(src, offset, count)
    }

    override fun write(absPos: Long, src: IntArray, offset: Int, count: Int,endian: Endian) {
        if (isReadOnly) throw IllegalStateException("只读模式无法写入")
        val buf = getMap(endian)
        buf.position(absPos.toInt())
        buf.asIntBuffer().put(src, offset, count) // 字节序已正确
    }

    override fun write(absPos: Long, src: ByteBuffer) {
        if (isReadOnly) throw IllegalStateException("只读模式无法写入")
        val buf = getMap(endian)
        buf.position(absPos.toInt())
        buf.put(src.duplicateCompat())
    }

    override fun write(absPos: Long, src: IntBuffer,endian: Endian) {
        if (isReadOnly) throw IllegalStateException("只读模式无法写入")
        val buf = getMap(endian)
        buf.position(absPos.toInt())
        buf.asIntBuffer().put(src.duplicateCompat())
    }

    // ====================== 容量管理 ======================
    override fun ensureCapacity(required: Long, usedLength: Long, isPrecise: Boolean) {
        if (isReadOnly) return
        val oldCapacity = currentCapacity
        if (required <= oldCapacity) return

        // 扩容算法与你原有逻辑完全一致
        val newCapacity = if (isPrecise) required else {
            if (oldCapacity < 1024 * 1024) maxOf(oldCapacity * 2, required)
            else maxOf(oldCapacity + (oldCapacity shr 1), required)
        }

        raf.setLength(newCapacity)
        remap(newCapacity) // 重建 map
        currentCapacity = newCapacity
    }

    override fun truncate(length: Long) {
        if (isReadOnly) return
        if (length >= currentCapacity) return
        raf.setLength(length)
        remap(length)
        currentCapacity = length
    }

    // ====================== 工具方法 ======================
    override fun toByteBuffer(absPos: Long, length: Int): ByteBuffer {
        val buf = getMap(endian)
        buf.position(absPos.toInt())
        buf.limit(absPos.toInt() + length)
        return buf.slice()
    }

    override fun copyTo(target: File, offset: Long, length: Long) {
        FileOutputStream(target).channel.use { out ->
            channel.transferTo(offset, length, out)
        }
    }

    override fun <T> array(): T? = null

    override fun close() {
        channel.close()
        raf.close()
        if (isTemp) file.delete()
    }
}

private class HybridBackend : StorageBackend {
    private var memoryBackend: MemoryBackend? = null
    var fileBackend: FileBackend? = null

    // 标记当前后端是否处于只读状态（如果是从 open 打开的）
    private var isCoWPending: Boolean = false

    override var endian:Endian
        get() = (fileBackend ?: memoryBackend!!).endian
        set(value) {
            (fileBackend ?: memoryBackend!!).endian = value
        }

    private constructor()

    companion object {
        fun allocateBytes(initialSize: Int): HybridBackend = HybridBackend().apply {
            if (MemoryLifecycleManager.isUnderPressure(initialSize.toLong())) {
                val temp = createTemp()
                fileBackend = FileBackend(temp, isTemp = true).apply {
                    ensureCapacity(initialSize.toLong(),initialSize.toLong(),true)
                }
            } else {
                memoryBackend = MemoryBackend(ByteBuffer.allocate(initialSize))
            }
        }
        fun allocateInts(initialSize: Int): HybridBackend = HybridBackend().apply {
            if (MemoryLifecycleManager.isUnderPressure(initialSize.toLong())) {
                val temp = createTemp()
                fileBackend = FileBackend(temp, isTemp = true).apply {
                    ensureCapacity(initialSize.toLong(),initialSize.toLong(),true)
                }
            } else {
                memoryBackend = MemoryBackend(IntBuffer.allocate(initialSize))
            }
        }

        fun wrap(array: ByteArray): HybridBackend = HybridBackend().apply {
            memoryBackend = MemoryBackend(ByteBuffer.wrap(array))
        }

        fun wrap(array: IntArray): HybridBackend = HybridBackend().apply {
            memoryBackend = MemoryBackend(IntBuffer.wrap(array))
        }
    }

    constructor(file: File, readOnly: Boolean) {
        if (readOnly) {
            // 只读模式：不直接写原文件，标记 CoW
            fileBackend = FileBackend(file, isTemp = false, isReadOnly = true)
            isCoWPending = true
        } else {
            // 修改模式：直接写原文件
            fileBackend = FileBackend(file, isTemp = false)
            isCoWPending = false
        }
    }

    // ====================== 写时复制触发器 ======================

    @Synchronized
    private fun ensureWritable(requiredSize: Long, usedLength: Long) {
        // 如果是只读文件后端，触发“脱离”，拷贝数据到临时文件
        if (isCoWPending && fileBackend != null) {
            val tempFile = createTemp()
            val newFb = FileBackend(tempFile, isTemp = true)

            // 物理拷贝原有内容
            if (usedLength > 0) {
                fileBackend!!.copyTo(tempFile, 0, usedLength)
            }
            newFb.ensureCapacity(requiredSize,usedLength,true)
            fileBackend!!.close() // 关闭旧的只读句柄
            this.fileBackend = newFb
            this.isCoWPending = false // 彻底变为可写的临时文件
        }
    }

    private fun createTemp() = File.createTempFile("coroutine_stream_", ".tmp").apply { deleteOnExit() }

    val isFileBackend: Boolean get() = fileBackend != null
    val currentFile: File? get() = fileBackend?.file

    override fun read(absPos: Long, dst: ByteArray, offset: Int, count: Int) =
        (fileBackend ?: memoryBackend!!).read(absPos, dst, offset, count)

    override fun read(absPos: Long, dst: IntArray, offset: Int, count: Int,endian: Endian) =
        (fileBackend ?: memoryBackend!!).read(absPos, dst, offset, count,endian)

    override fun write(absPos: Long, src: ByteArray, offset: Int, count: Int) {
        ensureWritable(absPos + count, absPos) // 写入前先检查是否需要脱离只读
        (fileBackend ?: memoryBackend!!).write(absPos, src, offset, count)
    }

    override fun write(absPos: Long, src: ByteBuffer) {
        ensureWritable(absPos + src.remaining(), absPos)
        (fileBackend ?: memoryBackend!!).write(absPos, src)
    }

    override fun write(absPos: Long, src: IntArray, offset: Int, count: Int,endian: Endian) {
        ensureWritable(absPos + count * 4, absPos) // 写入前先检查是否需要脱离只读
        (fileBackend ?: memoryBackend!!).write(absPos, src, offset, count,endian)
    }

    override fun write(absPos: Long, src: IntBuffer,endian: Endian) {
        ensureWritable(absPos + src.remaining() * 4, absPos)
        (fileBackend ?: memoryBackend!!).write(absPos, src,endian)
    }

    @Synchronized
    override fun ensureCapacity(required: Long, usedLength: Long, isPrecise: Boolean) {
        if (isCoWPending && required > (fileBackend?.file?.length() ?: 0)) {
            ensureWritable(required, usedLength)
        }

        if (fileBackend != null) {
            fileBackend!!.ensureCapacity(required, usedLength, isPrecise)
            return
        }

        // 内存扩容判断：堆内 → 堆外 → 文件
        if (MemoryLifecycleManager.isUnderPressure(required)) {
            switchToFile(required, usedLength, isPrecise)
        } else {
            memoryBackend?.ensureCapacity(required, usedLength, isPrecise)
        }
    }

    private fun switchToFile(required: Long, usedLength: Long, isPrecise: Boolean) {
        val tempFile = File.createTempFile("coro_stream_", ".tmp").apply { deleteOnExit() }
        val fb = FileBackend(tempFile, true)
        fb.ensureCapacity(required, usedLength, isPrecise)

        if (usedLength > 0 && memoryBackend != null) {
            val oldData = memoryBackend!!.toByteBuffer(0, usedLength.toInt())
            fb.write(0, oldData)
        }

        memoryBackend?.close()

        this.fileBackend = fb
        this.memoryBackend = null
    }

    override fun truncate(length: Long) {
        // 截断也属于写操作
        if (isCoWPending) {
            val currentFileLen = fileBackend?.file?.length() ?: 0
            if (length < currentFileLen) ensureWritable(length, length)
            else return // 如果截断长度比原文件长且处于只读，没必要动作
        }
        (fileBackend ?: memoryBackend!!).truncate(length)
    }

    override fun toByteBuffer(absPos: Long, length: Int): ByteBuffer =
        (fileBackend ?: memoryBackend!!).toByteBuffer(absPos, length)

    override fun copyTo(target: File, offset: Long, length: Long) =
        (fileBackend ?: memoryBackend!!).copyTo(target, offset, length)

    override fun <T> array(): T? = memoryBackend?.array()

    override fun close() {
        memoryBackend?.close()
        fileBackend?.close()
    }
}

// ====================== 【精简版】纯 InputStream 后端 ======================
private class InputStreamBackend(
    private val reopenStream: () -> InputStream
) : StorageBackend {

    override var endian: Endian = Endian.Small

    // 当前流实例
    private data class StreamCache(
        val stream: InputStream,
        var pos: Long
    )

    private var cache: StreamCache = StreamCache(reopenStream(), 0)

    var length = cache.stream.available()

    // ====================== 核心读取实现 ======================
    override fun read(absPos: Long, dst: ByteArray, offset: Int, count: Int) {
        val stream = synchronized(this) {
            val currentCache = cache
            if (currentCache.pos <= absPos) {
                // 缓存命中：可以从当前位置 skip 过去
                var skipped = 0L
                while (skipped < absPos - currentCache.pos) {
                    skipped += currentCache.stream.skip(absPos - currentCache.pos - skipped)
                }
                currentCache.pos = absPos
                currentCache.stream
            } else {
                // 缓存未命中：重新打开流
                currentCache.stream.close()
                val newStream = reopenStream()
                var skipped = 0L
                while (skipped < absPos) {
                    skipped += newStream.skip(absPos - skipped)
                }
                cache = StreamCache(newStream, absPos)
                newStream
            }
        }

        // 读取数据（不在同步块内，避免阻塞）
        var totalRead = 0
        while (totalRead < count) {
            val read = stream.read(dst, offset + totalRead, count - totalRead)
            if (read == -1) throw EOFException("流已结束")
            totalRead += read
        }

        // 更新缓存位置
        synchronized(this) {
            cache.let { if (it.stream === stream) it.pos += totalRead }
        }
    }

    override fun read(absPos: Long, dst: IntArray, offset: Int, count: Int, endian: Endian) {
        val byteArray = ByteArray(count * 4)
        read(absPos, byteArray, 0, byteArray.size)
        ByteBuffer.wrap(byteArray)
            .order(if (endian.isNull) this.endian.byteOrder else endian.byteOrder)
            .asIntBuffer()
            .get(dst, offset, count)
    }

    // ====================== 不支持的操作 ======================
    override fun write(absPos: Long, src: ByteArray, offset: Int, count: Int) {
        throw UnsupportedOperationException("InputStreamBackend 是只读的")
    }

    override fun write(absPos: Long, src: IntArray, offset: Int, count: Int, endian: Endian) {
        throw UnsupportedOperationException("InputStreamBackend 是只读的")
    }

    override fun write(absPos: Long, src: ByteBuffer) {
        throw UnsupportedOperationException("InputStreamBackend 是只读的")
    }

    override fun write(absPos: Long, src: IntBuffer, endian: Endian) {
        throw UnsupportedOperationException("InputStreamBackend 是只读的")
    }

    override fun ensureCapacity(required: Long, usedLength: Long, isPrecise: Boolean) {}

    override fun truncate(length: Long) {
        throw UnsupportedOperationException("InputStreamBackend 是只读的")
    }

    // ====================== 工具方法 ======================
    override fun toByteBuffer(absPos: Long, length: Int): ByteBuffer {
        val byteArray = ByteArray(length)
        read(absPos, byteArray, 0, length)
        return ByteBuffer.wrap(byteArray).order(endian.byteOrder)
    }

    override fun copyTo(target: File, offset: Long, length: Long) {
        target.outputStream().use { output ->
            val buffer = ByteArray(8192)
            var remaining = length
            var currentOffset = offset
            while (remaining > 0) {
                val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                read(currentOffset, buffer, 0, toRead)
                output.write(buffer, 0, toRead)
                currentOffset += toRead
                remaining -= toRead
            }
        }
    }

    override fun <T> array(): T? = cache.stream as? T?

    override fun close() {
        synchronized(this) {
            cache.stream.close()
            cache.pos = -1
        }
    }
}