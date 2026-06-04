package io.github.dreammooncai.pvz2tool.pop.image.ptx.crypt

import io.github.dreammooncai.pvz2tool.pop.image.Texture
import io.github.dreammooncai.pvz2tool.pop.image.YFBitmap
import io.github.dreammooncai.pvz2tool.pop.image.setPixelArgb
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream

/**
 * ARGB8888格式解码器
 * 每个像素4字节，顺序为：A, R, G, B
 */
class ARGB8888 : Texture() {
    override suspend fun decode(bs: CoroutineBinaryStream, width: Int, height: Int): YFBitmap {
        val bitmap = YFBitmap(width, height)

        for (y in 0 until height) {
            val row = IntArray(width)

            for (x in 0 until width) {
                val temp = bs.readUInt32()
                val b = ((temp shr 16) and 0xFFu)
                val g = ((temp shr 8) and 0xFFu)
                val r = (temp and 0xFFu)
                val a = ((temp shr 24) and 0xFFu)

                row[x] = ((a shl 24) or r or (g shl 8) or (b shl 16)).toInt()
            }
            for (x in 0 until width) {
                bitmap.setPixelArgb(x, y, row[x])
            }
        }

        return bitmap
    }
}