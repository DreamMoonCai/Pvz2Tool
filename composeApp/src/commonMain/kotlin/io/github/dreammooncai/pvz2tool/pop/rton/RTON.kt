@file:OptIn(ExperimentalSerializationApi::class)

package io.github.dreammooncai.pvz2tool.pop.rton

import io.github.alexzhirkevich.keight.JSEngine
import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.js.JSFunction
import io.github.alexzhirkevich.keight.js.JsObject
import io.github.alexzhirkevich.keight.js.Object
import io.github.alexzhirkevich.keight.js.js
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.bouncycastle.crypto.engines.RijndaelEngine
import org.bouncycastle.crypto.modes.CBCBlockCipher
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher
import org.bouncycastle.crypto.paddings.ZeroBytePadding
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import java.io.File
import java.security.MessageDigest

// 字符串池数据类
private data class PoolInfo(
    val offset: Long,
    val index: Int,
    val value: String
)

// 简化的字符串池实现
private class StringPool(private val autoPool: Boolean = false) {
    private val pool = mutableMapOf<String, PoolInfo>()
    private var position = 0L
    private var index = 0

    val size: Int get() = index

    operator fun get(index: Int): PoolInfo? = pool.values.elementAtOrNull(index)
    operator fun get(id: String): PoolInfo? {
        if (id !in pool && autoPool) throwInPool(id)
        return pool[id]
    }

    fun exists(id: String): Boolean = id in pool

    fun clear() {
        pool.clear()
        position = 0L
        index = 0
    }

    fun throwInPool(poolKey: String): PoolInfo {
        if (poolKey !in pool) {
            pool[poolKey] = PoolInfo(position, index++, poolKey)
            position += poolKey.length + 1
        }
        return pool.getValue(poolKey)
    }
}

// MD5 扩展
private fun String.md5(): String {
    val digest = MessageDigest.getInstance("MD5").digest(toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}

private object Rijndael {
    fun encrypt(plain: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        return process(true, plain, key, iv)
    }

    fun decrypt(cipherText: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        return process(false, cipherText, key, iv)
    }

    private fun process(encrypt: Boolean, data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val engine = RijndaelEngine(iv.size * 8)
        val blockCipher = CBCBlockCipher.newInstance(engine)
        val cipher = PaddedBufferedBlockCipher(blockCipher, ZeroBytePadding())

        val keyParam = KeyParameter(key.copyOf(32))
        val params = ParametersWithIV(keyParam, iv)

        cipher.init(encrypt, params)

        val output = ByteArray(cipher.getOutputSize(data.size))
        val len = cipher.processBytes(data, 0, data.size, output, 0)
        cipher.doFinal(output, len)

        return output
    }
}

/**
 * RTON 格式处理类。
 * 由于内部包含可变状态（字符串池），此类设计为【非线程安全】。
 *
 * 多线程环境下的正确用法：
 * 1. 每次使用创建新实例 (推荐)。
 * 2. 确保一个实例只被一个线程使用。
 *
 * 若需快速调用，请使用伴生对象中的静态方法 [RTON.decodeAuto], [RTON.encodeAuto] 等。
 */
class RTON {
    private val pool90 = StringPool()
    private val pool92 = StringPool()
    private val list90 = mutableListOf<ByteArray>()
    private val list92 = mutableListOf<ByteArray>()

    // JSON 配置
    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }

    // 伴生对象：存放常量与提供快速静态方法
    companion object {
        var encryptionKey: String = "com_popcap_pvz2_magento_product_2013_05_05"

        // 常量
        private const val MAGIC = "RTON"
        private const val VERSION = 0x1
        private const val EOF = "DONE"
        private const val ENCRYPTED_MARKER = "@rton_encrypted"

        // --- 快速静态方法 (线程安全：每次调用创建新实例) ---

        // 文件版
        suspend fun decodeAuto(input: String, output: String) = RTON().decodeAuto(File(input), File(output))
        suspend fun encodeAuto(input: String, output: String) = RTON().encodeAuto(File(input), File(output))
        suspend fun decode(input: String, output: String) = RTON().decode(File(input), File(output))
        suspend fun decodeAndDecrypt(input: String, output: String) = RTON().decodeAndDecrypt(File(input), File(output))
        suspend fun encode(input: String, output: String) = RTON().encode(File(input), File(output))
        suspend fun encodeAndEncrypt(input: String, output: String) = RTON().encodeAndEncrypt(File(input), File(output))

        // 流版本（你要的核心功能）
        suspend fun decodeAuto(inputBs: CoroutineBinaryStream, output: File) = RTON().decodeAuto(inputBs, output)
        suspend fun encodeAuto(input: File, outputBs: CoroutineBinaryStream) = RTON().encodeAuto(input, outputBs)
        suspend fun decode(inputBs: CoroutineBinaryStream, output: File) = RTON().decode(inputBs, output)
        suspend fun decodeAndDecrypt(inputBs: CoroutineBinaryStream, output: File) = RTON().decodeAndDecrypt(inputBs, output)
        suspend fun encode(input: File, outputBs: CoroutineBinaryStream) = RTON().encode(input, outputBs)
        suspend fun encodeAndEncrypt(input: File, outputBs: CoroutineBinaryStream) = RTON().encodeAndEncrypt(input, outputBs)
    }

    // 类型标记
    private object Type {
        const val FALSE = 0x00
        const val TRUE = 0x01
        const val NULL = 0x02
        const val SBYTE = 0x08
        const val SBYTE_ZERO = 0x09
        const val BYTE = 0x0A
        const val BYTE_ZERO = 0x0B
        const val INT16 = 0x10
        const val INT16_ZERO = 0x11
        const val UINT16 = 0x12
        const val UINT16_ZERO = 0x13
        const val INT32 = 0x20
        const val INT32_ZERO = 0x21
        const val FLOAT32 = 0x22
        const val FLOAT32_ZERO = 0x23
        const val VARINT32 = 0x24
        const val ZIGZAG32 = 0x25
        const val UINT32 = 0x26
        const val UINT32_ZERO = 0x27
        const val UVARINT32 = 0x28
        const val INT64 = 0x40
        const val INT64_ZERO = 0x41
        const val FLOAT64 = 0x42
        const val FLOAT64_ZERO = 0x43
        const val VARINT64 = 0x44
        const val ZIGZAG64 = 0x45
        const val UINT64 = 0x46
        const val UINT64_ZERO = 0x47
        const val UVARINT64 = 0x48
        const val STR_81 = 0x81
        const val STR_82 = 0x82
        const val RTID = 0x83
        const val RTID_0 = 0x84
        const val OBJECT = 0x85
        const val ARRAY = 0x86
        const val BINARY = 0x87
        const val POOL_90 = 0x90
        const val POOL_91 = 0x91
        const val POOL_92 = 0x92
        const val POOL_93 = 0x93
        const val BOOL_BC = 0xBC
        const val ARRAY_START = 0xFD
        const val ARRAY_END = 0xFE
        const val OBJECT_END = 0xFF
    }

    // 字符串模板
    private object Str {
        const val NULL = "*"
        const val RTID_0 = "RTID(0)"
        const val RTID_2 = "RTID(%d.%d.%08x@%s)"
        const val RTID_3 = "RTID(%s@%s)"
        const val BINARY = "\$BINARY(\"%s\", %d)"
        const val BINARY_BEGIN = "\$BINARY(\""
        const val BINARY_END = ")"
        const val BINARY_MID = "\", "
        const val RTID_BEGIN = "RTID("
        const val RTID_END = ")"
    }

    // ========================== 对外接口：流 + 文件双支持 ==========================

    /** 自动识别加密的解码（输入：流） */
    suspend fun decodeAuto(inputBs: CoroutineBinaryStream, output: File) = usePools {
        inputBs.readPosition = 0
        var isEncrypted = false
        val root = if (inputBs.length >= 2 && inputBs.peekInt16() == 0x10.toShort()) {
            isEncrypted = true
            inputBs.readPosition = 2
            val key = encryptionKey.md5().toByteArray(Charsets.UTF_8)
            val iv = ByteArray(24).also { System.arraycopy(key, 4, it, 0, 24) }
            val encrypted = inputBs.readBytes((inputBs.length - 2).toInt())
            val decrypted = Rijndael.decrypt(encrypted, key, iv)
            CoroutineBinaryStream.wrap(decrypted).use { decryptedBs ->
                decryptedBs.readPosition = 0
                decryptedBs.idString(MAGIC)
                decryptedBs.idInt32(VERSION)
                readObject(decryptedBs).also {
                    decryptedBs.idString(EOF)
                }
            }
        } else {
            inputBs.idString(MAGIC)
            inputBs.idInt32(VERSION)
            readObject(inputBs).also {
                inputBs.idString(EOF)
            }
        }

        val finalRoot = if (isEncrypted) {
            JsonObject((root + (ENCRYPTED_MARKER to JsonPrimitive(true))))
        } else {
            root
        }
        withContext(Dispatchers.IO) {
            output.writeText(json.encodeToString(finalRoot))
        }
    }

    /** 自动识别加密的编码（输出：流） */
    suspend fun encodeAuto(input: File, outputBs: CoroutineBinaryStream) = usePools {
        val originalRoot = json.parseToJsonElement(input.readText()).jsonObject
        val isEncrypted = originalRoot[ENCRYPTED_MARKER]?.jsonPrimitive?.booleanOrNull == true
        val rootToEncode = JsonObject(originalRoot - ENCRYPTED_MARKER)

        if (isEncrypted) {
            CoroutineBinaryStream.allocateBytes().use { source ->
                source.writeString(MAGIC)
                source.writeInt32(VERSION)
                writeObject(source, rootToEncode)
                source.writeString(EOF)
                source.readPosition = 0
                val key = encryptionKey.md5().toByteArray(Charsets.UTF_8)
                val iv = ByteArray(24).also { System.arraycopy(key, 4, it, 0, 24) }
                val encrypted = Rijndael.encrypt(source.readBytes(source.length.toInt()), key, iv)
                outputBs.writeInt16(0x10)
                outputBs.writeBytes(encrypted)
            }
        } else {
            outputBs.writeString(MAGIC)
            outputBs.writeInt32(VERSION)
            writeObject(outputBs, rootToEncode)
            outputBs.writeString(EOF)
        }
    }

    /** 普通解码（输入：流） */
    suspend fun decode(inputBs: CoroutineBinaryStream, output: File) = usePools {
        inputBs.readPosition = 0
        inputBs.idString(MAGIC)
        inputBs.idInt32(VERSION)
        val root = readObject(inputBs)
        inputBs.idString(EOF)
        withContext(Dispatchers.IO) {
            output.writeText(json.encodeToString(root))
        }
    }

    /** 解密 + 解码（输入：流） */
    suspend fun decodeAndDecrypt(inputBs: CoroutineBinaryStream, output: File) = usePools {
        inputBs.readPosition = 0
        val key = encryptionKey.md5().toByteArray(Charsets.UTF_8)
        val iv = ByteArray(24).also { System.arraycopy(key, 4, it, 0, 24) }
        inputBs.idInt16(0x10)
        val encrypted = inputBs.readBytes((inputBs.length - 2).toInt())
        val decrypted = Rijndael.decrypt(encrypted, key, iv)
        CoroutineBinaryStream.wrap(decrypted).use { bs ->
            bs.readPosition = 0
            bs.idString(MAGIC)
            bs.idInt32(VERSION)
            val root = readObject(bs)
            bs.idString(EOF)
            withContext(Dispatchers.IO) {
                output.writeText(json.encodeToString(root))
            }
        }
    }

    /** 普通编码（输出：流） */
    suspend fun encode(input: File, outputBs: CoroutineBinaryStream) = usePools {
        val root = json.parseToJsonElement(input.readText()).jsonObject
        outputBs.writeString(MAGIC)
        outputBs.writeInt32(VERSION)
        writeObject(outputBs, root)
        outputBs.writeString(EOF)
    }

    /** 编码 + 加密（输出：流） */
    suspend fun encodeAndEncrypt(input: File, outputBs: CoroutineBinaryStream) = usePools {
        val root = json.parseToJsonElement(input.readText()).jsonObject
        CoroutineBinaryStream.allocateBytes().use { source ->
            source.writeString(MAGIC)
            source.writeInt32(VERSION)
            writeObject(source, root)
            source.writeString(EOF)
            source.readPosition = 0
            val key = encryptionKey.md5().toByteArray(Charsets.UTF_8)
            val iv = ByteArray(24).also { System.arraycopy(key, 4, it, 0, 24) }
            val encrypted = Rijndael.encrypt(source.readBytes(source.length.toInt()), key, iv)
            outputBs.writeInt16(0x10)
            outputBs.writeBytes(encrypted)
        }
    }

    // ---------------- 兼容原有文件接口 ----------------
    suspend fun decodeAuto(input: File, output: File) = CoroutineBinaryStream.open(input).use { bs -> decodeAuto(bs, output) }
    suspend fun encodeAuto(input: File, output: File) {
        CoroutineBinaryStream.create(output).use { bs ->
            encodeAuto(input, bs)
            bs.saveFile()
        }
    }
    suspend fun decode(input: File, output: File) = CoroutineBinaryStream.open(input).use { bs -> decode(bs, output) }
    suspend fun decodeAndDecrypt(input: File, output: File) = CoroutineBinaryStream.open(input).use { bs -> decodeAndDecrypt(bs, output) }
    suspend fun encode(input: File, output: File) {
        CoroutineBinaryStream.create(output).use { bs ->
            encode(input, bs)
            bs.saveFile()
        }
    }
    suspend fun encodeAndEncrypt(input: File, output: File) {
        CoroutineBinaryStream.create(output).use { bs ->
            encodeAndEncrypt(input, bs)
            bs.saveFile()
        }
    }

    // --- 内部实现 ---
    private inline fun <T> usePools(block: () -> T): T {
        pool90.clear()
        pool92.clear()
        list90.clear()
        list92.clear()
        return try {
            block()
        } finally {
            pool90.clear()
            pool92.clear()
            list90.clear()
            list92.clear()
        }
    }

    private fun readBinary(bs: CoroutineBinaryStream): String {
        bs.readByte()
        val s = bs.readStringByVarInt32Head()
        val i = bs.readVarInt32()
        return Str.BINARY.format(s, i)
    }

    private fun readRTID(bs: CoroutineBinaryStream): String = when (val t = bs.readByte().toInt() and 0xFF) {
        0x00 -> Str.RTID_0
        0x01 -> {
            val v2 = bs.readVarInt32()
            val v1 = bs.readVarInt32()
            val x = bs.readUInt32()
            Str.RTID_2.format(v1, v2, x.toInt(), "")
        }
        0x02 -> {
            bs.readVarInt32()
            val s = bs.readStringByVarInt32Head()
            val v2 = bs.readVarInt32()
            val v1 = bs.readVarInt32()
            val x = bs.readUInt32()
            Str.RTID_2.format(v1, v2, x.toInt(), s)
        }
        0x03 -> {
            bs.readVarInt32()
            val s2 = bs.readStringByVarInt32Head()
            bs.readVarInt32()
            val s1 = bs.readStringByVarInt32Head()
            Str.RTID_3.format(s1, s2)
        }
        else -> error("Unknown RTID type: $t")
    }

    private fun readKey(bs: CoroutineBinaryStream, type: Int): String = when (type) {
        Type.NULL -> Str.NULL
        Type.STR_81 -> bs.readBytes(bs.readVarInt32()).toString(Charsets.UTF_8)
        Type.STR_82 -> {
            bs.readVarInt32()
            bs.readBytes(bs.readVarInt32()).toString(Charsets.UTF_8)
        }
        Type.RTID -> readRTID(bs)
        Type.RTID_0 -> Str.RTID_0
        Type.BINARY -> readBinary(bs)
        Type.POOL_90 -> {
            val b = bs.readBytes(bs.readVarInt32())
            list90 += b
            b.toString(Charsets.UTF_8)
        }
        Type.POOL_91 -> list90[bs.readVarInt32()].toString(Charsets.UTF_8)
        Type.POOL_92 -> {
            bs.readVarInt32()
            val b = bs.readBytes(bs.readVarInt32())
            list92 += b
            b.toString(Charsets.UTF_8)
        }
        Type.POOL_93 -> list92[bs.readVarInt32()].toString(Charsets.UTF_8)
        else -> error("Unknown key type: $type")
    }

    private suspend fun readValue(bs: CoroutineBinaryStream, type: Int): JsonElement = when (type) {
        Type.FALSE -> JsonPrimitive(false)
        Type.TRUE -> JsonPrimitive(true)
        Type.NULL -> JsonPrimitive(Str.NULL)
        Type.SBYTE -> JsonPrimitive(bs.readSByte())
        Type.SBYTE_ZERO -> JsonPrimitive(0)
        Type.BYTE -> JsonPrimitive(bs.readByte())
        Type.BYTE_ZERO -> JsonPrimitive(0)
        Type.INT16 -> JsonPrimitive(bs.readInt16())
        Type.INT16_ZERO -> JsonPrimitive(0)
        Type.UINT16 -> JsonPrimitive(bs.readUInt16())
        Type.UINT16_ZERO -> JsonPrimitive(0)
        Type.INT32 -> JsonPrimitive(bs.readInt32())
        Type.INT32_ZERO -> JsonPrimitive(0)
        Type.FLOAT32 -> JsonPrimitive(bs.readFloat32())
        Type.FLOAT32_ZERO -> JsonPrimitive(0f)
        Type.VARINT32 -> JsonPrimitive(bs.readVarInt32())
        Type.ZIGZAG32 -> JsonPrimitive(bs.readZigZag32())
        Type.UINT32 -> JsonPrimitive(bs.readUInt32())
        Type.UINT32_ZERO -> JsonPrimitive(0u)
        Type.UVARINT32 -> JsonPrimitive(bs.readUVarInt32())
        Type.INT64 -> JsonPrimitive(bs.readInt64())
        Type.INT64_ZERO -> JsonPrimitive(0L)
        Type.FLOAT64 -> JsonPrimitive(bs.readFloat64())
        Type.FLOAT64_ZERO -> JsonPrimitive(0.0)
        Type.VARINT64 -> JsonPrimitive(bs.readVarInt64())
        Type.ZIGZAG64 -> JsonPrimitive(bs.readZigZag64())
        Type.UINT64 -> JsonPrimitive(bs.readUInt64())
        Type.UINT64_ZERO -> JsonPrimitive(0uL)
        Type.UVARINT64 -> JsonPrimitive(bs.readUVarInt64())
        Type.STR_81 -> JsonPrimitive(bs.readBytes(bs.readVarInt32()).toString(Charsets.UTF_8))
        Type.STR_82 -> {
            bs.readVarInt32()
            JsonPrimitive(bs.readBytes(bs.readVarInt32()).toString(Charsets.UTF_8))
        }
        Type.RTID -> JsonPrimitive(readRTID(bs))
        Type.RTID_0 -> JsonPrimitive(Str.RTID_0)
        Type.OBJECT -> readObject(bs)
        Type.ARRAY -> readArray(bs)
        Type.BINARY -> JsonPrimitive(readBinary(bs))
        Type.POOL_90 -> {
            val b = bs.readBytes(bs.readVarInt32())
            list90 += b
            JsonPrimitive(b.toString(Charsets.UTF_8))
        }
        Type.POOL_91 -> JsonPrimitive(list90[bs.readVarInt32()].toString(Charsets.UTF_8))
        Type.POOL_92 -> {
            bs.readVarInt32()
            val b = bs.readBytes(bs.readVarInt32())
            list92 += b
            JsonPrimitive(b.toString(Charsets.UTF_8))
        }
        Type.POOL_93 -> JsonPrimitive(list92[bs.readVarInt32()].toString(Charsets.UTF_8))
        Type.BOOL_BC -> JsonPrimitive(bs.readByte() != 0.toByte())
        in 0xB0..0xBB -> error("0xb0-0xbb not supported")
        else -> error("Unknown value type: $type")
    }

    private suspend fun readArray(bs: CoroutineBinaryStream): JsonArray = buildJsonArray {
        bs.idByte(Type.ARRAY_START.toByte())
        repeat(bs.readVarInt32()) {
            add(readValue(bs, bs.readByte().toInt() and 0xFF))
        }
        bs.idByte(Type.ARRAY_END.toByte())
    }

    private suspend fun readObject(bs: CoroutineBinaryStream): JsonObject = buildJsonObject {
        while (true) {
            val t = bs.readByte().toInt() and 0xFF
            if (t == Type.OBJECT_END) break
            val key = readKey(bs, t)
            val vType = bs.readByte().toInt() and 0xFF
            put(key, readValue(bs, vType))
        }
    }

    private fun isAscii(s: String) = s.all { it.code <= 127 }

    private suspend fun writeBinary(bs: CoroutineBinaryStream, s: String): Boolean {
        if (!s.startsWith(Str.BINARY_BEGIN) || !s.endsWith(Str.BINARY_END)) return false
        val mid = s.lastIndexOf(Str.BINARY_MID)
        if (mid == -1) return false
        val v = try {
            s.substring(mid + 3, s.length - 1).toInt()
        } catch (_: Exception) {
            return false
        }
        val content = s.substring(9, mid)
        bs.writeByte(Type.BINARY.toByte())
        bs.writeByte(0)
        bs.writeStringByVarInt32Head(content)
        bs.writeVarInt32(v)
        return true
    }

    private suspend fun writeRTID(bs: CoroutineBinaryStream, s: String): Boolean {
        if (!s.startsWith(Str.RTID_BEGIN) || !s.endsWith(Str.RTID_END)) return false
        if (s == Str.RTID_0) {
            bs.writeByte(Type.RTID_0.toByte())
            return true
        }
        val inner = s.substring(5, s.length - 1)
        val at = inner.indexOf('@')
        if (at == -1) return false
        bs.writeByte(Type.RTID.toByte())
        val left = inner.substring(0, at)
        val right = inner.substring(at + 1)

        var isType2 = true
        var dot1 = 0
        var dot2 = 0
        var d = 0
        for (i in left.indices) {
            when {
                left[i] == '.' -> {
                    when (d) {
                        0 -> dot1 = i
                        1 -> dot2 = i
                        else -> isType2 = false
                    }
                    d++
                }
                left[i] !in '0'..'9' && !(d == 2 && left[i].lowercaseChar() in 'a'..'f') -> {
                    isType2 = false
                }
            }
            if (!isType2) break
        }
        if (d != 2) isType2 = false

        if (isType2) {
            bs.writeByte(0x02)
            bs.writeVarInt32(right.length)
            bs.writeStringByVarInt32Head(right)
            bs.writeVarInt32(left.substring(dot1 + 1, dot2).toInt())
            bs.writeVarInt32(left.substring(0, dot1).toInt())
            bs.writeUInt32(left.substring(dot2 + 1).toUInt(16))
        } else {
            bs.writeByte(0x03)
            bs.writeVarInt32(right.length)
            bs.writeStringByVarInt32Head(right)
            bs.writeVarInt32(left.length)
            bs.writeStringByVarInt32Head(left)
        }
        return true
    }

    private suspend fun writeStringInternal(bs: CoroutineBinaryStream, s: String) {
        when {
            s == Str.NULL -> bs.writeByte(Type.NULL.toByte())
            writeRTID(bs, s) -> {}
            writeBinary(bs, s) -> {}
            isAscii(s) -> {
                if (pool90.exists(s)) {
                    bs.writeByte(Type.POOL_91.toByte())
                    bs.writeVarInt32(pool90[s]!!.index)
                } else {
                    bs.writeByte(Type.POOL_90.toByte())
                    bs.writeStringByVarInt32Head(s)
                    pool90.throwInPool(s)
                }
            }
            else -> {
                if (pool92.exists(s)) {
                    bs.writeByte(Type.POOL_93.toByte())
                    bs.writeVarInt32(pool92[s]!!.index)
                } else {
                    bs.writeByte(Type.POOL_92.toByte())
                    bs.writeVarInt32(s.length)
                    bs.writeStringByVarInt32Head(s)
                    pool92.throwInPool(s)
                }
            }
        }
    }

    private suspend fun writeStringKey(bs: CoroutineBinaryStream, s: String) = writeStringInternal(bs, s)
    private suspend fun writeStringValue(bs: CoroutineBinaryStream, s: String) = writeStringInternal(bs, s)

    private suspend fun writeValue(bs: CoroutineBinaryStream, e: JsonElement) {
        when {
            e is JsonObject -> {
                bs.writeByte(Type.OBJECT.toByte())
                writeObject(bs, e)
            }
            e is JsonArray -> {
                bs.writeByte(Type.ARRAY.toByte())
                writeArray(bs, e)
            }
            e is JsonNull || (e is JsonPrimitive && e.content == Str.NULL) -> {
                bs.writeByte(Type.RTID_0.toByte())
            }
            // 字符串必须在 boolean 之前判断，避免 "true"/"false" 字符串被误判为 boolean
            e is JsonPrimitive && e.isString -> writeStringValue(bs, e.content)
            e is JsonPrimitive && e.booleanOrNull != null -> {
                bs.writeByte(if (e.boolean) Type.TRUE.toByte() else Type.FALSE.toByte())
            }
            e is JsonPrimitive && !e.isString -> {
                val raw = e.content
                if ('.' in raw || 'e' in raw.lowercase()) {
                    val d = e.double
                    if (d == 0.0) {
                        bs.writeByte(Type.FLOAT32_ZERO.toByte())
                    } else {
                        val f = e.float
                        if (f.toDouble() == d) {
                            bs.writeByte(Type.FLOAT32.toByte())
                            bs.writeFloat32(f)
                        } else {
                            bs.writeByte(Type.FLOAT64.toByte())
                            bs.writeFloat64(d)
                        }
                    }
                } else {
                    val l = e.long
                    if (l == 0L) {
                        bs.writeByte(Type.INT32_ZERO.toByte())
                    } else if (l > 0) {
                        if (l <= Int.MAX_VALUE) {
                            bs.writeByte(Type.VARINT32.toByte())
                            bs.writeVarInt32(l.toInt())
                        } else {
                            bs.writeByte(Type.VARINT64.toByte())
                            bs.writeVarInt64(l)
                        }
                    } else {
                        if (l + 0x40000000 >= 0) {
                            bs.writeByte(Type.ZIGZAG32.toByte())
                            bs.writeZigZag32(l.toInt())
                        } else {
                            bs.writeByte(Type.ZIGZAG64.toByte())
                            bs.writeZigZag64(l)
                        }
                    }
                }
            }
            else -> error("Unknown element: $e")
        }
    }

    private suspend fun writeArray(bs: CoroutineBinaryStream, a: JsonArray) {
        bs.writeByte(Type.ARRAY_START.toByte())
        bs.writeVarInt32(a.size)
        a.forEach { writeValue(bs, it) }
        bs.writeByte(Type.ARRAY_END.toByte())
    }

    private suspend fun writeObject(bs: CoroutineBinaryStream, o: JsonObject) {
        o.forEach { (k, v) ->
            writeStringKey(bs, k)
            writeValue(bs, v)
        }
        bs.writeByte(Type.OBJECT_END.toByte())
    }
}