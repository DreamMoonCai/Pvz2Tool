package io.github.dreammooncai.pvz2tool.pop.plugin.crypt

object Pvz2NumberCrypt {

    fun decrypt(num: Long): Long {
        val iArr = intArrayOf(13, 12, 15, 14, 9, 8, 11, 10, 5, 4, 7, 6, 1, 0, 3, 2)
        var j = num
        val j2: Long

        if (j >= 0) {
            j2 = (j / 8192) % 16
        } else {
            j += 4294967296L
            j2 = (j / 8192) % 16
        }

        val j3 = 8192L
        val j4 = 16L
        var j5 = ((j / j3) / j4) * j4 + iArr[j2.toInt()] + (524288L * (j % j3))

        if (j5 > Int.MAX_VALUE) {
            j5 -= 4294967296L
        }
        return j5
    }

    fun encrypt(num: Long): Long {
        var j = num
        if (j < 0) {
            j += 4294967296L
        }

        val sb = StringBuilder()
        val bin = longToBinary(j)
        val length = bin.length

        for (j2 in 32 downTo length + 1) {
            sb.append("0")
        }
        sb.append(bin)

        val binaryStr = sb.toString()
        val part1 = binaryStr.substring(13, 28)
        val part2 = binaryPart3Inverse(binaryStr.substring(28, 32))
        val part3 = binaryStr.substring(0, 13)

        val newBinary = part1 + part2 + part3
        val result = binaryToLong(newBinary)

        return if (result >= 2147483648L) {
            result - 4294967296L
        } else {
            result
        }
    }

    private fun longToBinary(j: Long): String {
        return j.toString(2)
    }

    private fun binaryToLong(str: String): Long {
        return str.toLong(2)
    }

    private fun binaryPart3Inverse(str: String): String {
        val sb = StringBuilder()
        val chars = str.toCharArray()

        for ((i, c) in chars.withIndex()) {
            if (i == 2) {
                sb.append(c)
            } else {
                sb.append(if (c == '0') '1' else '0')
            }
        }
        return sb.toString()
    }
}