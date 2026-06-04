package io.github.dreammooncai.pvz2tool.pop.image.ptx.crypt

import io.github.dreammooncai.pvz2tool.pop.image.Texture
import io.github.dreammooncai.pvz2tool.pop.image.YFBitmap
import io.github.dreammooncai.pvz2tool.pop.image.setPixelArgb
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream

/**
 * DXT1_RGB 解码器（严格 1:1 对齐 C# PopStudio 原版）
 * 4x4 块压缩，RGB565 颜色，无 Alpha（或 1bit Alpha）
 */
class DXT1_RGB : Texture() {

    override suspend fun decode(bs: CoroutineBinaryStream, width: Int, height: Int): YFBitmap {
        val bitmap = YFBitmap(width, height)
        val colorBuffer = IntArray(16) // 4x4 像素缓存

        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                // 读取 DXT1 8字节 块
                val c0 = bs.readUInt16().toInt()
                val c1 = bs.readUInt16().toInt()
                val bits0 = bs.readUInt16().toInt()
                val bits1 = bs.readUInt16().toInt()

                val colorBytes = ByteArray(4)
                colorBytes[0] = (bits0 and 0xFF).toByte()
                colorBytes[1] = (bits0 shr 8).toByte()
                colorBytes[2] = (bits1 and 0xFF).toByte()
                colorBytes[3] = (bits1 shr 8).toByte()

                // 颜色转换 RGB565 → ARGB8888
                val tempColor = IntArray(4)
                tempColor[0] = rgb565ToArgb(c0, 0xFF)
                tempColor[1] = rgb565ToArgb(c1, 0xFF)

                // C# 原版逻辑：c0 > c1 则四色模式，否则三色+透明
                if (c0 > c1) {
                    tempColor[2] = interpolate23(tempColor[0], tempColor[1])
                    tempColor[3] = interpolate13(tempColor[0], tempColor[1])
                } else {
                    tempColor[2] = interpolate11(tempColor[0], tempColor[1])
                    tempColor[3] = 0x00000000 // YFColor.Empty
                }

                // 解包 4x4 像素
                for (i in 0 until 4) {
                    var cb = colorBytes[i].toInt() and 0xFF
                    for (j in 0 until 4) {
                        val k = (i shl 2) or j
                        val bb = cb and 0b11
                        colorBuffer[k] = tempColor[bb]
                        cb = cb shr 2
                    }
                }

                // 写入位图（边界判断同 C#）
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

    /** RGB565 → ARGB8888 */
    private fun rgb565ToArgb(c: Int, alpha: Int): Int {
        val r = (c shr 11) and 0x1F
        val g = (c shr 5) and 0x3F
        val b = c and 0x1F
        val r8 = (r shl 3) or (r shr 2)
        val g8 = (g shl 2) or (g shr 4)
        val b8 = (b shl 3) or (b shr 2)
        return (alpha shl 24) or (r8 shl 16) or (g8 shl 8) or b8
    }

    /** (2c0 + c1)/3 */
    private fun interpolate23(c0: Int, c1: Int): Int {
        val r = ((2 * ((c0 shr 16) and 0xFF)) + ((c1 shr 16) and 0xFF)) / 3
        val g = ((2 * ((c0 shr 8) and 0xFF)) + ((c1 shr 8) and 0xFF)) / 3
        val b = ((2 * (c0 and 0xFF)) + (c1 and 0xFF)) / 3
        return 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
    }

    /** (c0 + 2c1)/3 */
    private fun interpolate13(c0: Int, c1: Int): Int {
        val r = (((c0 shr 16) and 0xFF) + 2 * ((c1 shr 16) and 0xFF)) / 3
        val g = (((c0 shr 8) and 0xFF) + 2 * ((c1 shr 8) and 0xFF)) / 3
        val b = ((c0 and 0xFF) + 2 * (c1 and 0xFF)) / 3
        return 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
    }

    /** (c0 + c1)/2 */
    private fun interpolate11(c0: Int, c1: Int): Int {
        val r = (((c0 shr 16) and 0xFF) + ((c1 shr 16) and 0xFF)) shr 1
        val g = (((c0 shr 8) and 0xFF) + ((c1 shr 8) and 0xFF)) shr 1
        val b = ((c0 and 0xFF) + (c1 and 0xFF)) shr 1
        return 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
    }
}