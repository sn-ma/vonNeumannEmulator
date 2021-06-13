package snma.neumann

import org.junit.jupiter.api.Test
import snma.neumann.CommonUtils.countValuableBits
import snma.neumann.CommonUtils.hexStringToInt
import snma.neumann.CommonUtils.intToHexString
import snma.neumann.CommonUtils.pushFront
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFails


class TestCommonUtils {
    @Test
    fun `Check countBits`() {
        listOf(
            3 to 0b101,
            0 to 0b0,
            10 to 0b1010101010
        ).forEach { (expectedBitsCount, number) ->
            assertEquals(expectedBitsCount, number.countValuableBits(),
                "$number has actually $expectedBitsCount bits, but got ${number.countValuableBits()}")
        }
    }

    private data class IntToHexStringParams(val intVal: Int?, val bytesCount: Int, val expected: String?)

    @Test
    fun `intToHexString works`() {
        listOf(
            IntToHexStringParams(1, 1, "01"),
            IntToHexStringParams(0, 1, "00"),
            IntToHexStringParams(null, 2, null),
            IntToHexStringParams(0, 3, "00 00 00"),
            IntToHexStringParams(0x012345, 3, "01 23 45"),
            IntToHexStringParams(0x0abbee, 3, "0A BB EE"),
        ).forEach { (intVal, bytesCount, expected) ->
            val actual = intToHexString(intVal, bytesCount)
            assertEquals(expected, actual,
                "While converting $intVal to hex string with bytesCount=$bytesCount " +
                        " got $actual, while expecting $expected")
        }
    }

    @Test
    fun `intToHexString fails`() {
        assertFails {
            intToHexString(0x0123, 1)
        }
    }

    @Test
    fun `hexStringToInt works`() {
        listOf(
            null to null,
            "12 34" to 0x1234,
            "AB CD" to 0xABCD,
            "ab cd" to 0xABCD,
            "a b c d  " to 0xABCD,
            "abcd" to 0xABCD,
            "Q" to null,
            "03" to 3,
            "0F" to 0x0F,
        ).forEach { (stringValue, expected) ->
            val actual = hexStringToInt(stringValue)
            assertEquals(expected, actual,
                "Wrong conversion result for '$stringValue'")
        }
    }

    @Test
    fun `Deque pushFront`() {
        listOf(
            Triple(listOf(1, 2, 3), arrayOf(), listOf(1, 2, 3)),
            Triple(listOf(1, 2, 3), arrayOf(0), listOf(0, 1, 2, 3)),
            Triple(listOf(1, 2, 3), arrayOf(-2, -1, 0), listOf(-2, -1, 0, 1, 2, 3)),
        ).forEach { (originalListData, addArray, expectedList) ->
            val deque = LinkedList(originalListData)
            deque.pushFront(*addArray)
            assertEquals(expectedList, deque)
        }
    }
}