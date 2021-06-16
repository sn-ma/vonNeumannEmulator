package snma.neumann.utils

object CommonUtils {
    fun Int.countValuableBits(): Int = if (this == 0) 0 else toString(2).length

    fun intToHexString(intVal: Int?, bytesCount: Int): String? {
        if (intVal == null) return null
        var answ = intVal.toString(16).uppercase()
        val zeroesToAdd = 2 * bytesCount - answ.length
        if (zeroesToAdd > 0) {
            answ = "0".repeat(zeroesToAdd) + answ
        } else if (zeroesToAdd < 0) {
            error("$intVal is more than $bytesCount byte(s)!")
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

    fun hexStringToInt(str: String?): Int? {
        if (str.isNullOrEmpty()) return null
        val replacedStr = str
            .replace(" ", "")
            .uppercase()
            .replace(leadingZeroesRE, """$1""")
        return if (replacedStr.isEmpty()) 0
        else replacedStr.toIntOrNull(16)
    }

    fun intToShortPrefixedHexString(int: Int): String = if (int < 0) "-" + intToShortPrefixedHexString(-int) else
        "0x${int.toString(16).uppercase()}"

    fun prefixedStringToInt(str: String): Int? = when {
        str.startsWith("-") -> prefixedStringToInt(str.substring(1))?.let { -it }
        str.startsWith("0x") -> str.substring(2).toIntOrNull(16)
        str.startsWith("0b") -> str.substring(2).toIntOrNull(2)
        else -> str.toIntOrNull()
    }
}