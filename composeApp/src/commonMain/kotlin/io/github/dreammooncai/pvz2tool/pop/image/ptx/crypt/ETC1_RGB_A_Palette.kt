package io.github.dreammooncai.pvz2tool.pop.image.ptx.crypt

import io.github.dreammooncai.pvz2tool.pop.image.Texture
import io.github.dreammooncai.pvz2tool.pop.image.YFBitmap
import io.github.dreammooncai.pvz2tool.pop.image.YFColor
import io.github.dreammooncai.pvz2tool.pop.image.getPixelArgb
import io.github.dreammooncai.pvz2tool.pop.image.setPixelArgb
import io.github.dreammooncai.pvz2tool.pop.plugin.encode.ETCEncode
import io.github.dreammooncai.pvz2tool.pop.plugin.io.Endian
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream

/**
 * ETC1_RGB_A_Palette（修复版 + 1:1 C# 编码实现）
 */
class ETC1_RGB_A_Palette : Texture() {

    var alphaSize = 0

    /**
     * 【1:1 还原 C#】
     * 返回值：return width << 2
     */
    override suspend fun encode(bs: CoroutineBinaryStream, bitmap: YFBitmap): Int {
        val width = bitmap.width
        val height = bitmap.height
        val total = width * height
        val etcEndian = if (bs.endian == Endian.Small) Endian.Big else Endian.Small

        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                val colors = Array(16) { YFColor() }
                for (py in 0 until 4) {
                    for (px in 0 until 4) {
                        val sx = x + px
                        val sy = y + py
                        val argb = if (sx < width && sy < height) {
                            bitmap.getPixelArgb(sx, sy)
                        } else 0
                        colors[py * 4 + px] = YFColor.fromArgb(argb)
                    }
                }
                val block = ETCEncode.genETC1(colors)
                bs.writeUInt64(block, etcEndian)
                x += 4
            }
            y += 4
        }

        // ==================== 下面这段是你致命错误的修复 ====================
        bs.writeUInt8(0x10u)
        for (i in 0 until 16) bs.writeUInt8(i.toUByte())

        val odd = total % 2 == 1
        val pixelCount = total / 2
        alphaSize = pixelCount + 17

        for (i in 0 until pixelCount) {
            // 正确坐标：不是 y=0，是 i 平铺整个图片
            val idx1 = i * 2
            val idx2 = i * 2 + 1
            val x1 = idx1 % width
            val y1 = idx1 / width
            val x2 = idx2 % width
            val y2 = idx2 / width

            val a1 = bitmap.getPixelArgb(x1, y1) shr 24 and 0xFF
            val a2 = bitmap.getPixelArgb(x2, y2) shr 24 and 0xFF

            val b = ((a1 and 0xF0) or (a2 shr 4)).toUByte()
            bs.writeUInt8(b)
        }

        if (odd) {
            val idx = total - 1
            val x = idx % width
            val y = idx / width
            val a = bitmap.getPixelArgb(x, y) shr 24 and 0xF0
            bs.writeUInt8(a.toUByte())
        }

        return width shl 2
    }
    // ==============================================
    // 你原来的 decode 方法 完全不变
    // ==============================================
    override suspend fun decode(bs: CoroutineBinaryStream, width: Int, height: Int): YFBitmap {
        val bitmap = YFBitmap(width, height)
        val total = width * height
        val etcEndian = if (bs.endian == Endian.Small) Endian.Big else Endian.Small

        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                val temp = bs.readUInt64(etcEndian)
                val diffBit = (temp shr 33) and 1UL == 1UL
                val flipBit = (temp shr 32) and 1UL == 1UL

                var r1 = 0
                var g1 = 0
                var b1 = 0
                var r2 = 0
                var g2 = 0
                var b2 = 0

                if (diffBit) {
                    val r = ((temp shr 59) and 0x1FUL).toInt()
                    val g = ((temp shr 51) and 0x1FUL).toInt()
                    val b = ((temp shr 43) and 0x1FUL).toInt()

                    r1 = (r shl 3) or ((r and 0x1C) shr 2)
                    g1 = (g shl 3) or ((g and 0x1C) shr 2)
                    b1 = (b shl 3) or ((b and 0x1C) shr 2)

                    val dr = ((temp shr 56) and 0x7UL).toInt()
                    val dg = ((temp shr 48) and 0x7UL).toInt()
                    val db = ((temp shr 40) and 0x7UL).toInt()

                    val r2Raw = r + ((dr shl 29) shr 29)
                    val g2Raw = g + ((dg shl 29) shr 29)
                    val b2Raw = b + ((db shl 29) shr 29)

                    r2 = (r2Raw shl 3) or ((r2Raw and 0x1C) shr 2)
                    g2 = (g2Raw shl 3) or ((g2Raw and 0x1C) shr 2)
                    b2 = (b2Raw shl 3) or ((b2Raw and 0x1C) shr 2)
                } else {
                    r1 = ((temp shr 60) and 0xFUL).toInt() * 0x11
                    g1 = ((temp shr 52) and 0xFUL).toInt() * 0x11
                    b1 = ((temp shr 44) and 0xFUL).toInt() * 0x11
                    r2 = ((temp shr 56) and 0xFUL).toInt() * 0x11
                    g2 = ((temp shr 48) and 0xFUL).toInt() * 0x11
                    b2 = ((temp shr 40) and 0xFUL).toInt() * 0x11
                }

                val table1 = ((temp shr 37) and 0x7UL).toInt()
                val table2 = ((temp shr 34) and 0x7UL).toInt()

                for (py in 0 until 4) {
                    for (px in 0 until 4) {
                        if (x + px < width && y + py < height) {
                            val bitPos = (px shl 2) or py
                            val valBit = ((temp shr bitPos) and 0x1UL).toInt()
                            val neg = ((temp shr (bitPos + 16)) and 0x1UL) == 1UL

                            val isLeftOrTop = (flipBit && py < 2) || (!flipBit && px < 2)

                            val add = if (isLeftOrTop) {
                                ETCEncode.ETC1Modifiers[table1][valBit] * if (neg) -1 else 1
                            } else {
                                ETCEncode.ETC1Modifiers[table2][valBit] * if (neg) -1 else 1
                            }

                            val rr = if (isLeftOrTop) r1 else r2
                            val gg = if (isLeftOrTop) g1 else g2
                            val bb = if (isLeftOrTop) b1 else b2

                            val finalR = ETCEncode.colorClamp(rr + add)
                            val finalG = ETCEncode.colorClamp(gg + add)
                            val finalB = ETCEncode.colorClamp(bb + add)

                            bitmap.setPixelArgb(x + px, y + py, 0xFF shl 24 or (finalR shl 16) or (finalG shl 8) or finalB)
                        }
                    }
                }
                x += 4
            }
            y += 4
        }

        val indexNumber = bs.readUInt8().toInt()
        val bitsLength: Int
        val indexTable: ByteArray

        if (indexNumber == 0) {
            bitsLength = 1
            indexTable = byteArrayOf(0, 0xFF.toByte())
        } else {
            bitsLength = if (indexNumber == 1) 1 else 31 - Integer.numberOfLeadingZeros(indexNumber - 1) + 1
            indexTable = ByteArray(indexNumber)
            for (i in 0 until indexNumber) {
                val b = bs.readUInt8()
                indexTable[i] = (((b.toInt() and 0xFF) shl 4) or (b.toInt() and 0x0F)).toByte()
            }
        }

        var bitBuffer = 0    // 字节缓存
        var bitPos = 0       // 位指针（C# 初始 = 0）

        for (i in 0 until total) {
            var idx = 0
            // 读取 bitsLength 位（高位在前）
            for (j in bitsLength - 1 downTo 0) {
                if (bitPos == 0) {
                    bitBuffer = bs.readUInt8().toInt() // 从当前流位置读下一个字节
                }
                // C# 位读取逻辑：BitPosition = (BitPosition + 7) % 8
                bitPos = (bitPos + 7) % 8
                val bit = (bitBuffer shr bitPos) and 1
                idx = idx or (bit shl j)
            }
            // 写入 Alpha 通道
            val alpha = indexTable[idx].toInt() and 0xFF
            val px = i % width
            val py = i / width
            val c = bitmap.getPixelArgb(px, py)
            bitmap.setPixelArgb(px, py, (alpha shl 24) or (c and 0x00FFFFFF))
        }

        return bitmap
    }
}