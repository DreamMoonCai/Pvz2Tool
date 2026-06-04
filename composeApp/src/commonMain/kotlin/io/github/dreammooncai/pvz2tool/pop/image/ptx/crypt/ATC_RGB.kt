package io.github.dreammooncai.pvz2tool.pop.image.ptx.crypt

import io.github.dreammooncai.pvz2tool.pop.image.Texture
import io.github.dreammooncai.pvz2tool.pop.image.YFBitmap
import io.github.dreammooncai.pvz2tool.pop.image.setPixelArgb
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream
import kotlin.math.abs

/**
 * ATC_RGB 解码器（严格 1:1 复刻 C# PopStudio 原版）
 * 4×4 块压缩，8字节/块
 * 支持双色模式 + 插值
 */
class ATC_RGB : Texture() {

    override suspend fun decode(bs: CoroutineBinaryStream, width: Int, height: Int): YFBitmap {
        val bitmap = YFBitmap(width, height)
        val colorBuffer = IntArray(4)
        val pixelBuffer = IntArray(16)

        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                // 读取 2 个颜色字
                val color0 = (bs.readUInt8().toUShort() or ((bs.readUInt8().toInt()) shl 8).toUShort()).toInt()
                val color1 = (bs.readUInt8().toUShort() or ((bs.readUInt8().toInt()) shl 8).toUShort()).toInt()
                val mode = (color0 and 0x8000) != 0

                // 解析颜色表
                if (mode) {
                    // 模式 1：color0=555，color1=565 + 黑白插值
                    val r = (color0 shr 10) and 0x1F
                    val g = (color0 shr 5) and 0x1F
                    val b = color0 and 0x1F
                    val c2 = argb(
                        (r shl 3) or (r shr 2),
                        (g shl 3) or (g shr 2),
                        (b shl 3) or (b shr 2)
                    )

                    val r1 = (color1 shr 11) and 0x1F
                    val g1 = (color1 shr 5) and 0x3F
                    val b1 = color1 and 0x1F
                    val c3 = argb(
                        (r1 shl 3) or (r1 shr 2),
                        (g1 shl 2) or (g1 shr 4),
                        (b1 shl 3) or (b1 shr 2)
                    )

                    colorBuffer[0] = argb(0, 0, 0)
                    colorBuffer[1] = argb(
                        abs((getR(c2) shl 2) - getR(c3)) shr 2,
                        abs((getG(c2) shl 2) - getG(c3)) shr 2,
                        abs((getB(c2) shl 2) - getB(c3)) shr 2
                    )
                    colorBuffer[2] = c2
                    colorBuffer[3] = c3
                } else {
                    // 模式 0：标准插值 555 + 565
                    val r = (color0 shr 10) and 0x1F
                    val g = (color0 shr 5) and 0x1F
                    val b = color0 and 0x1F
                    val c0 = argb(
                        (r shl 3) or (r shr 2),
                        (g shl 3) or (g shr 2),
                        (b shl 3) or (b shr 2)
                    )

                    val r1 = (color1 shr 11) and 0x1F
                    val g1 = (color1 shr 5) and 0x3F
                    val b1 = color1 and 0x1F
                    val c3 = argb(
                        (r1 shl 3) or (r1 shr 2),
                        (g1 shl 2) or (g1 shr 4),
                        (b1 shl 3) or (b1 shr 2)
                    )

                    colorBuffer[0] = c0
                    colorBuffer[3] = c3
                    colorBuffer[1] = argb(
                        (getR(c0) * 5 + getR(c3) * 3 + 4) shr 3,
                        (getG(c0) * 5 + getG(c3) * 3 + 4) shr 3,
                        (getB(c0) * 5 + getB(c3) * 3 + 4) shr 3
                    )
                    colorBuffer[2] = argb(
                        (getR(c0) * 3 + getR(c3) * 5 + 4) shr 3,
                        (getG(c0) * 3 + getG(c3) * 5 + 4) shr 3,
                        (getB(c0) * 3 + getB(c3) * 5 + 4) shr 3
                    )
                }

                // 读取 4 字节索引
                var colorFlags = bs.readUInt32().toLong()
                for (i in 0 until 16) {
                    pixelBuffer[i] = colorBuffer[(colorFlags and 0x3).toInt()]
                    colorFlags = colorFlags shr 2
                }

                // 写入 4×4 块
                for (dy in 0 until 4) {
                    for (dx in 0 until 4) {
                        val px = x + dx
                        val py = y + dy
                        if (px < width && py < height) {
                            bitmap.setPixelArgb(px, py, pixelBuffer[(dy shl 2) or dx])
                        }
                    }
                }

                x += 4
            }
            y += 4
        }

        return bitmap
    }

    private fun argb(r: Int, g: Int, b: Int): Int {
        return 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
    }

    private fun getR(argb: Int): Int = (argb shr 16) and 0xFF
    private fun getG(argb: Int): Int = (argb shr 8) and 0xFF
    private fun getB(argb: Int): Int = argb and 0xFF
}