// AbstractCoroutineBinaryStream.kt
package io.github.dreammooncai.pvz2tool.pop.plugin.io

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.IntBuffer
import kotlin.experimental.and
import kotlin.experimental.or

/**
 * 协程二进制流基类（全功能模板）
 * 所有类型转换、Endian处理、ID校验、Peek逻辑全部在此实现。
 * 子类仅需实现最核心的读写原语。
 */
abstract class AbstractCoroutineBinaryStream: AutoCloseable {
    // ====================== 公共状态 ======================
    open var encode: String = "UTF-8"
    open var endian: Endian = Endian.Small
    open var stringEndian: Endian = Endian.Big

    // 公共缓冲区，提升到基类，子类共用
    protected val tempBuffer = ByteArray(128)

    // ====================== 核心抽象原语 (子类唯一需要实现的) ======================

    /** 当前读/写指针 (由子类维护具体存储) */
    abstract var readPosition: Long

    /** 流总长度 */
    abstract val length: Long

    /** 剩余可读长度 */
    open val remaining: Long get() = length - readPosition

    fun hasNext(): Boolean = remaining > 0

    /**
     * 核心读原语：读取 exactly [count] 个字节
     * 基类保证调用此方法时已经检查过越界，子类直接读即可。
     */
    protected abstract fun coreRead(dst: ByteArray, offset: Int, count: Int)

    /**
     * 核心写原语：写入 [count] 个字节
     */
    protected abstract suspend fun coreWrite(src: ByteArray, offset: Int, count: Int)

    /**
     * 核心写原语 (ByteBuffer 版本，可选优化)
     */
    protected abstract suspend fun coreWrite(src: ByteBuffer)

    /**
     * 核心读原语：读取 exactly [count] 个字节
     * 基类保证调用此方法时已经检查过越界，子类直接读即可。
     */
    protected abstract fun coreRead(dst: IntArray, offset: Int, count: Int, endian: Endian = Endian.Null)

    /**
     * 核心写原语：写入 [count] 个字节
     */
    protected abstract suspend fun coreWrite(src: IntArray, offset: Int, count: Int, endian: Endian = Endian.Null)

    /**
     * 核心写原语 (IntBuffer 版本，可选优化)
     */
    protected abstract suspend fun coreWrite(src: IntBuffer, endian: Endian = Endian.Null)

    // ====================== Helper: Endian 解析 ======================
    private fun rEndian(e: Endian) = if (e.isNull) this.endian else e
    private fun sEndian(e: Endian) = if (e.isNull) this.stringEndian else e

    // ====================== 基础填充与读取 (基于原语构建) ======================

    /** 填充 tempBuffer */
    protected fun fillBuffer(numBytes: Int) {
        if (remaining < numBytes) throw EOFException("流已结束")
        coreRead(tempBuffer, 0, numBytes)
    }

    /** 读取 ByteArray */
    fun readBytes(count: Int): ByteArray {
        val b = ByteArray(count)
        if (count > 0) coreRead(b, 0, count)
        return b
    }

    fun read(buffer: ByteArray, offset: Int, count: Int): Int {
        val rem = remaining.toInt()
        if (rem <= 0) return -1
        val actual = minOf(count, rem)
        coreRead(buffer, offset, actual)
        return actual
    }

    /** 读取 IntArray */
    fun readInts(count: Int, endian: Endian = Endian.Null): IntArray {
        val b = IntArray(count)
        if (count > 0) coreRead(b, 0, count,endian)
        return b
    }

    fun read(buffer: IntArray, offset: Int, count: Int, endian: Endian = Endian.Null): Int {
        val rem = remaining.toInt() / 4
        if (rem <= 0) return -1
        val actual = minOf(count, rem)
        coreRead(buffer, offset, actual,endian)
        return actual
    }

    fun read(): Int {
        if (remaining <= 0) return -1
        fillBuffer(1)
        return tempBuffer[0].toInt() and 0xFF
    }

    fun readExactly(buffer: ByteArray, offset: Int, count: Int) {
        var total = 0
        while (total < count) {
            val n = read(buffer, offset + total, count - total)
            if (n == -1) throw EOFException("流已结束")
            total += n
        }
    }

    // ====================== 基础类型读 API (全在基类实现) ======================

    // 8-bit
    fun readSByte(endian: Endian = Endian.Null): Byte { fillBuffer(1); return tempBuffer[0] }
    fun readInt8(endian: Endian = Endian.Null): Byte = readSByte(endian)
    fun readByte(endian: Endian = Endian.Null): Byte = readSByte(endian)
    fun readUInt8(endian: Endian = Endian.Null): UByte { fillBuffer(1); return tempBuffer[0].toUByte() }
    fun readBoolean(endian: Endian = Endian.Null): Boolean { fillBuffer(1); return tempBuffer[0] != 0.toByte() }
    fun readBool(endian: Endian = Endian.Null): Boolean = readBoolean(endian)

    // 16-bit
    fun readShort(endian: Endian = Endian.Null): Short = readInt16(endian)
    fun readInt16(endian: Endian = Endian.Null): Short {
        val e = rEndian(endian)
        fillBuffer(2)
        return if (e == Endian.Big) {
            ((tempBuffer[1].toShort() and 0xFF) or (tempBuffer[0].toInt() shl 8).toShort())
        } else {
            ((tempBuffer[0].toShort() and 0xFF) or (tempBuffer[1].toInt() shl 8).toShort())
        }
    }
    fun readUShort(endian: Endian = Endian.Null): UShort = readUInt16(endian)
    fun readUInt16(endian: Endian = Endian.Null): UShort {
        val e = rEndian(endian)
        fillBuffer(2)
        return if (e == Endian.Big) {
            ((tempBuffer[1].toUShort() and 0xFFU) or (tempBuffer[0].toUInt() shl 8).toUShort())
        } else {
            ((tempBuffer[0].toUShort() and 0xFFU) or (tempBuffer[1].toUInt() shl 8).toUShort())
        }
    }
    fun readChar(endian: Endian = Endian.Null): Char = readUInt16(endian).toInt().toChar()

    // 24-bit
    fun readThreeByte(endian: Endian = Endian.Null): Int = readInt24(endian)
    fun readInt24(endian: Endian = Endian.Null): Int {
        val i = readUInt24(endian)
        return if ((i and 0x800000u) != 0u) (i or 0xff000000u).toInt() else i.toInt()
    }
    fun readUThreeByte(endian: Endian = Endian.Null): UInt = readUInt24(endian)
    fun readUInt24(endian: Endian = Endian.Null): UInt {
        val e = rEndian(endian)
        fillBuffer(3)
        return if (e == Endian.Big) {
            ((tempBuffer[2].toUInt() and 0xFFU) or
                    ((tempBuffer[1].toUInt() and 0xFFU) shl 8) or
                    ((tempBuffer[0].toUInt() and 0xFFU) shl 16))
        } else {
            ((tempBuffer[0].toUInt() and 0xFFU) or
                    ((tempBuffer[1].toUInt() and 0xFFU) shl 8) or
                    ((tempBuffer[2].toUInt() and 0xFFU) shl 16))
        }
    }

    // 32-bit
    fun readInt(endian: Endian = Endian.Null): Int = readInt32(endian)
    fun readInt32(endian: Endian = Endian.Null): Int {
        val e = rEndian(endian)
        fillBuffer(4)
        return if (e == Endian.Big) {
            (tempBuffer[3].toInt() and 0xFF) or
                    ((tempBuffer[2].toInt() and 0xFF) shl 8) or
                    ((tempBuffer[1].toInt() and 0xFF) shl 16) or
                    ((tempBuffer[0].toInt() and 0xFF) shl 24)
        } else {
            (tempBuffer[0].toInt() and 0xFF) or
                    ((tempBuffer[1].toInt() and 0xFF) shl 8) or
                    ((tempBuffer[2].toInt() and 0xFF) shl 16) or
                    ((tempBuffer[3].toInt() and 0xFF) shl 24)
        }
    }
    fun readUInt(endian: Endian = Endian.Null): UInt = readUInt32(endian)
    fun readUInt32(endian: Endian = Endian.Null): UInt {
        val e = rEndian(endian)
        fillBuffer(4)
        return if (e == Endian.Big) {
            ((tempBuffer[3].toUInt() and 0xFFU) or
                    ((tempBuffer[2].toUInt() and 0xFFU) shl 8) or
                    ((tempBuffer[1].toUInt() and 0xFFU) shl 16) or
                    ((tempBuffer[0].toUInt() and 0xFFU) shl 24))
        } else {
            ((tempBuffer[0].toUInt() and 0xFFU) or
                    ((tempBuffer[1].toUInt() and 0xFFU) shl 8) or
                    ((tempBuffer[2].toUInt() and 0xFFU) shl 16) or
                    ((tempBuffer[3].toUInt() and 0xFFU) shl 24))
        }
    }
    fun readSingle(endian: Endian = Endian.Null): Float = readFloat32(endian)
    fun readFloat(endian: Endian = Endian.Null): Float = readFloat32(endian)
    fun readFloat32(endian: Endian = Endian.Null): Float {
        val e = rEndian(endian)
        val num = readUInt32(endian)
        return ByteBuffer.allocate(4).apply { order(e.byteOrder).putInt(num.toInt()).flip() }.float
    }

    // 64-bit
    fun readLong(endian: Endian = Endian.Null): Long = readInt64(endian)
    fun readInt64(endian: Endian = Endian.Null): Long {
        val e = rEndian(endian)
        fillBuffer(8)
        return if (e == Endian.Big) {
            val high = ((tempBuffer[3].toLong() and 0xFFL) or
                    ((tempBuffer[2].toLong() and 0xFFL) shl 8) or
                    ((tempBuffer[1].toLong() and 0xFFL) shl 16) or
                    ((tempBuffer[0].toLong() and 0xFFL) shl 24))
            val low = ((tempBuffer[7].toLong() and 0xFFL) or
                    ((tempBuffer[6].toLong() and 0xFFL) shl 8) or
                    ((tempBuffer[5].toLong() and 0xFFL) shl 16) or
                    ((tempBuffer[4].toLong() and 0xFFL) shl 24))
            (high shl 32) or (low and 0xFFFFFFFFL)
        } else {
            val low = ((tempBuffer[0].toLong() and 0xFFL) or
                    ((tempBuffer[1].toLong() and 0xFFL) shl 8) or
                    ((tempBuffer[2].toLong() and 0xFFL) shl 16) or
                    ((tempBuffer[3].toLong() and 0xFFL) shl 24))
            val high = ((tempBuffer[4].toLong() and 0xFFL) or
                    ((tempBuffer[5].toLong() and 0xFFL) shl 8) or
                    ((tempBuffer[6].toLong() and 0xFFL) shl 16) or
                    ((tempBuffer[7].toLong() and 0xFFL) shl 24))
            (high shl 32) or (low and 0xFFFFFFFFL)
        }
    }
    fun readULong(endian: Endian = Endian.Null): ULong = readUInt64(endian)
    fun readUInt64(endian: Endian = Endian.Null): ULong {
        val e = rEndian(endian)
        fillBuffer(8)
        return if (e == Endian.Big) {
            val high = ((tempBuffer[3].toULong() and 0xFFUL) or
                    ((tempBuffer[2].toULong() and 0xFFUL) shl 8) or
                    ((tempBuffer[1].toULong() and 0xFFUL) shl 16) or
                    ((tempBuffer[0].toULong() and 0xFFUL) shl 24))
            val low = ((tempBuffer[7].toULong() and 0xFFUL) or
                    ((tempBuffer[6].toULong() and 0xFFUL) shl 8) or
                    ((tempBuffer[5].toULong() and 0xFFUL) shl 16) or
                    ((tempBuffer[4].toULong() and 0xFFUL) shl 24))
            (high shl 32) or low
        } else {
            val low = ((tempBuffer[0].toULong() and 0xFFUL) or
                    ((tempBuffer[1].toULong() and 0xFFUL) shl 8) or
                    ((tempBuffer[2].toULong() and 0xFFUL) shl 16) or
                    ((tempBuffer[3].toULong() and 0xFFUL) shl 24))
            val high = ((tempBuffer[4].toULong() and 0xFFUL) or
                    ((tempBuffer[5].toULong() and 0xFFUL) shl 8) or
                    ((tempBuffer[6].toULong() and 0xFFUL) shl 16) or
                    ((tempBuffer[7].toULong() and 0xFFUL) shl 24))
            (high shl 32) or low
        }
    }
    fun readDouble(endian: Endian = Endian.Null): Double = readFloat64(endian)
    fun readFloat64(endian: Endian = Endian.Null): Double {
        val e = rEndian(endian)
        val num = readUInt64(endian)
        return ByteBuffer.allocate(8).apply { order(e.byteOrder).putLong(num.toLong()).flip() }.double
    }

    // VarInt / ZigZag
    fun readVarInt32(endian: Endian = Endian.Null): Int {
        var num = 0; var bit = 0; var b: UByte
        do {
            if (bit >= 35) throw IOException("VarInt 过大")
            b = readUInt8(endian)
            num = num or ((b.toInt() and 0x7F) shl bit)
            bit += 7
        } while ((b.toInt() and 0x80) != 0)
        return num
    }
    fun readVarInt64(endian: Endian = Endian.Null): Long {
        var num = 0L; var bit = 0; var b: UByte
        do {
            if (bit >= 70) throw IOException("VarInt 过大")
            b = readUInt8(endian)
            num = num or ((b.toLong() and 0x7F) shl bit)
            bit += 7
        } while ((b.toInt() and 0x80) != 0)
        return num
    }
    fun readUVarInt32(endian: Endian = Endian.Null): UInt = readVarInt32(endian).toUInt()
    fun readUVarInt64(endian: Endian = Endian.Null): ULong = readVarInt64(endian).toULong()
    fun readZigZag32(endian: Endian = Endian.Null): Int {
        val n = readVarInt32(endian).toUInt()
        return ((n shl 31).toInt() shr 31) xor (n shr 1).toInt()
    }
    fun readZigZag64(endian: Endian = Endian.Null): Long {
        val n = readVarInt64(endian).toULong()
        return (n shr 1).toLong() xor (-(n and 1uL).toLong())
    }

    // String
    fun readString(count: Int, endian: Endian = Endian.Null): String {
        val e = sEndian(endian)
        val ary = readBytes(count)
        if (e == Endian.Small) ary.reverse()
        return String(ary, charset(encode))
    }
    fun readStringByEmpty(endian: Endian = Endian.Null): String {
        val e = sEndian(endian)
        val bytes = mutableListOf<Byte>()
        while (true) {
            val b = readUInt8()
            if (b == 0u.toUByte()) break
            bytes.add(b.toByte())
        }
        val b = bytes.toByteArray()
        if (e == Endian.Small) b.reverse()
        return String(b, charset(encode))
    }
    fun readStringByUInt8Head(endian: Endian = Endian.Null): String = readString(readUInt8().toInt(), endian)
    fun readStringByUInt16Head(endian: Endian = Endian.Null): String = readString(readUInt16().toInt(), endian)
    fun readStringByInt16Head(endian: Endian = Endian.Null): String = readString(readInt16().toInt(), endian)
    fun readStringByInt32Head(endian: Endian = Endian.Null): String = readString(readInt32(), endian)
    fun readStringByVarInt32Head(endian: Endian = Endian.Null): String = readString(readVarInt32(), endian)

    // ====================== 基础类型写 API (全在基类实现) ======================

    suspend fun write(b: Int) = write(byteArrayOf(b.toByte()))
    suspend fun write(buffer: ByteArray, offset: Int = 0, count: Int = buffer.size) = coreWrite(buffer, offset, count)
    suspend fun write(buffer: ByteBuffer) = coreWrite(buffer)

    suspend fun write(buffer: IntArray, offset: Int = 0, count: Int = buffer.size, endian: Endian = Endian.Null) = coreWrite(buffer, offset, count,endian)
    suspend fun write(buffer: IntBuffer, endian: Endian = Endian.Null) = coreWrite(buffer,endian)

    suspend fun write(input: InputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long = withContext(Dispatchers.IO) {
        var bytesCopied: Long = 0
        val buffer = ByteArray(bufferSize)
        var bytes = input.read(buffer)
        while (bytes >= 0) {
            write(buffer, 0, bytes)
            bytesCopied += bytes
            bytes = input.read(buffer)
        }
        bytesCopied
    }

    // 8-bit
    suspend fun writeSByte(value: Byte) { tempBuffer[0] = value; coreWrite(tempBuffer, 0, 1) }
    suspend fun writeInt8(value: Byte) = writeSByte(value)
    suspend fun writeByte(value: Byte) = writeSByte(value)
    suspend fun writeUInt8(value: UByte) { tempBuffer[0] = value.toByte(); coreWrite(tempBuffer, 0, 1) }
    suspend fun writeBoolean(value: Boolean) { tempBuffer[0] = if (value) 1.toByte() else 0.toByte(); coreWrite(tempBuffer, 0, 1) }
    suspend fun writeBool(value: Boolean) = writeBoolean(value)

    // 16-bit
    suspend fun writeShort(value: Short, endian: Endian = Endian.Null) = writeInt16(value, endian)
    suspend fun writeInt16(value: Short, endian: Endian = Endian.Null) {
        val e = rEndian(endian)
        if (e == Endian.Big) {
            tempBuffer[1] = value.toByte(); tempBuffer[0] = (value.toInt() shr 8).toByte()
        } else {
            tempBuffer[0] = value.toByte(); tempBuffer[1] = (value.toInt() shr 8).toByte()
        }
        coreWrite(tempBuffer, 0, 2)
    }
    suspend fun writeUShort(value: UShort, endian: Endian = Endian.Null) = writeUInt16(value, endian)
    suspend fun writeUInt16(value: UShort, endian: Endian = Endian.Null) {
        val e = rEndian(endian)
        if (e == Endian.Big) {
            tempBuffer[1] = value.toByte(); tempBuffer[0] = (value.toInt() shr 8).toByte()
        } else {
            tempBuffer[0] = value.toByte(); tempBuffer[1] = (value.toInt() shr 8).toByte()
        }
        coreWrite(tempBuffer, 0, 2)
    }
    suspend fun writeChar(value: Char, endian: Endian = Endian.Null) = writeUInt16(value.code.toUShort(), endian)

    // 24-bit
    suspend fun writeThreeByte(value: Int, endian: Endian = Endian.Null) = writeInt24(value, endian)
    suspend fun writeInt24(value: Int, endian: Endian = Endian.Null) {
        val e = rEndian(endian)
        if (e == Endian.Big) {
            tempBuffer[2] = value.toByte(); tempBuffer[1] = (value shr 8).toByte(); tempBuffer[0] = (value shr 16).toByte()
        } else {
            tempBuffer[0] = value.toByte(); tempBuffer[1] = (value shr 8).toByte(); tempBuffer[2] = (value shr 16).toByte()
        }
        coreWrite(tempBuffer, 0, 3)
    }
    suspend fun writeUThreeByte(value: UInt, endian: Endian = Endian.Null) = writeUInt24(value, endian)
    suspend fun writeUInt24(value: UInt, endian: Endian = Endian.Null) {
        val e = rEndian(endian)
        if (e == Endian.Big) {
            tempBuffer[2] = value.toByte(); tempBuffer[1] = (value shr 8).toByte(); tempBuffer[0] = (value shr 16).toByte()
        } else {
            tempBuffer[0] = value.toByte(); tempBuffer[1] = (value shr 8).toByte(); tempBuffer[2] = (value shr 16).toByte()
        }
        coreWrite(tempBuffer, 0, 3)
    }

    // 32-bit
    suspend fun writeInt(value: Int, endian: Endian = Endian.Null) = writeInt32(value, endian)
    suspend fun writeInt32(value: Int, endian: Endian = Endian.Null) {
        val e = rEndian(endian)
        if (e == Endian.Big) {
            tempBuffer[3] = value.toByte(); tempBuffer[2] = (value shr 8).toByte()
            tempBuffer[1] = (value shr 16).toByte(); tempBuffer[0] = (value shr 24).toByte()
        } else {
            tempBuffer[0] = value.toByte(); tempBuffer[1] = (value shr 8).toByte()
            tempBuffer[2] = (value shr 16).toByte(); tempBuffer[3] = (value shr 24).toByte()
        }
        coreWrite(tempBuffer, 0, 4)
    }
    suspend fun writeUInt(value: UInt, endian: Endian = Endian.Null) = writeUInt32(value, endian)
    suspend fun writeUInt32(value: UInt, endian: Endian = Endian.Null) {
        val e = rEndian(endian)
        if (e == Endian.Big) {
            tempBuffer[3] = value.toByte(); tempBuffer[2] = (value shr 8).toByte()
            tempBuffer[1] = (value shr 16).toByte(); tempBuffer[0] = (value shr 24).toByte()
        } else {
            tempBuffer[0] = value.toByte(); tempBuffer[1] = (value shr 8).toByte()
            tempBuffer[2] = (value shr 16).toByte(); tempBuffer[3] = (value shr 24).toByte()
        }
        coreWrite(tempBuffer, 0, 4)
    }
    suspend fun writeSingle(value: Float, endian: Endian = Endian.Null) = writeFloat32(value, endian)
    suspend fun writeFloat(value: Float, endian: Endian = Endian.Null) = writeFloat32(value, endian)
    suspend fun writeFloat32(value: Float, endian: Endian = Endian.Null) {
        val e = rEndian(endian)
        val bytes = ByteBuffer.allocate(4).order(e.byteOrder).putFloat(value)
        writeUInt32(bytes.getInt(0).toUInt(), e)
    }

    // 64-bit
    suspend fun writeLong(value: Long, endian: Endian = Endian.Null) = writeInt64(value, endian)
    suspend fun writeInt64(value: Long, endian: Endian = Endian.Null) {
        val e = rEndian(endian)
        if (e == Endian.Big) {
            tempBuffer[7] = value.toByte(); tempBuffer[6] = (value shr 8).toByte()
            tempBuffer[5] = (value shr 16).toByte(); tempBuffer[4] = (value shr 24).toByte()
            tempBuffer[3] = (value shr 32).toByte(); tempBuffer[2] = (value shr 40).toByte()
            tempBuffer[1] = (value shr 48).toByte(); tempBuffer[0] = (value shr 56).toByte()
        } else {
            tempBuffer[0] = value.toByte(); tempBuffer[1] = (value shr 8).toByte()
            tempBuffer[2] = (value shr 16).toByte(); tempBuffer[3] = (value shr 24).toByte()
            tempBuffer[4] = (value shr 32).toByte(); tempBuffer[5] = (value shr 40).toByte()
            tempBuffer[6] = (value shr 48).toByte(); tempBuffer[7] = (value shr 56).toByte()
        }
        coreWrite(tempBuffer, 0, 8)
    }
    suspend fun writeULong(value: ULong, endian: Endian = Endian.Null) = writeUInt64(value, endian)
    suspend fun writeUInt64(value: ULong, endian: Endian = Endian.Null) {
        val e = rEndian(endian)
        if (e == Endian.Big) {
            tempBuffer[7] = value.toByte(); tempBuffer[6] = (value shr 8).toByte()
            tempBuffer[5] = (value shr 16).toByte(); tempBuffer[4] = (value shr 24).toByte()
            tempBuffer[3] = (value shr 32).toByte(); tempBuffer[2] = (value shr 40).toByte()
            tempBuffer[1] = (value shr 48).toByte(); tempBuffer[0] = (value shr 56).toByte()
        } else {
            tempBuffer[0] = value.toByte(); tempBuffer[1] = (value shr 8).toByte()
            tempBuffer[2] = (value shr 16).toByte(); tempBuffer[3] = (value shr 24).toByte()
            tempBuffer[4] = (value shr 32).toByte(); tempBuffer[5] = (value shr 40).toByte()
            tempBuffer[6] = (value shr 48).toByte(); tempBuffer[7] = (value shr 56).toByte()
        }
        coreWrite(tempBuffer, 0, 8)
    }
    suspend fun writeDouble(value: Double, endian: Endian = Endian.Null) = writeFloat64(value, endian)
    suspend fun writeFloat64(value: Double, endian: Endian = Endian.Null) {
        val e = rEndian(endian)
        val bytes = ByteBuffer.allocate(8).order(e.byteOrder).putDouble(value)
        writeUInt64(bytes.getLong(0).toULong(), e)
    }

    // VarInt / ZigZag
    suspend fun writeVarInt32(value: Int) {
        var num = value.toUInt()
        while (num >= 128u) { writeByte((num or 0x80u).toByte()); num = num shr 7 }
        writeByte(num.toByte())
    }
    suspend fun writeVarInt64(value: Long) {
        var num = value.toULong()
        while (num >= 128uL) { writeByte((num or 0x80uL).toByte()); num = num shr 7 }
        writeByte(num.toByte())
    }
    suspend fun writeUVarInt32(value: UInt) = writeVarInt32(value.toInt())
    suspend fun writeUVarInt64(value: ULong) = writeVarInt64(value.toLong())
    suspend fun writeZigZag32(value: Int) = writeVarInt32((value shl 1) xor (value shr 31))
    suspend fun writeZigZag64(value: Long) = writeVarInt64((value shl 1) xor (value shr 63))

    // Bytes & String
    suspend fun writeBytes(value: ByteArray) = write(value)
    suspend fun writeString(value: String?, endian: Endian = Endian.Null) {
        if (value == null) return
        val e = sEndian(endian)
        var ary = value.toByteArray(charset(encode))
        if (e == Endian.Small) ary.reverse()
        write(ary)
    }
    suspend fun writeString(value: String?, length: Int, endian: Endian = Endian.Null) {
        if (value == null) { write(ByteArray(length)); return }
        val e = sEndian(endian)
        var ary = value.toByteArray(charset(encode))
        if (e == Endian.Small) ary.reverse()
        val t = ByteArray(length)
        ary.copyInto(t, endIndex = minOf(ary.size, length))
        write(t)
    }
    suspend fun writeStringByEmpty(value: String?, endian: Endian = Endian.Null) {
        if (value == null) { writeUInt8(0u); return }
        writeString(value, endian)
        writeUInt8(0u)
    }
    suspend fun writeStringByUInt8Head(value: String?) {
        if (value == null) { writeUInt8(0u); return }
        val bytes = value.toByteArray(charset(encode))
        writeUInt8(bytes.size.toUByte())
        writeBytes(bytes)
    }
    suspend fun writeStringByUInt16Head(value: String?) {
        if (value == null) { writeUInt16(0u); return }
        val bytes = value.toByteArray(charset(encode))
        writeUInt16(bytes.size.toUShort())
        writeBytes(bytes)
    }
    suspend fun writeStringByInt16Head(value: String?) {
        if (value == null) { writeInt16(0); return }
        val bytes = value.toByteArray(charset(encode))
        writeInt16(bytes.size.toShort())
        writeBytes(bytes)
    }
    suspend fun writeStringByInt32Head(value: String?) {
        if (value == null) { writeInt32(0); return }
        val bytes = value.toByteArray(charset(encode))
        writeInt32(bytes.size)
        writeBytes(bytes)
    }
    suspend fun writeStringByVarInt32Head(value: String?) {
        if (value == null) { writeVarInt32(0); return }
        val bytes = value.toByteArray(charset(encode))
        writeVarInt32(bytes.size)
        writeBytes(bytes)
    }

    // ====================== ID 校验 API ======================
    fun idByte(h: Byte) { if (readUInt8() != h.toUByte()) throw IOException("Data mismatch") }
    fun idUInt8(h: Byte, endian: Endian = Endian.Null) { if (readUInt8(endian) != h.toUByte()) throw IOException("Data mismatch") }
    fun idInt16(h: Short, endian: Endian = Endian.Null) { if (readInt16(endian) != h) throw IOException("Data mismatch") }
    fun idUInt16(h: UShort, endian: Endian = Endian.Null) { if (readUInt16(endian) != h) throw IOException("Data mismatch") }
    fun idInt32(h: Int, endian: Endian = Endian.Null) { if (readInt32(endian) != h) throw IOException("Data mismatch") }
    fun idUInt32(h: UInt, endian: Endian = Endian.Null) { if (readUInt32(endian) != h) throw IOException("Data mismatch") }
    fun idBytes(h: ByteArray) { if (!readBytes(h.size).contentEquals(h)) throw IOException("Data mismatch") }
    fun idString(h: String, endian: Endian = Endian.Null) { if (readString(h.length, endian) != h) throw IOException("Data mismatch") }

    // ====================== Peek API ======================
    inline fun <T> peek(block: AbstractCoroutineBinaryStream.() -> T): T {
        val pos = readPosition
        try {
            return block()
        } finally {
            readPosition = pos
        }
    }
    fun peekByte(endian: Endian = Endian.Null): Byte = peek { readByte(endian) }
    fun peekInt16(endian: Endian = Endian.Null): Short = peek { readInt16(endian) }
    fun peekUInt16(endian: Endian = Endian.Null): UShort = peek { readUInt16(endian) }
    fun peekInt32(endian: Endian = Endian.Null): Int = peek { readInt32(endian) }
    fun peekString(count: Int, endian: Endian = Endian.Null): String = peek { readString(count, endian) }
    fun getStringByEmpty(offset: Long, endian: Endian = Endian.Null): String = peek {
        readPosition = offset
        readStringByEmpty(endian)
    }

    // ====================== 转换 API (需要子类实现) ======================
    abstract fun toByteArray(): ByteArray
    abstract fun toBuffer(): ByteBuffer
}