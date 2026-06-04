package io.github.dreammooncai.pvz2tool.pop.image.ptx.crypt

import io.github.dreammooncai.pvz2tool.pop.image.Texture
import io.github.dreammooncai.pvz2tool.pop.image.YFBitmap
import io.github.dreammooncai.pvz2tool.pop.image.setPixelArgb
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream

/**
 * RGBA5551 格式解码器（严格对齐 C# PopStudio 原版）
 * 16位像素：R5 G5 B5 A1
 * 扩位公式：(x << 3) | (x >> 2)
 * Alpha：1位 → 0 或 255
 */
class RGBA5551 : Texture() {

    override suspend fun decode(bs: CoroutineBinaryStream, width: Int, height: Int): YFBitmap {
        val bitmap = YFBitmap(width, height)

        for (y in 0 until height) {
            val row = IntArray(width)

            for (x in 0 until width) {
                val temp = bs.readUInt16().toUInt()

                // C# 原版位运算 完全一致
                val r = (temp and 0xF800u) shr 11 // R 5bit
                val g = (temp and 0x7C0u) shr 6  // G 5bit
                val b = (temp and 0x3Eu) shr 1   // B 5bit
                val a = temp and 0x1u            // A 1bit

                // C# 原版 5bit → 8bit 扩位算法
                val r8 = ((r shl 3) or (r shr 2)).toInt()
                val g8 = ((g shl 3) or (g shr 2)).toInt()
                val b8 = ((b shl 3) or (b shr 2)).toInt()

                // C# 关键：(byte)-(temp & 1) → 1位alpha转 0 / 255
                val a8 = if (a == 0u) 0 else 255

                // 合成 ARGB8888
                row[x] = (a8 shl 24) or (r8 shl 16) or (g8 shl 8) or b8
            }
            for (x in 0 until width) {
                bitmap.setPixelArgb(x, y, row[x])
            }
        }

        return bitmap
    }
}