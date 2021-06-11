package snma.neumann

import snma.neumann.BinaryUtils.countValuableBits
import kotlin.test.Test
import kotlin.test.assertEquals


class TestBinaryUtils {
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
}