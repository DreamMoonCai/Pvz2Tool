package io.github.dreammooncai.pvz2tool.pop.image.ptx.crypt

import io.github.dreammooncai.pvz2tool.pop.image.Texture
import io.github.dreammooncai.pvz2tool.pop.image.YFBitmap
import io.github.dreammooncai.pvz2tool.pop.image.setPixelArgb
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream

/**
 * RGB565_Block 32x32块解码器（严格复刻 C# PopStudio 原版）
 * 格式：R5 G6 B5 / 16位像素，Alpha固定255
 * 分块规则：32x32按块读取，超出宽高部分不绘制
 */
class RGB565_Block : Texture() {
    override suspend fun decode(bs: CoroutineBinaryStream, width: Int, height: Int): YFBitmap {
        val bitmap = YFBitmap(width, height)

        // C# 原版 32x32 分块遍历逻辑
        var i = 0
        while (i < height) {
            var w = 0
            while (w < width) {
                // 每个块 32x32
                for (j in 0 until 32) {
                    for (k in 0 until 32) {
                        val temp = bs.readUInt16().toUInt()

                        // 边界判断（与C#完全一致）
                        if (i + j < height && w + k < width) {
                            // C# 原版位运算：RGB565 → RGB888
                            val r = ((temp and 0xF800u) shr 8).toInt()
                            val g = ((temp and 0x7E0u) shr 3).toInt()
                            val b = ((temp and 0x1Fu) shl 3).toInt()
                            val a = 0xFF // 固定不透明

                            // 合成ARGB8888
                            val argb = (a shl 24) or (r shl 16) or (g shl 8) or b
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