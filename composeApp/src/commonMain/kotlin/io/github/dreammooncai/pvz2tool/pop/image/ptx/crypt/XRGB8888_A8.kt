package io.github.dreammooncai.pvz2tool.pop.image.ptx.crypt

import io.github.dreammooncai.pvz2tool.pop.image.Texture
import io.github.dreammooncai.pvz2tool.pop.image.YFBitmap
import io.github.dreammooncai.pvz2tool.pop.image.getPixelArgb
import io.github.dreammooncai.pvz2tool.pop.image.setPixelArgb
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream

/**
 * XRGB8888_A8 解码器（严格复刻 C# PopStudio 原版）
 * 格式：先读 4 字节 XRGB (忽略最高字节) → 再读 1 字节 A8 透明度
 */
class XRGB8888_A8 : Texture() {

    override suspend fun decode(bs: CoroutineBinaryStream, width: Int, height: Int): YFBitmap {
        val bitmap = YFBitmap(width, height)
        val total = width * height

        // == 第一步：读取 XRGB8888 (R<<16 | G<<8 | B) ==
        for (i in 0 until total) {
            val temp = bs.readUInt32()
            val r = ((temp shr 16) and 0xFFu).toInt()
            val g = ((temp shr 8) and 0xFFu).toInt()
            val b = (temp and 0xFFu).toInt()
            val argb = 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
            val x = i % width
            val y = i / width
            bitmap.setPixelArgb(x, y, argb)
        }

        // == 第二步：读取独立 A8 透明度通道，覆盖 Alpha ==
        for (i in 0 until total) {
            val alpha = bs.readUInt8().toInt()
            val x = i % width
            val y = i / width
            val color = bitmap.getPixelArgb(x, y)
            val newColor = (alpha shl 24) or (color and 0x00FFFFFF)
            bitmap.setPixelArgb(x, y, newColor)
        }

        return bitmap
    }
}