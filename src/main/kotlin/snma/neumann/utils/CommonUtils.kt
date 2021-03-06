package snma.neumann.utils

import org.slf4j.LoggerFactory

object CommonUtils {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun Int.countValuableBits(): Int = if (this == 0) 0 else toString(2).length

    fun intToHexString(intVal: Int?, bytesCount: Int): String? {
        if (intVal == null) return null
        var answ = intVal.toString(16).uppercase()
        val zeroesToAdd = 2 * bytesCount - answ.length
        if (zeroesToAdd > 0) {
            answ = "0".repeat(zeroesToAdd) + answ
        } else if (zeroesToAdd < 0) {
            logger.error("$intVal is more than $bytesCount byte(s)!")
            return null
        }
        val charIterator = answ.iterator()
        return buildString {
            while (true) {
                append(charIterator.nextChar())
                append(charIterator.nextChar())
                if (!charIterator.hasNext()) {
                    break
                } else {
                    append(' ')
                }
            }
        }
    }

    private val leadingZeroesRE = """^0+([^0]*[\dA-Z])$""".toRegex()

    fun hexStringToInt(str: String?, bytesCount: Int): Int? {
        if (str.isNullOrEmpty()) return null
        val replacedStr = str
            .replace(" ", "")
            .uppercase()
            .replace(leadingZeroesRE, """$1""")
        if (replacedStr.length / 2.0 > bytesCount) return null
        if (replacedStr.startsWith("-")) return null
        return replacedStr.toIntOrNull(16)
    }

    fun intToShortPrefixedHexString(int: Int): String = if (int < 0) "-" + intToShortPrefixedHexString(-int) else
        "0x${int.toString(16).uppercase()}"

    fun prefixedStringToInt(str: String): Int? = when {
        str.startsWith("-") -> prefixedStringToInt(str.substring(1))?.let { -it }
        str.startsWith("0x") -> str.substring(2).toIntOrNull(16)
        str.startsWith("0b") -> str.substring(2).toIntOrNull(2)
        else -> str.toIntOrNull()
    }

    fun intToBinaryString(value: Int, bitsCount: Int): String {
        var answer = value.toString(2)
        val zeroesRequired = bitsCount - answer.length
        if (zeroesRequired > 0) {
            answer = "0".repeat(zeroesRequired) + answer
        } else if (zeroesRequired < 0) {
            logger.error("Too big number to transform in binary form: $value, it should be $bitsCount bits")
        }
        val reversedAnswer = buildString {
            var counter = 0
            for (i in (answer.length - 1) downTo 0) {
                append(answer[i])
                counter++
                if (counter == 4 && i != 0) {
                    append(' ')
                    counter = 0
                }
            }
        }
        return reversedAnswer.reversed()
    }

    data class Holder<T>(var value: T)
}