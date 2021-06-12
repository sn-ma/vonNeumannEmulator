package snma.neumann.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TestAddressingMode {
    @Test
    fun getByFirstByte() {
        listOf(
            0b0010_1010 to AddressingMode.CONSTANT,
            0b0101_0101 to AddressingMode.REGISTER,
            0b1000_1111 to AddressingMode.DIRECT,
            0b1111_0011 to AddressingMode.INDIRECT,
        ).forEach { (firstByte, expected) ->
            val actual = AddressingMode.getByFirstByte(firstByte)
            assertEquals(expected, actual,
                "Byte 0b${firstByte.toString(2)} must map to $expected, but got $actual")
        }
    }
}