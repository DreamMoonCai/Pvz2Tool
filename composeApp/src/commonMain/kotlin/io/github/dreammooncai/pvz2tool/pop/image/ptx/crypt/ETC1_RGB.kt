package io.github.dreammooncai.pvz2tool.pop.image.ptx.crypt

import io.github.dreammooncai.pvz2tool.pop.image.Texture
import io.github.dreammooncai.pvz2tool.pop.image.YFBitmap
import io.github.dreammooncai.pvz2tool.pop.image.setPixelArgb
import io.github.dreammooncai.pvz2tool.pop.plugin.encode.ETCEncode
import io.github.dreammooncai.pvz2tool.pop.plugin.io.Endian
import io.github.dreammooncai.pvz2tool.pop.plugin.io.CoroutineBinaryStream

/**
 * ETC1 RGB 解码器（严格对齐 C# PopStudio 原版）
 * 4×4分块，8字节/块
 */
class ETC1_RGB : Texture() {

    override suspend fun decode(bs: CoroutineBinaryStream, width: Int, height: Int): YFBitmap {
        val bitmap = YFBitmap(width, height)

        // C#：大端/小端反转
        val codeEndian = true // 与 bs.Endian == Small 等价

        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                val temp = (if (codeEndian) bs.readUInt64(Endian.Big) else bs.readUInt64(Endian.Small)).toUInt()

                val diffbit = (temp shr 33) and 1u == 1u
                val flipbit = (temp shr 32) and 1u == 1u

                var r1 = 0
                var g1 = 0
                var b1 = 0
                var r2 = 0
                var g2 = 0
                var b2 = 0

                if (diffbit) {
                    // 5位差分模式
                    val r = ((temp shr 59) and 0x1Fu).toInt()
                    val g = ((temp shr 51) and 0x1Fu).toInt()
                    val b = ((temp shr 43) and 0x1Fu).toInt()

                    r1 = (r shl 3) or ((r and 0x1C) shr 2)
                    g1 = (g shl 3) or ((g and 0x1C) shr 2)
                    b1 = (b shl 3) or ((b and 0x1C) shr 2)

                    val dr = ((temp shr 56) and 0x7u).toInt().toSigned3()
                    val dg = ((temp shr 48) and 0x7u).toInt().toSigned3()
                    val db = ((temp shr 40) and 0x7u).toInt().toSigned3()

                    val r2c = r + dr
                    val g2c = g + dg
                    val b2c = b + db

                    r2 = (r2c shl 3) or ((r2c and 0x1C) shr 2)
                    g2 = (g2c shl 3) or ((g2c and 0x1C) shr 2)
                    b2 = (b2c shl 3) or ((b2c and 0x1C) shr 2)
                } else {
                    // 4位模式
                    r1 = ((temp shr 60) and 0xFu).toInt() * 0x11
                    g1 = ((temp shr 52) and 0xFu).toInt() * 0x11
                    b1 = ((temp shr 44) and 0xFu).toInt() * 0x11
                    r2 = ((temp shr 56) and 0xFu).toInt() * 0x11
                    g2 = ((temp shr 48) and 0xFu).toInt() * 0x11
                    b2 = ((temp shr 40) and 0xFu).toInt() * 0x11
                }

                val table1 = ((temp shr 37) and 0x7u).toInt()
                val table2 = ((temp shr 34) and 0x7u).toInt()

                for (py in 0 until 4) {
                    for (px in 0 until 4) {
                        if (x + px < width && y + py < height) {
                            val bitIndex = (px shl 2) or py
                            val valBit = ((temp shr bitIndex) and 1u).toInt()
                            val negBit = ((temp shr (bitIndex + 16)) and 1u) == 1u

                            val useTable1 = (flipbit && py < 2) || (!flipbit && px < 2)
                            val mod = if (useTable1) {
                                ETCEncode.ETC1Modifiers[table1][valBit]
                            } else {
                                ETCEncode.ETC1Modifiers[table2][valBit]
                            }
                            val add = if (negBit) -mod else mod

                            val r = ETCEncode.colorClamp(if (useTable1) r1 + add else r2 + add)
                            val g = ETCEncode.colorClamp(if (useTable1) g1 + add else g2 + add)
                            val b = ETCEncode.colorClamp(if (useTable1) b1 + add else b2 + add)
                            val argb = 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b

                            bitmap.setPixelArgb(x + px, y + py, argb)
                        }
                    }
                }
                x += 4
            }
            y += 4
        }
        return bitmap
    }

    private fun Int.toSigned3(): Int {
        return if (this >= 4) this - 8 else this
    }
}