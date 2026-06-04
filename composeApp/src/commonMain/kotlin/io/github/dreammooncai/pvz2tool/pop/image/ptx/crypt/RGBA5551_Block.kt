package io.github.dreammooncai.pvz2tool.pop.image.ptx.crypt

import io.github.dreammooncai.pvz2tool.pop.image.Texture
import io.github.dreammooncai.pvz2tool.pop.image.YFBitmap
import io.github.dreammooncai.pvz2tool.pop.image.setPixelArgb
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream

/**
 * RGBA5551_Block 32x32块解码器（严格复刻 C# PopStudio 原版）
 * 格式：R5 G5 B5 A1 / 16位像素
 * 注意：Alpha = !(temp & 1) 【C# 原版 -(temp & 0x1) 等价逻辑】
 */
class RGBA5551_Block : Texture() {

    override suspend fun decode(bs: CoroutineBinaryStream, width: Int, height: Int): YFBitmap {
        val bitmap = YFBitmap(width, height)

        // C# 原版 32x32 分块遍历
        var i = 0
        while (i < height) {
            var w = 0
            while (w < width) {
                for (j in 0 until 32) {
                    for (k in 0 until 32) {
                        val temp = bs.readUInt16().toUInt()

                        // 边界判断
                        if (i + j < height && w + k < width) {
                            // C# 原版位运算
                            val r = (temp shr 11) and 0x1Fu
                            val g = (temp shr 6) and 0x1Fu
                            val b = (temp shr 1) and 0x1Fu
                            val aBit = (temp and 0x1u).toInt()

                            // 5bit → 8bit 扩位（和C#一致）
                            val r8 = ((r shl 3) or (r shr 2)).toInt()
                            val g8 = ((g shl 3) or (g shr 2)).toInt()
                            val b8 = ((b shl 3) or (b shr 2)).toInt()
                            
                            // 关键：C# -(temp & 1) → 0→255，1→0
                            val a8 = if (aBit == 0) 0xFF else 0x00

                            // 合成 ARGB8888
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