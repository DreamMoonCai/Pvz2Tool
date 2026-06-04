package io.github.dreammooncai.pvz2tool.pop.image.ptx.crypt

import io.github.dreammooncai.pvz2tool.pop.image.Texture
import io.github.dreammooncai.pvz2tool.pop.image.YFBitmap
import io.github.dreammooncai.pvz2tool.pop.image.setPixelArgb
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream

/**
 * DXT3_RGBA 解码器（严格复刻 C# PopStudio 原版）
 * 格式：8字节 Alpha(4x4x4bit) + 8字节 DXT1 RGB
 * Alpha 直接 4bit → 8bit (a << 4 | a)
 */
class DXT3_RGBA : Texture() {

    override suspend fun decode(bs: CoroutineBinaryStream, width: Int, height: Int): YFBitmap {
        val bitmap = YFBitmap(width, height)
        val colorBuffer = IntArray(16)
        val alphaBuffer = ByteArray(16)

        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                val tempa = shortArrayOf(0, 0, 0, 0)
                for (i in 0 until 4) {
                    tempa[i] = bs.readUInt16().toShort()
                }

                // 解码 Alpha 4bit → 8bit（与 C# 完全一致）
                for (j in 0 until 4) {
                    var aVal = tempa[j].toInt()
                    for (i in 0 until 4) {
                        val alpha4 = aVal and 0xF
                        alphaBuffer[(j shl 2) or i] = ((alpha4 shl 4) or alpha4).toByte()
                        aVal = aVal shr 4
                    }
                }

                // 读取 DXT1 颜色部分
                val c0 = bs.readUInt16().toInt()
                val c1 = bs.readUInt16().toInt()
                val bits0 = bs.readUInt16().toInt()
                val bits1 = bs.readUInt16().toInt()

                val colorBytes = ByteArray(4)
                colorBytes[0] = (bits0 and 0xFF).toByte()
                colorBytes[1] = (bits0 shr 8).toByte()
                colorBytes[2] = (bits1 and 0xFF).toByte()
                colorBytes[3] = (bits1 shr 8).toByte()

                // RGB565 → ARGB8888
                val tempColor = IntArray(4)
                tempColor[0] = rgb565ToArgb(c0, 0xFF)
                tempColor[1] = rgb565ToArgb(c1, 0xFF)
                tempColor[2] = interpolate23(tempColor[0], tempColor[1])
                tempColor[3] = interpolate13(tempColor[0], tempColor[1])

                // 解包 4x4 颜色 + 合并 Alpha
                for (i in 0 until 4) {
                    var cb = colorBytes[i].toInt() and 0xFF
                    for (j in 0 until 4) {
                        val k = (i shl 2) or j
                        val bb = cb and 0b11
                        val rgb = tempColor[bb]
                        val a = alphaBuffer[k].toInt() and 0xFF
                        colorBuffer[k] = (a shl 24) or (rgb and 0x00FFFFFF)
                        cb = cb shr 2
                    }
                }

                // 写入像素
                for (i in 0 until 4) {
                    for (j in 0 until 4) {
                        val px = x + j
                        val py = y + i
                        if (px < width && py < height) {
                            bitmap.setPixelArgb(px, py, colorBuffer[(i shl 2) or j])
                        }
                    }
                }

                x += 4
            }
            y += 4
        }

        return bitmap
    }

    // RGB565 转 ARGB8888（与 C# 一致）
    private fun rgb565ToArgb(c: Int, alpha: Int): Int {
        val r = (c shr 11) and 0x1F
        val g = (c shr 5) and 0x3F
        val b = c and 0x1F
        val r8 = (r shl 3) or (r shr 2)
        val g8 = (g shl 2) or (g shr 4)
        val b8 = (b shl 3) or (b shr 2)
        return (alpha shl 24) or (r8 shl 16) or (g8 shl 8) or b8
    }

    // (2c0 + c1)/3
    private fun interpolate23(c0: Int, c1: Int): Int {
        val r = ((2 * ((c0 shr 16) and 0xFF)) + ((c1 shr 16) and 0xFF)) / 3
        val g = ((2 * ((c0 shr 8) and 0xFF)) + ((c1 shr 8) and 0xFF)) / 3
        val b = ((2 * (c0 and 0xFF)) + (c1 and 0xFF)) / 3
        return 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
    }

    // (c0 + 2c1)/3
    private fun interpolate13(c0: Int, c1: Int): Int {
        val r = (((c0 shr 16) and 0xFF) + 2 * ((c1 shr 16) and 0xFF)) / 3
        val g = (((c0 shr 8) and 0xFF) + 2 * ((c1 shr 8) and 0xFF)) / 3
        val b = ((c0 and 0xFF) + 2 * (c1 and 0xFF)) / 3
        return 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
    }
}