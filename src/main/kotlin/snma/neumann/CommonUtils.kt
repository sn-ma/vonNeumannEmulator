package snma.neumann

import java.util.*

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

    fun<T> LinkedList<T>.pushFront(vararg items: T) {
        for (i in (items.size - 1) downTo 0) {
            addFirst(items[i])
        }
    }
}