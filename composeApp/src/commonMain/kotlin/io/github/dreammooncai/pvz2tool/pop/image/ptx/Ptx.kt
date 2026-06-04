package io.github.dreammooncai.pvz2tool.pop.image.ptx

import io.github.dreammooncai.pvz2tool.pop.image.Texture
import io.github.dreammooncai.pvz2tool.pop.image.*
import io.github.dreammooncai.pvz2tool.pop.image.YFBitmap
import io.github.dreammooncai.pvz2tool.pop.image.ptx.crypt.ARGB8888_Padding
import io.github.dreammooncai.pvz2tool.pop.image.ptx.crypt.ETC1_RGB_A_Palette
import io.github.dreammooncai.pvz2tool.pop.plugin.io.Endian
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream
import java.io.File


object Ptx {
    var PtxABGR8888Mode = false
    var PtxARGB8888PaddingMode = false
    var RsbPtxABGR8888Mode = true
    var RsbPtxARGB8888PaddingMode = false

    // 原有解码逻辑（精简冗余，保留核心）
    suspend fun decode(ptxPath: String, pngPath: String, fromRsb: Boolean = false) {
        val ptxFile = File(ptxPath)
        if (!ptxFile.exists()) throw IllegalArgumentException("PTX文件不存在: $ptxPath")
        CoroutineBinaryStream.open(ptxPath).use { bs -> decode(bs, pngPath, fromRsb) }
    }

    suspend fun decode(ptxFile: File, pngFile: File, fromRsb: Boolean = false) {
        if (!ptxFile.exists()) throw IllegalArgumentException("PTX文件不存在: ${ptxFile.absolutePath}")
        CoroutineBinaryStream.open(ptxFile.absolutePath).use { bs -> decode(bs, pngFile.absolutePath, fromRsb) }
    }

    suspend fun decode(bs: CoroutineBinaryStream, pngPath: String, fromRsb: Boolean = false) {
        val head = PtxHead().read(bs)
        decode(bs, head, fromRsb).use { bitmap ->
            bitmap.saveToPng(pngPath)
        }
    }

    suspend fun decode(bs: CoroutineBinaryStream, head: PtxHead, fromRsb: Boolean = false): YFBitmap {
        val decoder = Texture.getDecoder(head.format, bs.endian, head, fromRsb)
        if (head.format == PtxFormat.DXT5_RGBA_MortonBlock) {
            bs.endian = Endian.Small
            val bitmap = decoder.decode(bs, head.width, head.height)
            bs.endian = Endian.Big
            return bitmap
        }
        return decoder.decode(bs, head.width, head.height)
    }

    suspend fun encode(
        inFile: File,
        outFile: File,
        format: PtxFormat,
        encodeEndian: Endian = Endian.Small,
        chineseMode: Boolean = false
    ) = encode(inFile.absolutePath,outFile.absolutePath,format,encodeEndian,chineseMode)

    suspend fun encode(
        inFile: String,
        outFile: String,
        format: PtxFormat,
        encodeEndian: Endian = Endian.Small,
        chineseMode: Boolean = false
    ) {
        CoroutineBinaryStream.create(outFile).use { bs ->
            encode(inFile,bs,format,encodeEndian,chineseMode)
            bs.saveFile()
        }
    }

    suspend fun encode(
        inFile: File,
        outFile: CoroutineBinaryStream,
        format: PtxFormat,
        encodeEndian: Endian = Endian.Small,
        chineseMode: Boolean = false
    ) = encode(inFile.absolutePath,outFile,format,encodeEndian,chineseMode)

    suspend fun encode(
        inFile: String,
        outFile: CoroutineBinaryStream,
        format: PtxFormat,
        encodeEndian: Endian = Endian.Small,
        chineseMode: Boolean = false
    ) {
        outFile.let { bs ->
            bs.endian = encodeEndian
            (YFBitmap.fromFile(inFile) ?: error("无法找到目标图片")).use { bitmap ->
                val offset = bs.writePosition
                val head = PtxHead().apply {
                    width = bitmap.width
                    height = bitmap.height
                    this.format = format
                }
                head.write(bs)

                // 核心编码逻辑（精简分支，保留必要处理）
                val encoder = when (format) {
                    PtxFormat.RGBA4444_Block, PtxFormat.RGB565_Block, PtxFormat.RGBA5551_Block -> {
                        head.width = alignTo(head.width, 32)
                        head.height = alignTo(head.height, 32)
                        Texture.getEncoder(format, bs.endian, chineseMode)
                    }
                    else -> Texture.getEncoder(format, bs.endian, chineseMode)
                }

                if (encoder is ARGB8888_Padding) {
                    var w: Int = bitmap.width
                    if ((w % 64) != 0) w = (w / 64) * 64 + 64
                    encoder.blockSize = w shl 2
                }

                // 特殊格式字节序处理
                if (format == PtxFormat.DXT5_RGBA_MortonBlock && bs.endian == Endian.Big) {
                    bs.endian = Endian.Small
                    head.check = encoder.encode(bs, bitmap)
                    bs.endian = Endian.Big
                } else {
                    head.check = encoder.encode(bs, bitmap)
                }

                // ETC1调色板特殊处理
                if (encoder is ETC1_RGB_A_Palette) {
                    head.alphaSize = encoder.alphaSize
                    head.alphaFormat = 0x64
                }

                bs.setWritePosition(offset)
                head.write(bs)
            }
        }
    }

    // 对齐工具（精简）
    private fun alignTo(value: Int, align: Int) = if (value % align == 0) value else (value / align) * align + align
}