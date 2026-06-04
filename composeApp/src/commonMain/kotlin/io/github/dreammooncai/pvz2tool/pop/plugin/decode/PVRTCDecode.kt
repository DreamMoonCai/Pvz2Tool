package io.github.dreammooncai.pvz2tool.pop.plugin.decode

import io.github.dreammooncai.pvz2tool.pop.image.YFBitmap
import io.github.dreammooncai.pvz2tool.pop.image.YFColor
import io.github.dreammooncai.pvz2tool.pop.image.setPixelArgb

object PVRTCDecode {
    private class Pixel128S(
        var red: Int = 0,
        var green: Int = 0,
        var blue: Int = 0,
        var alpha: Int = 0
    )

    private class PVRTCWord(
        var ModulationData: Int = 0,
        var ColorData: Int = 0
    )

    private class PVRTCWordIndices {
        val P = intArrayOf(0, 0)
        val Q = intArrayOf(0, 0)
        val R = intArrayOf(0, 0)
        val S = intArrayOf(0, 0)
    }

    private val RepVals0 = intArrayOf(0, 3, 5, 8)

    // ────────────────────────────────────────────────────
    // GetColorA / GetColorB (完全对齐C#)
    // ────────────────────────────────────────────────────
    private fun GetColorA(ColorData: Int): YFColor {
        var red: Int
        var green: Int
        var blue: Int
        var alpha: Int
        if ((ColorData and 0x8000) != 0) {
            red = ((ColorData and 0x7C00) shr 10)
            green = ((ColorData and 0x3E0) shr 5)
            blue = ((ColorData and 0x1E) or ((ColorData and 0x1E) shr 4))
            alpha = 0xF
        } else {
            red = (((ColorData and 0xF00) shr 7) or ((ColorData and 0xF00) shr 11))
            green = (((ColorData and 0xF0) shr 3) or ((ColorData and 0xF0) shr 7))
            blue = (((ColorData and 0xE) shl 1) or ((ColorData and 0xE) shr 2))
            alpha = ((ColorData and 0x7000) shr 11)
        }
        return YFColor(red,green,blue, alpha)
    }

    private fun GetColorB(ColorData: Int): YFColor {
        var red: Int
        var green: Int
        var blue: Int
        var alpha: Int
        val ColorData = ColorData.toLong()
        if ((ColorData and 0x80000000) != 0L) {
            red = ((ColorData and 0x7C000000) shr 26).toInt()
            green = ((ColorData and 0x3E00000) shr 21).toInt()
            blue = ((ColorData and 0x1F0000) shr 16).toInt()
            alpha = 0xF
        } else {
            red = (((ColorData and 0xF000000) shr 23) or ((ColorData and 0xF000000) shr 27)).toInt()
            green = (((ColorData and 0xF00000) shr 19) or ((ColorData and 0xF00000) shr 23)).toInt()
            blue = (((ColorData and 0xF0000) shr 15) or ((ColorData and 0xF0000) shr 19)).toInt()
            alpha = ((ColorData and 0x70000000) shr 27).toInt()
        }
        return YFColor(red,green,blue, alpha)
    }

    // ────────────────────────────────────────────────────
    // InterpolateColors
    // ────────────────────────────────────────────────────
    private fun InterpolateColors(
        P: YFColor, Q: YFColor, R: YFColor, S: YFColor,
        pPixel: Array<Pixel128S>, bpp: Int
    ) {
        val WordWidth = if (bpp == 2) 8 else 4
        val WordHeight = 4

        val hP = Pixel128S(P.red, P.green, P.blue, P.alpha)
        val hQ = Pixel128S(Q.red, Q.green, Q.blue, Q.alpha)
        val hR = Pixel128S(R.red, R.green, R.blue, R.alpha)
        val hS = Pixel128S(S.red, S.green, S.blue, S.alpha)

        val QminusP = Pixel128S(hQ.red - hP.red, hQ.green - hP.green, hQ.blue - hP.blue, hQ.alpha - hP.alpha)
        val SminusR = Pixel128S(hS.red - hR.red, hS.green - hR.green, hS.blue - hR.blue, hS.alpha - hR.alpha)

        hP.red *= WordWidth
        hP.green *= WordWidth
        hP.blue *= WordWidth
        hP.alpha *= WordWidth
        hR.red *= WordWidth
        hR.green *= WordWidth
        hR.blue *= WordWidth
        hR.alpha *= WordWidth

        if (bpp == 2) {
            for (x in 0 until WordWidth) {
                val result = Pixel128S(hP.red * 4, hP.green * 4, hP.blue * 4, hP.alpha * 4)
                val dY = Pixel128S(hR.red - hP.red, hR.green - hP.green, hR.blue - hP.blue, hR.alpha - hP.alpha)
                for (y in 0 until WordHeight) {
                    val idx = y * WordWidth + x
                    pPixel[idx].red = (result.red shr 7) + (result.red shr 2)
                    pPixel[idx].green = (result.green shr 7) + (result.green shr 2)
                    pPixel[idx].blue = (result.blue shr 7) + (result.blue shr 2)
                    pPixel[idx].alpha = (result.alpha shr 5) + (result.alpha shr 1)
                    result.red += dY.red
                    result.green += dY.green
                    result.blue += dY.blue
                    result.alpha += dY.alpha
                }
                hP.red += QminusP.red
                hP.green += QminusP.green
                hP.blue += QminusP.blue
                hP.alpha += QminusP.alpha
                hR.red += SminusR.red
                hR.green += SminusR.green
                hR.blue += SminusR.blue
                hR.alpha += SminusR.alpha
            }
        } else {
            for (y in 0 until WordHeight) {
                val result = Pixel128S(hP.red * 4, hP.green * 4, hP.blue * 4, hP.alpha * 4)
                val dY = Pixel128S(hR.red - hP.red, hR.green - hP.green, hR.blue - hP.blue, hR.alpha - hP.alpha)
                for (x in 0 until WordWidth) {
                    val idx = y * WordWidth + x
                    pPixel[idx].red = (result.red shr 6) + (result.red shr 1)
                    pPixel[idx].green = (result.green shr 6) + (result.green shr 1)
                    pPixel[idx].blue = (result.blue shr 6) + (result.blue shr 1)
                    pPixel[idx].alpha = (result.alpha shr 4) + result.alpha
                    result.red += dY.red
                    result.green += dY.green
                    result.blue += dY.blue
                    result.alpha += dY.alpha
                }
                hP.red += QminusP.red
                hP.green += QminusP.green
                hP.blue += QminusP.blue
                hP.alpha += QminusP.alpha
                hR.red += SminusR.red
                hR.green += SminusR.green
                hR.blue += SminusR.blue
                hR.alpha += SminusR.alpha
            }
        }
    }

    // ────────────────────────────────────────────────────
    // UnpackModulations
    // ────────────────────────────────────────────────────
    private fun UnpackModulations(
        word: PVRTCWord, offsetX: Int, offsetY: Int,
        ModulationValues: Array<IntArray>, ModulationModes: Array<IntArray>, bpp: Int
    ) {
        val WordModMode = (word.ColorData and 0x1).toUInt()
        var ModulationBits = word.ModulationData.toUInt()
        val WordWidth = if (bpp == 2) 8 else 4
        val WordHeight = 4

        if (bpp == 2) {
            var wmm = WordModMode.toInt()
            var mb = ModulationBits
            if (wmm != 0) {
                if ((mb and 0x1u) != 0u) {
                    if ((mb and (0x1u shl 20)) != 0u) wmm = 3 else wmm = 2
                    if ((mb and (0x1u shl 21)) != 0u) mb = mb or (0x1u shl 20)
                    else mb = mb and (0x1u shl 20).inv()
                }
                if ((mb and 0x2u) != 0u) mb = mb or 0x1u else mb = mb and 0x1u.inv()

                for (y in 0 until WordHeight) {
                    for (x in 0 until WordWidth) {
                        ModulationModes[x + offsetX][y + offsetY] = wmm
                        if (((x xor y) and 1) == 0) {
                            ModulationValues[x + offsetX][y + offsetY] = (mb and 3u).toInt()
                            mb = mb shr 2
                        }
                    }
                }
            } else {
                for (y in 0 until WordHeight) {
                    for (x in 0 until WordWidth) {
                        ModulationModes[x + offsetX][y + offsetY] = wmm
                        ModulationValues[x + offsetX][y + offsetY] = if ((mb and 1u) != 0u) 3 else 0
                        mb = mb shr 1
                    }
                }
            }
        } else {
            if (WordModMode != 0u) {
                for (y in 0 until WordHeight) {
                    for (x in 0 until WordWidth) {
                        var v = (ModulationBits and 3u).toInt()
                        v = when (v) {
                            1 -> 4
                            2 -> 14
                            3 -> 8
                            else -> v
                        }
                        ModulationValues[y + offsetY][x + offsetX] = v
                        ModulationBits = ModulationBits shr 2
                    }
                }
            } else {
                for (y in 0 until WordHeight) {
                    for (x in 0 until WordWidth) {
                        var v = (ModulationBits and 3u).toInt() * 3
                        if (v > 3) v -= 1
                        ModulationValues[y + offsetY][x + offsetX] = v
                        ModulationBits = ModulationBits shr 2
                    }
                }
            }
        }
    }

    // ────────────────────────────────────────────────────
    // GetModulationValues
    // ────────────────────────────────────────────────────
    private fun GetModulationValue(
        ModulationValues: Array<IntArray>, ModulationModes: Array<IntArray>,
        xPos: Int, yPos: Int, bpp: Int
    ): Int {
        if (bpp == 2) {
            return if (ModulationModes[xPos][yPos] == 0) {
                RepVals0[ModulationValues[xPos][yPos]]
            } else {
                if (((xPos xor yPos) and 1) == 0) {
                    RepVals0[ModulationValues[xPos][yPos]]
                } else if (ModulationModes[xPos][yPos] == 1) {
                    (RepVals0[ModulationValues[xPos][yPos - 1]] +
                     RepVals0[ModulationValues[xPos][yPos + 1]] +
                     RepVals0[ModulationValues[xPos - 1][yPos]] +
                     RepVals0[ModulationValues[xPos + 1][yPos]] + 2) shr 2
                } else if (ModulationModes[xPos][yPos] == 2) {
                    (RepVals0[ModulationValues[xPos - 1][yPos]] +
                     RepVals0[ModulationValues[xPos + 1][yPos]] + 1) shr 1
                } else {
                    (RepVals0[ModulationValues[xPos][yPos - 1]] +
                     RepVals0[ModulationValues[xPos][yPos + 1]] + 1) shr 1
                }
            }
        }
        return ModulationValues[xPos][yPos]
    }

    // ────────────────────────────────────────────────────
    // PvrtcGetDecompressedPixels
    // ────────────────────────────────────────────────────
    private fun PvrtcGetDecompressedPixels(
        P: PVRTCWord, Q: PVRTCWord, R: PVRTCWord, S: PVRTCWord,
        pColorData: Array<YFColor>, bpp: Int,
        ModulationValues: Array<IntArray>, ModulationModes: Array<IntArray>,
        upscaledColorA: Array<Pixel128S>, upscaledColorB: Array<Pixel128S>
    ) {
        val WordWidth = if (bpp == 2) 8 else 4
        val WordHeight = 4

        UnpackModulations(P, 0, 0, ModulationValues, ModulationModes, bpp)
        UnpackModulations(Q, WordWidth, 0, ModulationValues, ModulationModes, bpp)
        UnpackModulations(R, 0, WordHeight, ModulationValues, ModulationModes, bpp)
        UnpackModulations(S, WordWidth, WordHeight, ModulationValues, ModulationModes, bpp)

        InterpolateColors(GetColorA(P.ColorData), GetColorA(Q.ColorData),
                           GetColorA(R.ColorData), GetColorA(S.ColorData), upscaledColorA, bpp)
        InterpolateColors(GetColorB(P.ColorData), GetColorB(Q.ColorData),
                           GetColorB(R.ColorData), GetColorB(S.ColorData), upscaledColorB, bpp)

        for (y in 0 until WordHeight) {
            for (x in 0 until WordWidth) {
                val mx = x + WordWidth / 2
                val my = y + WordHeight / 2
                var mod = GetModulationValue(ModulationValues, ModulationModes, mx, my, bpp)
                var punch = false
                if (mod > 10) {
                    punch = true
                    mod -= 10
                }
                val idx = y * WordWidth + x
                val ca = upscaledColorA[idx]
                val cb = upscaledColorB[idx]

                val r = (ca.red * (8 - mod) + cb.red * mod) / 8
                val g = (ca.green * (8 - mod) + cb.green * mod) / 8
                val b = (ca.blue * (8 - mod) + cb.blue * mod) / 8
                val a = if (punch) 0 else (ca.alpha * (8 - mod) + cb.alpha * mod) / 8

                if (bpp == 2) {
                    pColorData[idx] = YFColor(r, g, b, a)
                } else {
                    val i = y + x * WordHeight
                    pColorData[i] = YFColor(r, g, b, a)
                }
            }
        }
    }

    // ────────────────────────────────────────────────────
    // WrapWordIndex / TwiddleUV
    // ────────────────────────────────────────────────────
    private fun WrapWordIndex(numWords: Int, word: Int): Int {
        return (word + numWords) % numWords
    }

    private fun TwiddleUV(XSize: Int, YSize: Int, XPos: Int, YPos: Int): Int {
        var minDim = XSize
        var maxVal = YPos
        if (YSize < XSize) {
            minDim = YSize
            maxVal = XPos
        }
        var twiddle = 0
        var src = 1
        var dst = 1
        var shift = 0
        while (src < minDim) {
            if ((YPos and src) != 0) twiddle = twiddle or dst
            if ((XPos and src) != 0) twiddle = twiddle or (dst shl 1)
            src = src shl 1
            dst = dst shl 2
            shift++
        }
        maxVal = maxVal shr shift
        twiddle = twiddle or (maxVal shl (shift shl 1))
        return twiddle
    }

    // ────────────────────────────────────────────────────
    // MapDecompressedData
    // ────────────────────────────────────────────────────
    private suspend fun MapDecompressedData(
        outBmp: YFBitmap, wordW: Int,
        pWord: Array<YFColor>, indices: PVRTCWordIndices, bpp: Int
    ) {
        val WordWidth = if (bpp == 2) 8 else 4
        val WordHeight = 4
        val dw = WordWidth / 2
        val dh = WordHeight / 2

        for (y in 0 until dh) {
            for (x in 0 until dw) {
                val c = pWord[y * WordWidth + x]
                outBmp.setPixelArgb(
                    indices.P[0] * WordWidth + x + dw,
                    indices.P[1] * WordHeight + y + dh,
                    c.toArgb()
                )
                val c2 = pWord[y * WordWidth + x + dw]
                outBmp.setPixelArgb(
                    indices.Q[0] * WordWidth + x,
                    indices.Q[1] * WordHeight + y + dh,
                    c2.toArgb()
                )
                val c3 = pWord[(y + dh) * WordWidth + x]
                outBmp.setPixelArgb(
                    indices.R[0] * WordWidth + x + dw,
                    indices.R[1] * WordHeight + y,
                    c3.toArgb()
                )
                val c4 = pWord[(y + dh) * WordWidth + x + dw]
                outBmp.setPixelArgb(
                    indices.S[0] * WordWidth + x,
                    indices.S[1] * WordHeight + y,
                    c4.toArgb()
                )
            }
        }
    }

    // ────────────────────────────────────────────────────
    // 主函数：pvrtcDecompress
    // ────────────────────────────────────────────────────
    suspend fun pvrtcDecompress(pCompressedData: ByteArray, outBitmap: YFBitmap, width: Int, height: Int, bpp: Int) {
        val WordWidth = if (bpp == 2) 8 else 4
        val WordHeight = 4
        val numXWords = width / WordWidth
        val numYWords = height / WordHeight

        val modValues = Array(16) { IntArray(8) }
        val modModes = Array(16) { IntArray(8) }
        val colorBuf = Array(32) { Pixel128S() }
        val colorBuf2 = Array(32) { Pixel128S() }
        val pixelBuf = Array(32) { YFColor() }

        for (wordY in -1 until numYWords - 1) {
            for (wordX in -1 until numXWords - 1) {
                val indices = PVRTCWordIndices()
                indices.P[0] = WrapWordIndex(numXWords, wordX)
                indices.P[1] = WrapWordIndex(numYWords, wordY)
                indices.Q[0] = WrapWordIndex(numXWords, wordX + 1)
                indices.Q[1] = WrapWordIndex(numYWords, wordY)
                indices.R[0] = WrapWordIndex(numXWords, wordX)
                indices.R[1] = WrapWordIndex(numYWords, wordY + 1)
                indices.S[0] = WrapWordIndex(numXWords, wordX + 1)
                indices.S[1] = WrapWordIndex(numYWords, wordY + 1)

                fun getWord(ix: Int, iy: Int): PVRTCWord {
                    val tw = TwiddleUV(numXWords, numYWords, ix, iy) shl 1
                    val m = readIntLE(pCompressedData, tw * 4)
                    val c = readIntLE(pCompressedData, (tw + 1) * 4)
                    return PVRTCWord(m, c)
                }

                val P = getWord(indices.P[0], indices.P[1])
                val Q = getWord(indices.Q[0], indices.Q[1])
                val R = getWord(indices.R[0], indices.R[1])
                val S = getWord(indices.S[0], indices.S[1])

                PvrtcGetDecompressedPixels(P, Q, R, S, pixelBuf, bpp, modValues, modModes, colorBuf, colorBuf2)
                MapDecompressedData(outBitmap, width, pixelBuf, indices, bpp)
            }
        }
    }

    // 小端读取 Int
    private fun readIntLE(data: ByteArray, offset: Int): Int {
        return (data[offset + 3].toInt() shl 24) or
               (data[offset + 2].toInt() and 0xFF shl 16) or
               (data[offset + 1].toInt() and 0xFF shl 8) or
               (data[offset].toInt() and 0xFF)
    }
}