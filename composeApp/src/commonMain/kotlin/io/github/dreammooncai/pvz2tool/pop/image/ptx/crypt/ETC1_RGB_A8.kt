package io.github.dreammooncai.pvz2tool.pop.image.ptx.crypt

import io.github.dreammooncai.pvz2tool.pop.image.Texture
import io.github.dreammooncai.pvz2tool.pop.image.YFBitmap
import io.github.dreammooncai.pvz2tool.pop.image.getPixelArgb
import io.github.dreammooncai.pvz2tool.pop.image.setPixelArgb
import io.github.dreammooncai.pvz2tool.pop.plugin.encode.ETCEncode
import io.github.dreammooncai.pvz2tool.pop.plugin.io.Endian
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream

/**
 * ETC1_RGB_A8 解码器（严格复刻 C# PopStudio 原版）
 * 格式：ETC1 压缩 RGB + 独立 8bit Alpha 通道
 * 块大小：4x4
 */
class ETC1_RGB_A8 : Texture() {
    override suspend fun decode(bs: CoroutineBinaryStream, width: Int, height: Int): YFBitmap {
        val bitmap = YFBitmap(width, height)
        val etcEndian = if (bs.endian == Endian.Small) Endian.Big else Endian.Small

        // == 第一步：解码 ETC1 RGB 数据 ==
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
                    // 差异模式
                    val r = ((temp shr 59) and 0x1FUL).toInt()
                    val g = ((temp shr 51) and 0x1FUL).toInt()
                    val b = ((temp shr 43) and 0x1FUL).toInt()

                    r1 = (r shl 3) or ((r and 0x1C) shr 2)
                    g1 = (g shl 3) or ((g and 0x1C) shr 2)
                    b1 = (b shl 3) or ((b and 0x1C) shr 2)

                    val dr = ((temp shr 56) and 0x7UL).toByte().toInt()
                    val dg = ((temp shr 48) and 0x7UL).toByte().toInt()
                    val db = ((temp shr 40) and 0x7UL).toByte().toInt()

                    r2 = ((r + dr) shl 3) or (((r + dr) and 0x1C) shr 2)
                    g2 = ((g + dg) shl 3) or (((g + dg) and 0x1C) shr 2)
                    b2 = ((b + db) shl 3) or (((b + db) and 0x1C) shr 2)
                } else {
                    // 个体模式
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

                            val add = if ((flipBit && py < 2) || (!flipBit && px < 2)) {
                                ETCEncode.ETC1Modifiers[table1][valBit] * if (neg) -1 else 1
                            } else {
                                ETCEncode.ETC1Modifiers[table2][valBit] * if (neg) -1 else 1
                            }

                            val r = ETCEncode.colorClamp(r1 + add)
                            val g = ETCEncode.colorClamp(g1 + add)
                            val b = ETCEncode.colorClamp(b1 + add)
                            bitmap.setPixelArgb(x + px, y + py, 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b)
                        }
                    }
                }
                x += 4
            }
            y += 4
        }

        // == 第二步：读取完整的 A8 透明度通道（C# 原版逻辑）==
        val total = width * height
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