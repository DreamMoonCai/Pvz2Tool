package io.github.dreammooncai.pvz2tool.pop.image.ptx.crypt

import io.github.dreammooncai.pvz2tool.pop.image.Texture
import io.github.dreammooncai.pvz2tool.pop.image.YFBitmap
import io.github.dreammooncai.pvz2tool.pop.image.setPixelArgb
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream

/**
 * ARGB8888_Padding（严格 1:1 复刻 C# PopStudio 原版）
 * 每行按 64 对齐，blockSize = w * 4（w 是对齐后的宽度）
 */
class ARGB8888_Padding : Texture() {
    var blockSize = 0

    override suspend fun decode(bs: CoroutineBinaryStream, width: Int, height: Int): YFBitmap {
        // 你提供的 C# 对齐逻辑：w 对齐到 64
        var alignedWidth = width
        if (alignedWidth % 64 != 0) {
            alignedWidth = (alignedWidth / 64) * 64 + 64
        }
        val blockSize = alignedWidth shl 2 // *4

        return decode(bs, width, height, blockSize)
    }

    suspend fun decode(bs: CoroutineBinaryStream, width: Int, height: Int, blockSize: Int): YFBitmap {
        val bitmap = YFBitmap(width, height)
        val startPos = bs.readPosition
        var line = 0

        for (y in 0 until height) {
            // 读取一行 ARGB8888
            for (x in 0 until width) {
                val temp = bs.readUInt32()
                val a = (temp shr 24).toInt() and 0xFF
                val r = (temp shr 16).toInt() and 0xFF
                val g = (temp shr 8).toInt() and 0xFF
                val b = temp.toInt() and 0xFF
                bitmap.setPixelArgb(x, y, (a shl 24) or (r shl 16) or (g shl 8) or b)
            }

            // 跳过填充：跳转到下一行
            line++
            bs.readPosition = startPos + line * blockSize
        }

        return bitmap
    }
}