package io.github.dreammooncai.pvz2tool.pop.image.ptx.crypt

import io.github.dreammooncai.pvz2tool.pop.image.Texture
import io.github.dreammooncai.pvz2tool.pop.image.YFBitmap
import io.github.dreammooncai.pvz2tool.pop.image.setPixelArgb
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.max

/**
 * DXT5_RGBA Morton 序块解码器（严格复刻 C# PopStudio 原版逻辑）
 * 支持 32x32 分块 + Morton 坐标重排
 */
class DXT5_RGBA_MortonBlock : Texture() {

    // Morton 坐标表（与 C# 完全一致）
    private val mortonAry_x = intArrayOf(
        0,4,0,4,8,12,8,12,0,4,0,4,8,12,8,12,
        16,20,16,20,24,28,24,28,16,20,16,20,24,28,24,28,
        0,4,0,4,8,12,8,12,0,4,0,4,8,12,8,12,
        16,20,16,20,24,28,24,28,16,20,16,20,24,28,24,28
    )
    private val mortonAry_y = intArrayOf(
        0,0,4,4,0,0,4,4,8,8,12,12,8,8,12,12,
        0,0,4,4,0,0,4,4,8,8,12,12,8,8,12,12,
        16,16,20,20,16,16,20,20,24,24,28,28,24,24,28,28,
        16,16,20,20,16,16,20,20,24,24,28,28,24,24,28,28
    )

    override suspend fun decode(bs: CoroutineBinaryStream, width: Int, height: Int): YFBitmap {
        val bitmap = YFBitmap(width, height)
        val max = max(width, height)
        var newWidth = width
        var newHeight = height

        // 计算目标纹理尺寸（完全同 C#）
        if (max < 32) {
            if ((newWidth and (newWidth - 1)) != 0) {
                newWidth = (1 shl floor(log2(newWidth.toDouble())).toInt() + 1)
            }
            if ((newHeight and (newHeight - 1)) != 0) {
                newHeight = (1 shl floor(log2(newHeight.toDouble())).toInt() + 1)
            }
            if (newWidth != newHeight) {
                val m = max(newWidth, newHeight)
                newWidth = m
                newHeight = m
            }
        } else {
            if ((newWidth and 31) != 0) {
                newWidth = (newWidth or 31) + 1
            }
            if ((newHeight and 31) != 0) {
                newHeight = (newHeight or 31) + 1
            }
        }

        val colorBuffer = IntArray(16)

        if (newWidth < 32) {
            val maxDi = (newWidth * newWidth) shr 4
            for (di in 0 until maxDi) {
                decodeDxt5Block(bs, colorBuffer)
                val dx = mortonAry_x[di]
                val dy = mortonAry_y[di]
                writeBlock(bitmap, dx, dy, width, height, colorBuffer)
            }
        } else {
            var y = 0
            while (y < newHeight) {
                var x = 0
                while (x < newWidth) {
                    for (di in 0 until 64) {
                        decodeDxt5Block(bs, colorBuffer)
                        val dx = mortonAry_x[di]
                        val dy = mortonAry_y[di]
                        writeBlock(bitmap, x + dx, y + dy, width, height, colorBuffer)
                    }
                    x += 32
                }
                y += 32
            }
        }

        return bitmap
    }

    /**
     * 解码单个 DXT5 8字节 块（alpha） + 8字节（color）
     */
    private suspend fun decodeDxt5Block(bs: CoroutineBinaryStream, colorOut: IntArray) {
        // ------------------ Alpha 部分 ------------------
        val alpha0 = bs.readUInt8().toInt()
        val alpha1 = bs.readUInt8().toInt()
        val alphaBits =
            (bs.readUInt16().toLong() and 0xFFFFL) or
            ((bs.readUInt16().toLong() and 0xFFFFL) shl 16) or
            ((bs.readUInt16().toLong() and 0xFFFFL) shl 32)

        val alphaTable = IntArray(8)
        if (alpha0 > alpha1) {
            alphaTable[0] = alpha0
            alphaTable[1] = alpha1
            alphaTable[2] = (6 * alpha0 + alpha1) / 7
            alphaTable[3] = (5 * alpha0 + 2 * alpha1) / 7
            alphaTable[4] = (4 * alpha0 + 3 * alpha1) / 7
            alphaTable[5] = (3 * alpha0 + 4 * alpha1) / 7
            alphaTable[6] = (2 * alpha0 + 5 * alpha1) / 7
            alphaTable[7] = (alpha0 + 6 * alpha1) / 7
        } else {
            alphaTable[0] = alpha0
            alphaTable[1] = alpha1
            alphaTable[2] = (4 * alpha0 + alpha1) / 5
            alphaTable[3] = (3 * alpha0 + 2 * alpha1) / 5
            alphaTable[4] = (2 * alpha0 + 3 * alpha1) / 5
            alphaTable[5] = (alpha0 + 4 * alpha1) / 5
            alphaTable[6] = 0
            alphaTable[7] = 255
        }

        val alpha = IntArray(16)
        var abits = alphaBits
        for (i in 0 until 16) {
            alpha[i] = alphaTable[(abits and 7).toInt()]
            abits = abits shr 3
        }

        // ------------------ Color 部分 ------------------
        val c0 = bs.readUInt16().toInt()
        val c1 = bs.readUInt16().toInt()
        val colorBits = bs.readUInt32().toInt()

        val color = IntArray(4)
        color[0] = rgb565ToArgb(c0, 255)
        color[1] = rgb565ToArgb(c1, 255)
        color[2] = interpolateColor(color[0], color[1], 2)
        color[3] = interpolateColor(color[0], color[1], 1)

        // ------------------ 组装 4x4 像素 ------------------
        var bits = colorBits
        for (i in 0 until 16) {
            val c = color[bits and 3]
            val a = alpha[i]
            colorOut[i] = (a shl 24) or (c and 0x00FFFFFF)
            bits = bits shr 2
        }
    }

    /**
     * RGB565 → ARGB8888
     */
    private fun rgb565ToArgb(c: Int, a: Int): Int {
        val r = (c shr 11) and 0x1F
        val g = (c shr 5) and 0x3F
        val b = c and 0x1F
        val r8 = (r shl 3) or (r shr 2)
        val g8 = (g shl 2) or (g shr 4)
        val b8 = (b shl 3) or (b shr 2)
        return (a shl 24) or (r8 shl 16) or (g8 shl 8) or b8
    }

    /**
     * DXT1 颜色插值（1/3 或 2/3）
     */
    private fun interpolateColor(c1: Int, c2: Int, mode: Int): Int {
        val r1 = (c1 shr 16) and 0xFF
        val g1 = (c1 shr 8) and 0xFF
        val b1 = c1 and 0xFF
        val r2 = (c2 shr 16) and 0xFF
        val g2 = (c2 shr 8) and 0xFF
        val b2 = c2 and 0xFF

        return if (mode == 2) {
            val r = (2 * r1 + r2) / 3
            val g = (2 * g1 + g2) / 3
            val b = (2 * b1 + g2) / 3
            (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        } else {
            val r = (r1 + 2 * r2) / 3
            val g = (g1 + 2 * g2) / 3
            val b = (b1 + 2 * b2) / 3
            (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
    }

    /**
     * 将 4x4 块写入位图
     */
    private suspend fun writeBlock(bitmap: YFBitmap, bx: Int, by: Int, w: Int, h: Int, colors: IntArray) {
        for (y in 0 until 4) {
            for (x in 0 until 4) {
                val px = bx + x
                val py = by + y
                if (px < w && py < h) {
                    bitmap.setPixelArgb(px, py, colors[y * 4 + x])
                }
            }
        }
    }
}