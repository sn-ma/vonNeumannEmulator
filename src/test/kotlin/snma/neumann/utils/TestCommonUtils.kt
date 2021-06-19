package snma.neumann.utils

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import snma.neumann.utils.CommonUtils.countValuableBits
import snma.neumann.utils.CommonUtils.hexStringToInt
import snma.neumann.utils.CommonUtils.intToHexString
import java.util.stream.Stream
import kotlin.test.assertEquals


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
            "0F FF FF" to null,
        ).forEach { (stringValue, expected) ->
            val actual = hexStringToInt(stringValue, 2)
            assertEquals(expected, actual,
                "Wrong conversion result for '$stringValue'")
        }
    }

    companion object {
        data class SampleIntToPrefHex(val int: Int, val expected: String)

        @JvmStatic
        @Suppress("unused")
        fun genIntToPrefixedStringsData(): Stream<SampleIntToPrefHex> = Stream.of(
            SampleIntToPrefHex(0, "0x0"),
            SampleIntToPrefHex(0x11, "0x11"),
            SampleIntToPrefHex(-0x101, "-0x101"),
        )

        data class SamplePrefStringToInt(val string: String, val expected: Int)

        @JvmStatic
        @Suppress("unused")
        fun getPrefStringToIntData(): Stream<SamplePrefStringToInt> = Stream.of(
            SamplePrefStringToInt("0x123", 0x123),
            SamplePrefStringToInt("-0x123", -0x123),
            SamplePrefStringToInt("-0x0", 0),
            SamplePrefStringToInt("0b101", 0b101),
            SamplePrefStringToInt("-0b101", -0b101),
            SamplePrefStringToInt("1234", 1234),
            SamplePrefStringToInt("-1234", -1234),
            SamplePrefStringToInt("0", 0),
        )

        data class SampleIntToBinaryString(val int: Int, val bitsCount: Int, val expected: String)

        @JvmStatic
        @Suppress("unused")
        fun getIntToBinaryString(): Stream<SampleIntToBinaryString> = Stream.of(
            SampleIntToBinaryString(0, 3, "000"),
            SampleIntToBinaryString(10, 8, "0000 1010"),
        )
    }

    @ParameterizedTest
    @MethodSource("genIntToPrefixedStringsData")
    fun intToShortPrefixedHexString(sample: SampleIntToPrefHex) {
        assertEquals(sample.expected, CommonUtils.intToShortPrefixedHexString(sample.int))
    }

    @ParameterizedTest
    @MethodSource("getPrefStringToIntData")
    fun prefixedStringToInt(sample: SamplePrefStringToInt) {
        assertEquals(sample.expected, CommonUtils.prefixedStringToInt(sample.string))
    }

    @ParameterizedTest
    @MethodSource("getIntToBinaryString")
    fun intToBinaryString(sample: SampleIntToBinaryString) {
        assertEquals(sample.expected, CommonUtils.intToBinaryString(sample.int, sample.bitsCount))
    }
}