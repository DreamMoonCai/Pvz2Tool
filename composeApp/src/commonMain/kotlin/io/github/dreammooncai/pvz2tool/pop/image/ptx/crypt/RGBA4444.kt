package io.github.dreammooncai.pvz2tool.pop.image.ptx.crypt

import io.github.dreammooncai.pvz2tool.pop.image.Texture
import io.github.dreammooncai.pvz2tool.pop.image.YFBitmap
import io.github.dreammooncai.pvz2tool.pop.image.setPixelArgb
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream

/**
 * RGBA4444 格式解码器（严格对齐 C# PopStudio 原版）
 * 16位像素：R4 G4 B4 A4
 * 扩位公式：(x << 4) | x
 */
class RGBA4444 : Texture() {
    override suspend fun decode(bs: CoroutineBinaryStream, width: Int, height: Int): YFBitmap {
        val bitmap = YFBitmap(width, height)

        for (y in 0 until height) {
            val row = IntArray(width)

            for (x in 0 until width) {
                val temp = bs.readUInt16().toUInt()

                // C# 原版位运算 ↓ 完全一致
                val r = (temp shr 12) and 0x0Fu // R 4bit
                val g = (temp shr 8)  and 0x0Fu // G 4bit
                val b = (temp shr 4)  and 0x0Fu // B 4bit
                val a = temp         and 0x0Fu // A 4bit

                // C# 原版 4bit → 8bit 扩位公式：(x << 4) | x
                val r8 = ((r shl 4) or r).toInt()
                val g8 = ((g shl 4) or g).toInt()
                val b8 = ((b shl 4) or b).toInt()
                val a8 = ((a shl 4) or a).toInt()

                // 输出 ARGB8888
                row[x] = (a8 shl 24) or (r8 shl 16) or (g8 shl 8) or b8
            }
            for (x in 0 until width) {
                bitmap.setPixelArgb(x, y, row[x])
            }
        }

        return bitmap
    }
}