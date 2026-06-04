package io.github.dreammooncai.pvz2tool.pop.plugin.encode

import io.github.dreammooncai.pvz2tool.pop.image.YFColor
import kotlin.math.abs

object ETCEncode {
    val ETC1Modifiers = arrayOf(
        intArrayOf(2, 8),
        intArrayOf(5, 17),
        intArrayOf(9, 29),
        intArrayOf(13, 42),
        intArrayOf(18, 60),
        intArrayOf(24, 80),
        intArrayOf(33, 106),
        intArrayOf(47, 183)
    )

    fun genETC1(colors: Array<YFColor>): ULong {
        val horizontal = genHorizontal(colors)
        val vertical = genVertical(colors)
        val scoreH = getScore(colors, decodeETC1(horizontal))
        val scoreV = getScore(colors, decodeETC1(vertical))
        return if (scoreH < scoreV) horizontal else vertical
    }

    fun decodeETC1(temp: ULong): Array<YFColor> {
        val res = Array(16) { YFColor() }
        val diffBit = (temp shr 33 and 1uL) == 1uL
        val flipBit = (temp shr 32 and 1uL) == 1uL

        var r1 = 0
        var g1 = 0
        var b1 = 0
        var r2 = 0
        var g2 = 0
        var b2 = 0

        if (diffBit) {
            val r = (temp shr 59 and 0x1FuL).toInt()
            val g = (temp shr 51 and 0x1FuL).toInt()
            val b = (temp shr 43 and 0x1FuL).toInt()
            r1 = (r shl 3) or (r and 0x1C shr 2)
            g1 = (g shl 3) or (g and 0x1C shr 2)
            b1 = (b shl 3) or (b and 0x1C shr 2)

            val dr = (temp shr 56 and 0x7uL).toInt().let {
                if (it >= 4) it - 8 else it
            }
            val dg = (temp shr 48 and 0x7uL).toInt().let {
                if (it >= 4) it - 8 else it
            }
            val db = (temp shr 40 and 0x7uL).toInt().let {
                if (it >= 4) it - 8 else it
            }

            val rr = r + dr
            val gg = g + dg
            val bb = b + db

            r2 = (rr shl 3) or (rr and 0x1C shr 2)
            g2 = (gg shl 3) or (gg and 0x1C shr 2)
            b2 = (bb shl 3) or (bb and 0x1C shr 2)
        } else {
            r1 = (temp shr 60 and 0xFuL).toInt() * 17
            g1 = (temp shr 52 and 0xFuL).toInt() * 17
            b1 = (temp shr 44 and 0xFuL).toInt() * 17
            r2 = (temp shr 56 and 0xFuL).toInt() * 17
            g2 = (temp shr 48 and 0xFuL).toInt() * 17
            b2 = (temp shr 40 and 0xFuL).toInt() * 17
        }

        val t1 = (temp shr 37 and 0x7uL).toInt()
        val t2 = (temp shr 34 and 0x7uL).toInt()

        for (py in 0..3) {
            for (px in 0..3) {
                val bit = px * 4 + py
                val v = (temp shr bit and 1uL).toInt()
                val neg = (temp shr (bit + 16) and 1uL) == 1uL
                val use1 = flipBit && py < 2 || !flipBit && px < 2

                val add = if (use1) ETC1Modifiers[t1][v] else ETC1Modifiers[t2][v]
                val finalAdd = add * if (neg) -1 else 1

                val r = colorClamp(if (use1) r1 + finalAdd else r2 + finalAdd)
                val g = colorClamp(if (use1) g1 + finalAdd else g2 + finalAdd)
                val b = colorClamp(if (use1) b1 + finalAdd else b2 + finalAdd)

                res[py * 4 + px] = YFColor(r, g, b, 255)
            }
        }
        return res
    }

    private fun getScore(ori: Array<YFColor>, gen: Array<YFColor>): Int {
        var s = 0
        for (i in ori.indices) {
            s += abs(ori[i].red - gen[i].red)
            s += abs(ori[i].green - gen[i].green)
            s += abs(ori[i].blue - gen[i].blue)
        }
        return s
    }

    private fun genHorizontal(colors: Array<YFColor>): ULong {
        var d = 0uL
        d = d or (0uL shl 32) // flip=0

        val left = getLeft(colors)
        val (base1, mod1) = genMod(left)
        d = setT1(d, mod1)
        d = genPix(d, left, base1, mod1, 0, 2, 0, 4)

        val right = getRight(colors)
        val (base2, mod2) = genMod(right)
        d = setT2(d, mod2)
        d = genPix(d, right, base2, mod2, 2, 4, 0, 4)

        d = setBase(d, base1, base2)
        return d
    }

    private fun genVertical(colors: Array<YFColor>): ULong {
        var d = 0uL
        d = d or (1uL shl 32) // flip=1

        val top = getTop(colors)
        val (base1, mod1) = genMod(top)
        d = setT1(d, mod1)
        d = genPix(d, top, base1, mod1, 0, 4, 0, 2)

        val bottom = getBottom(colors)
        val (base2, mod2) = genMod(bottom)
        d = setT2(d, mod2)
        d = genPix(d, bottom, base2, mod2, 0, 4, 2, 4)

        d = setBase(d, base1, base2)
        return d
    }

    private fun setBase(d: ULong, c1: YFColor, c2: YFColor): ULong {
        var data = d
        val r1 = c1.red
        val g1 = c1.green
        val b1 = c1.blue
        val r2 = c2.red
        val g2 = c2.green
        val b2 = c2.blue

        val dr = (r2 - r1) / 8
        val dg = (g2 - g1) / 8
        val db = (b2 - b1) / 8

        if (dr in -3..2 && dg in -3..2 && db in -3..2) {
            data = data or (1uL shl 33)
            val r = r1 / 8
            val g = g1 / 8
            val b = b1 / 8
            data = data or (r.toULong() shl 59)
            data = data or (g.toULong() shl 51)
            data = data or (b.toULong() shl 43)
            data = data or ((dr and 7).toULong() shl 56)
            data = data or ((dg and 7).toULong() shl 48)
            data = data or ((db and 7).toULong() shl 40)
        } else {
            data = data and (1uL shl 33).inv()
            data = data or ((r1 / 17).toULong() shl 60)
            data = data or ((g1 / 17).toULong() shl 52)
            data = data or ((b1 / 17).toULong() shl 44)
            data = data or ((r2 / 17).toULong() shl 56)
            data = data or ((g2 / 17).toULong() shl 48)
            data = data or ((b2 / 17).toULong() shl 40)
        }
        return data
    }

    private fun genPix(
        d: ULong, pixels: Array<YFColor>, base: YFColor, mod: Int,
        x0: Int, x1: Int, y0: Int, y1: Int
    ): ULong {
        var data = d
        val baseLum = (base.red + base.green + base.blue) / 3
        var idx = 0
        for (yy in y0 until y1) {
            for (xx in x0 until x1) {
                val p = pixels[idx++]
                val lum = (p.red + p.green + p.blue) / 3
                val diff = lum - baseLum
                val bit = xx * 4 + yy

                if (diff < 0) data = data or (1uL shl (bit + 16))

                val d0 = abs(diff) - ETC1Modifiers[mod][0]
                val d1 = abs(diff) - ETC1Modifiers[mod][1]
                if (abs(d1) < abs(d0)) {
                    data = data or (1uL shl bit)
                }
            }
        }
        return data
    }

    private fun genMod(pixels: Array<YFColor>): Pair<YFColor, Int> {
        var min = YFColor(255, 255, 255)
        var max = YFColor(0, 0, 0)
        var minY = Int.MAX_VALUE
        var maxY = Int.MIN_VALUE

        for (c in pixels) {
            val y = (c.red + c.green + c.blue) / 3
            if (y < minY) {
                minY = y
                min = c
            }
            if (y > maxY) {
                maxY = y
                max = c
            }
        }

        val d = (max.red - min.red + max.green - min.green + max.blue - min.blue) / 3
        var best = 0
        var bestScore = Int.MAX_VALUE

        for (i in ETC1Modifiers.indices) {
            val s = ETC1Modifiers[i][0] * 2
            val score = abs(d - s)
            if (score < bestScore) {
                bestScore = score
                best = i
            }
        }

        val r = (min.red + max.red) / 2
        val g = (min.green + max.green) / 2
        val b = (min.blue + max.blue) / 2
        return YFColor(r, g, b) to best
    }

    private fun setT1(d: ULong, t: Int) = (d and (7uL shl 37).inv()) or ((t.toULong() and 7uL) shl 37)
    private fun setT2(d: ULong, t: Int) = (d and (7uL shl 34).inv()) or ((t.toULong() and 7uL) shl 34)

    private fun getLeft(c: Array<YFColor>) = Array(8) { i -> c[i / 2 * 4 + i % 2] }
    private fun getRight(c: Array<YFColor>) = Array(8) { i -> c[i / 2 * 4 + i % 2 + 2] }
    private fun getTop(c: Array<YFColor>) = Array(8) { i -> c[i] }
    private fun getBottom(c: Array<YFColor>) = Array(8) { i -> c[i + 8] }

    fun colorClamp(v: Int) = v.coerceIn(0, 255)
}