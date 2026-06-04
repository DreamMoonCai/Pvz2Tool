package io.github.dreammooncai.pvz2tool.pop.image.ptx.crypt

import io.github.dreammooncai.pvz2tool.pop.image.Texture
import io.github.dreammooncai.pvz2tool.pop.image.YFBitmap
import io.github.dreammooncai.pvz2tool.pop.image.setPixelArgb
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream

/**
 * ATC_RGBA4 解码器（严格 1:1 复刻 C# PopStudio 原版）
 * 4x4 块压缩 + 4bit alpha + ATC 双色 / 三色模式
 */
class ATC_RGBA4 : Texture() {
    override suspend fun decode(bs: CoroutineBinaryStream, width: Int, height: Int): YFBitmap {
        val image = YFBitmap(width, height)
        val colorBuffer = IntArray(16)
        val colorPalette = IntArray(4)
        val alphaBuffer = ByteArray(16)

        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                // ------------------------------
                // 1. 读取 64bit alpha 数据 (4bit/px)
                // ------------------------------
                val alphaWord = bs.readUInt64() // 小端
                var tempAlpha = alphaWord
                for (j in 0 until 4) {
                    for (i in 0 until 4) {
                        val a4 = (tempAlpha and 0xFUL).toInt()
                        alphaBuffer[(j shl 2) or i] = ((a4 shl 4) or a4).toByte()
                        tempAlpha = tempAlpha shr 4
                    }
                }

                // ------------------------------
                // 2. 读取两个颜色字
                // ------------------------------
                val color0 = bs.readUInt16().toUInt()
                val color1 = bs.readUInt16().toUInt()

                // 模式：color0 bit15 = 1 → 透明模式
                val mode = (color0 and 0x8000U) != 0U

                // ------------------------------
                // 3. 解析调色板
                // ------------------------------
                if (mode) {
                    // 模式1：包含黑色（透明色）
                    val r0 = ((color0 shr 10) and 0x1FU).toInt()
                    val g0 = ((color0 shr 5) and 0x1FU).toInt()
                    val b0 = (color0 and 0x1FU).toInt()
                    val c2 = makeColor888(r0, g0, b0)

                    val r1 = ((color1 shr 11) and 0x1FU).toInt()
                    val g1 = ((color1 shr 5) and 0x3FU).toInt()
                    val b1 = (color1 and 0x1FU).toInt()
                    val c3 = makeColor888(r1, g1, b1)

                    colorPalette[0] = 0xFF000000.toInt() // 黑
                    colorPalette[2] = c2
                    colorPalette[3] = c3
                    colorPalette[1] = mixColor(c2, c3, 0.25f)
                } else {
                    // 模式0：标准插值（5:3 / 3:5）
                    val r0 = ((color0 shr 10) and 0x1FU).toInt()
                    val g0 = ((color0 shr 5) and 0x1FU).toInt()
                    val b0 = (color0 and 0x1FU).toInt()
                    val c0 = makeColor888(r0, g0, b0)

                    val r1 = ((color1 shr 11) and 0x1FU).toInt()
                    val g1 = ((color1 shr 5) and 0x3FU).toInt()
                    val b1 = (color1 and 0x1FU).toInt()
                    val c3 = makeColor888(r1, g1, b1)

                    colorPalette[0] = c0
                    colorPalette[3] = c3
                    colorPalette[1] = mix53(c0, c3)
                    colorPalette[2] = mix35(c0, c3)
                }

                // ------------------------------
                // 4. 读取颜色索引 32bit
                // ------------------------------
                var colorFlags = bs.readUInt32().toInt()
                for (i in 0 until 16) {
                    val baseColor = colorPalette[colorFlags and 0x3]
                    val a = alphaBuffer[i].toInt() and 0xFF
                    colorBuffer[i] = (a shl 24) or (baseColor and 0x00FFFFFF)
                    colorFlags = colorFlags shr 2
                }

                // ------------------------------
                // 5. 写入 4x4 块
                // ------------------------------
                writeBlock(image, x, y, width, height, colorBuffer)

                x += 4
            }
            y += 4
        }
        return image
    }

    // 5/6bit → 8bit 颜色
    private fun makeColor888(r: Int, g: Int, b: Int): Int {
        val r8 = (r shl 3) or (r shr 2)
        val g8 = if (g and 0x40 != 0) (g shl 2) or (g shr 4) else (g shl 3) or (g shr 2)
        val b8 = (b shl 3) or (b shr 2)
        return 0xFF000000.toInt() or (r8 shl 16) or (g8 shl 8) or b8
    }

    // c0 * 5 + c1 *3 >>3
    private fun mix53(c0: Int, c1: Int): Int {
        val r = ((getR(c0) *5 + getR(c1)*3 +4) shr 3)
        val g = ((getG(c0) *5 + getG(c1)*3 +4) shr 3)
        val b = ((getB(c0) *5 + getB(c1)*3 +4) shr 3)
        return 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
    }

    // c0 *3 + c1 *5 >>3
    private fun mix35(c0: Int, c1: Int): Int {
        val r = ((getR(c0) *3 + getR(c1)*5 +4) shr 3)
        val g = ((getG(c0) *3 + getG(c1)*5 +4) shr 3)
        val b = ((getB(c0) *3 + getB(c1)*5 +4) shr 3)
        return 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
    }

    // 简单 25% 混合
    private fun mixColor(c0: Int, c1: Int, t: Float): Int {
        val r = (getR(c0) * (1-t) + getR(c1)*t).toInt()
        val g = (getG(c0) * (1-t) + getG(c1)*t).toInt()
        val b = (getB(c0) * (1-t) + getB(c1)*t).toInt()
        return 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
    }

    private fun getR(c: Int) = (c shr 16) and 0xFF
    private fun getG(c: Int) = (c shr 8) and 0xFF
    private fun getB(c: Int) = c and 0xFF

    // 写块
    private suspend fun writeBlock(
        image: YFBitmap,
        bx: Int, by: Int,
        width: Int, height: Int,
        colors: IntArray
    ) {
        for (y in 0 until 4) {
            for (x in 0 until 4) {
                val px = bx + x
                val py = by + y
                if (px < width && py < height) {
                    image.setPixelArgb(px, py, colors[y *4 +x])
                }
            }
        }
    }
}