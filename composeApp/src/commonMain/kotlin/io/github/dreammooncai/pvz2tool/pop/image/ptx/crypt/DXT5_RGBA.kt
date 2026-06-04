package io.github.dreammooncai.pvz2tool.pop.image.ptx.crypt

import io.github.dreammooncai.pvz2tool.pop.image.Texture
import io.github.dreammooncai.pvz2tool.pop.image.YFBitmap
import io.github.dreammooncai.pvz2tool.pop.image.setPixelArgb
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream

/**
 * DXT5_RGBA 标准块解码器（严格对齐 C# PopStudio 原版）
 * 16字节/4x4块：8字节Alpha + 8字节DXT1颜色
 */
class DXT5_RGBA : Texture() {

    override suspend fun decode(bs: CoroutineBinaryStream, width: Int, height: Int): YFBitmap {
        val bitmap = YFBitmap(width, height)
        val colorBuffer = IntArray(16) // 存储4x4解码后的颜色

        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                // 解码一个DXT5块 (16字节)
                decodeDxt5Block(bs, colorBuffer)

                // 将4x4块写入位图
                for (dy in 0 until 4) {
                    for (dx in 0 until 4) {
                        val px = x + dx
                        val py = y + dy
                        if (px < width && py < height) {
                            bitmap.setPixelArgb(px, py, colorBuffer[dy * 4 + dx])
                        }
                    }
                }
                x += 4
            }
            y += 4
        }

        return bitmap
    }

    /**
     * 解码单个DXT5块 (16字节)
     * 与C# Read方法内逻辑完全一致
     */
    private suspend fun decodeDxt5Block(bs: CoroutineBinaryStream, colorOut: IntArray) {
        // --------------------------
        // 1. 读取并解压缩 ALPHA (8字节)
        // --------------------------
        val alpha0 = bs.readUInt8().toInt()
        val alpha1 = bs.readUInt8().toInt()
        val alphaBits = (bs.readUInt16().toLong() and 0xFFFFL) or
                        ((bs.readUInt16().toLong() and 0xFFFFL) shl 16) or
                        ((bs.readUInt16().toLong() and 0xFFFFL) shl 32)

        val alphaTable = IntArray(8)
        if (alpha0 > alpha1) {
            // 8级alpha插值
            alphaTable[0] = alpha0
            alphaTable[1] = alpha1
            alphaTable[2] = (6 * alpha0 + alpha1) / 7
            alphaTable[3] = (5 * alpha0 + 2 * alpha1) / 7
            alphaTable[4] = (4 * alpha0 + 3 * alpha1) / 7
            alphaTable[5] = (3 * alpha0 + 4 * alpha1) / 7
            alphaTable[6] = (2 * alpha0 + 5 * alpha1) / 7
            alphaTable[7] = (alpha0 + 6 * alpha1) / 7
        } else {
            // 6级alpha插值 + 0和255
            alphaTable[0] = alpha0
            alphaTable[1] = alpha1
            alphaTable[2] = (4 * alpha0 + alpha1) / 5
            alphaTable[3] = (3 * alpha0 + 2 * alpha1) / 5
            alphaTable[4] = (2 * alpha0 + 3 * alpha1) / 5
            alphaTable[5] = (alpha0 + 4 * alpha1) / 5
            alphaTable[6] = 0
            alphaTable[7] = 255
        }

        // 读取16个像素的alpha索引
        val alpha = IntArray(16)
        var abits = alphaBits
        for (i in 0 until 16) {
            alpha[i] = alphaTable[(abits and 7).toInt()]
            abits = abits shr 3
        }

        // --------------------------
        // 2. 读取并解压缩颜色 (8字节 DXT1)
        // --------------------------
        val c0 = bs.readUInt16().toInt()
        val c1 = bs.readUInt16().toInt()
        val colorBits = bs.readUInt32().toInt()

        val color = IntArray(4)
        color[0] = rgb565ToArgb(c0, 0xFF) // C#中的tempcolor[0]
        color[1] = rgb565ToArgb(c1, 0xFF) // C#中的tempcolor[1]
        color[2] = interpolateColor(color[0], color[1], 2) // (2c0 + c1)/3
        color[3] = interpolateColor(color[0], color[1], 1) // (c0 + 2c1)/3

        // --------------------------
        // 3. 组合颜色与Alpha
        // --------------------------
        var bits = colorBits
        for (i in 0 until 16) {
            val c = color[bits and 3]
            val a = alpha[i]
            // 替换Alpha通道
            colorOut[i] = (a shl 24) or (c and 0x00FFFFFF)
            bits = bits shr 2
        }
    }

    /**
     * RGB565 转 ARGB8888 (与C#完全一致)
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
     * DXT1颜色插值 (与C#完全一致)
     */
    private fun interpolateColor(c1: Int, c2: Int, mode: Int): Int {
        val r1 = (c1 shr 16) and 0xFF
        val g1 = (c1 shr 8) and 0xFF
        val b1 = c1 and 0xFF
        val r2 = (c2 shr 16) and 0xFF
        val g2 = (c2 shr 8) and 0xFF
        val b2 = c2 and 0xFF

        val r: Int
        val g: Int
        val b: Int
        if (mode == 2) {
            r = (2 * r1 + r2 + 1) / 3
            g = (2 * g1 + g2 + 1) / 3
            b = (2 * b1 + b2 + 1) / 3
        } else {
            r = (r1 + 2 * r2 + 1) / 3
            g = (g1 + 2 * g2 + 1) / 3
            b = (b1 + 2 * b2 + 1) / 3
        }
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}