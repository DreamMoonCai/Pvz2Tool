package io.github.dreammooncai.pvz2tool.pop.image.ptx.crypt

import io.github.dreammooncai.pvz2tool.pop.image.Texture
import io.github.dreammooncai.pvz2tool.pop.image.YFBitmap
import io.github.dreammooncai.pvz2tool.pop.image.setPixelArgb
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream

/**
 * RGBA4444_Block 32x32块解码器（严格复刻 C# PopStudio 原版）
 * 格式：R4 G4 B4 A4 / 16位像素
 * 分块规则：32x32 按块读取，超出宽高部分不绘制
 */
class RGBA4444_Block : Texture() {

    override suspend fun decode(bs: CoroutineBinaryStream, width: Int, height: Int): YFBitmap {
        val bitmap = YFBitmap(width, height)

        // C# 原版：32x32 分块遍历
        var i = 0
        while (i < height) {
            var w = 0
            while (w < width) {
                // 每个块 32x32
                for (j in 0 until 32) {
                    for (k in 0 until 32) {
                        val temp = bs.readUInt16().toUInt()

                        // 边界判断（和 C# 完全一致）
                        if (i + j < height && w + k < width) {
                            // C# 原版位运算
                            val r = (temp shr 12) and 0x0Fu
                            val g = (temp shr 8) and 0x0Fu
                            val b = (temp shr 4) and 0x0Fu
                            val a = temp and 0x0Fu

                            // 4bit → 8bit 扩位：(x << 4) | x
                            val r8 = ((r shl 4) or r).toInt()
                            val g8 = ((g shl 4) or g).toInt()
                            val b8 = ((b shl 4) or b).toInt()
                            val a8 = ((a shl 4) or a).toInt()

                            // 写入 ARGB
                            val argb = (a8 shl 24) or (r8 shl 16) or (g8 shl 8) or b8
                            bitmap.setPixelArgb(w + k, i + j, argb)
                        }
                    }
                }
                w += 32
            }
            i += 32
        }

        return bitmap
    }
}