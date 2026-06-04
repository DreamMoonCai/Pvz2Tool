package io.github.dreammooncai.pvz2tool.pop.image.ptx.crypt

import io.github.dreammooncai.pvz2tool.pop.image.Texture
import io.github.dreammooncai.pvz2tool.pop.image.YFBitmap
import io.github.dreammooncai.pvz2tool.pop.image.YFColor
import io.github.dreammooncai.pvz2tool.pop.image.forEachPixel
import io.github.dreammooncai.pvz2tool.pop.image.forEachPosition
import io.github.dreammooncai.pvz2tool.pop.image.setPixel
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream

/**
 * ABGR8888 解码器（严格复刻 C# PopStudio 原版）
 * 格式：A << 24 | B << 16 | G << 8 | R
 * 读取：R = 低8位, G = 8~15位, B = 16~23位, A = 高8位
 */
class ABGR8888 : Texture() {

    override suspend fun encode(bs: CoroutineBinaryStream, bitmap: YFBitmap): Int {
        bitmap.forEachPixel { color ->
            bs.writeUInt32(color.toAbgr().toUInt())
        }

        return bitmap.width shl 2
    }

    override suspend fun decode(bs: CoroutineBinaryStream, width: Int, height: Int): YFBitmap {
        val bitmap = YFBitmap(width, height)
        bitmap.forEachPosition { x, y ->
            val temp = bs.readUInt32()

            bitmap.setPixel(x, y, YFColor(
                (temp and 0xFFu).toInt(),
                ((temp shr 8) and 0xFFu).toInt(),
                ((temp shr 16) and 0xFFu).toInt(),
                (temp shr 24).toInt())
            )
        }

        return bitmap
    }
}