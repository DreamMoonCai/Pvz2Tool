package io.github.dreammooncai.pvz2tool.pop.core.rsb.util

import io.github.dreammooncai.pvz2tool.pop.core.rsb.Rsb
import io.github.dreammooncai.pvz2tool.pop.core.rsb.model.compress.CompressionLevel
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.zip.Deflater
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream
import kotlin.use

object Zlib {
    suspend fun pack(
        inFile: File,
        outFile: File,
        level: CompressionLevel,
        isChineseMode: Boolean = false
    ) {
        CoroutineBinaryStream.open(inFile).use { inFile ->
            CoroutineBinaryStream.create(outFile).use { outFile ->
                if (isChineseMode) {
                    outFile.writeInt32(Rsb.SMF_MAGIC)
                    outFile.writeInt32(inFile.length.toInt())
                }
                compressZLib(inFile,outFile,level)
                outFile.saveFile()
            }
        }
    }

    suspend fun pack(
        inFile: CoroutineBinaryStream,
        outFile: CoroutineBinaryStream,
        level: CompressionLevel,
        isChineseMode: Boolean = false
    ) {
        if (isChineseMode) {
            outFile.writeInt32(Rsb.SMF_MAGIC)
            outFile.writeInt32(inFile.length.toInt())
        }
        compressZLib(inFile,outFile,level)
        outFile.saveFile()
    }

    fun unpack(inFile: File, outFile: File) {
        CoroutineBinaryStream.open(inFile).toZlibInputStreamAndClose().use { input ->
            outFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    suspend fun unpack(inFile: CoroutineBinaryStream, outFile: CoroutineBinaryStream) {
        inFile.toZlibInputStream().use { input ->
            outFile.write(input)
            outFile.saveFile()
        }
    }
}

/**
 * 纯内存 ZLib 压缩（仅使用 CoroutineBinaryStream）
 * 无 InputStream / OutputStream，零拷贝，高性能
 */
@Suppress("NewApi")
suspend fun compressZLib(
    input: CoroutineBinaryStream,
    output: CoroutineBinaryStream,
    level: Int = 6
) {
    if (input.length == 0L) {
        return
    }
    // 读取全部输入数据
    val deflater = Deflater(level)

    try {
        if (isSupportZlibBuffer()) {
            deflater.setInput(input.toBuffer())
        } else {
            deflater.setInput(input.toByteArray())
        }
        deflater.finish()

        val buffer = ByteArray(input.length.toInt().coerceIn(8192, 30 * 1024 * 1024))
        while (!deflater.finished()) {
            val size = deflater.deflate(buffer)
            if (size > 0) {
                output.write(buffer, count = size)
            }
        }
    } finally {
        deflater.end()
    }
}

suspend fun compressZLib(
    input: CoroutineBinaryStream,
    output: CoroutineBinaryStream,
    level: CompressionLevel
) {
    val deflateLevel = when (level) {
        CompressionLevel.Fastest -> 0
        CompressionLevel.Optimal -> 6
        CompressionLevel.Smallest -> 9
    }
    compressZLib(input, output, deflateLevel)
}

/**
 * 纯内存 ZLib 解压缩（仅使用 CoroutineBinaryStream）
 * 无 InputStream / OutputStream，完全协程友好
 */
@Suppress("NewApi")
suspend fun decompressZLib(
    input: CoroutineBinaryStream,
    output: CoroutineBinaryStream
) {
    if (input.length == 0L) {
        return
    }

    val inflater = Inflater()

    try {
        if (isSupportZlibBuffer()) {
            inflater.setInput(input.toBuffer())
        } else {
            inflater.setInput(input.toByteArray())
        }
        val buffer = ByteArray(output.length.toInt().coerceIn(8192, 30 * 1024 * 1024))

        while (!inflater.finished()) {
            val size = inflater.inflate(buffer)
            if (size <= 0) break
            output.write(buffer, count = size)
        }
    } finally {
        inflater.end()
    }
}

fun decompressZLib(
    input: CoroutineBinaryStream,
    bufferSize: Int = input.length.toInt()
): InputStream {
    if (input.length == 0L || bufferSize == 0) {
        return ByteArrayInputStream(ByteArray(0))
    }
    return InflaterInputStream(input.inputStream(),Inflater(), bufferSize.coerceIn(8192, 30 * 1024 * 1024))
}

/**
 * 标准判断：是否为合法 ZLib 流
 * 不移动指针，无副作用
 */
fun CoroutineBinaryStream.isZlib(): Boolean {
    if (length < 2) return false
    return peek {
        val cmf = readUInt8()
        val flg = readUInt8()
        val cm = cmf and 0x0Fu

        // 标准 ZLIB 3.0 + DEFLATE 算法
        if (cmf != 0x78u.toUByte() || cm != 0x08u.toUByte()) return false

        // RFC1950 强制校验
        (cmf.toInt() * 256 + flg.toInt()) % 31 == 0
    }
}

fun ByteArray.isZlib(): Boolean {
    if (size < 2) return false
    val cmf = this[0].toInt() and 0xFF
    val flg = this[1].toInt() and 0xFF

    if (cmf != 0x78) return false

    return (cmf * 256 + flg) % 31 == 0
}

fun CoroutineBinaryStream.toZlibInputStream(): InputStream = let { bsOrigin ->
    runCatching {
        if (bsOrigin.readInt32() == Rsb.SMF_MAGIC) {
            val length = bsOrigin.readInt32()
            return@runCatching decompressZLib(bsOrigin,length)
        }
        bsOrigin.readPosition = 0
        if (bsOrigin.isZlib()) decompressZLib(bsOrigin) else bsOrigin.inputStream()
    }.getOrElse {
        bsOrigin.readPosition = 0
        bsOrigin.inputStream()
    }
}

fun CoroutineBinaryStream.toZlibInputStreamAndClose(): InputStream = use { it.toZlibInputStream() }

internal expect fun isSupportZlibBuffer(): Boolean