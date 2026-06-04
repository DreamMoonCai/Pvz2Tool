package io.github.dreammooncai.pvz2tool.pop.image.ptx.crypt

import io.github.dreammooncai.pvz2tool.pop.image.Texture
import io.github.dreammooncai.pvz2tool.pop.image.YFBitmap
import io.github.dreammooncai.pvz2tool.pop.image.setPixelArgb
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream

/**
 * RGB565 格式解码器（严格对齐 C# PopStudio 原版）
 * 像素格式：16位，R5 G6 B5
 * 读取逻辑与位移、扩位算法完全一致
 */
class RGB565 : Texture() {
    override suspend fun decode(bs: CoroutineBinaryStream, width: Int, height: Int): YFBitmap {
        val bitmap = YFBitmap(width, height)

        for (y in 0 until height) {
            val row = IntArray(width)

            for (x in 0 until width) {
                val temp = bs.readUInt16().toUInt()

                // C# 原版位移逻辑 ↓ 完全一样
                val r = (temp shr 11) and 0x1Fu // 5 bit
                val g = (temp shr 5) and 0x3Fu  // 6 bit
                val b = temp and 0x1Fu          // 5 bit

                // C# 原版扩位公式：将 5/6 位还原为 8 位颜色
                val r8 = ((r shl 3) or (r shr 2)).toInt()
                val g8 = ((g shl 2) or (g shr 4)).toInt()
                val b8 = ((b shl 3) or (b shr 2)).toInt()
                val a8 = 0xFF // 不透明

                // ARGB 格式
                row[x] = (a8 shl 24) or (r8 shl 16) or (g8 shl 8) or b8
            }
            for (x in 0 until width) {
                bitmap.setPixelArgb(x, y, row[x])
            }
        }

        return bitmap
    }
}