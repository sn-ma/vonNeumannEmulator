package snma.neumann.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TestAddressingMode {
    @Test
    fun getByFirstByte() {
        listOf(
            0b0010_1010 to AddressingMode.Companion.CONSTANT,
            0b0101_0101 to AddressingMode.Companion.REGISTER,
            0b1000_1111 to AddressingMode.Companion.DIRECT,
            0b1111_0011 to AddressingMode.Companion.INDIRECT,
        ).forEach { (firstByte, expected) ->
            val actual = AddressingMode.getByFirstByte(firstByte)
            assertEquals(expected, actual,
                "Byte 0b${firstByte.toString(2)} mapped wrong")
        }
    }

    @Test
    fun `get the corresponding register`() {
        val firstByte = 0b0100_0101
        val cpuModel = CpuModel(BusModel())
        val expected = cpuModel.getOpenRegisterByIndex(5)
        with (AddressingMode.Companion.REGISTER) {
            val actual = getRegisterByFirstByte(cpuModel, firstByte)
            assertEquals(expected, actual,
                "getting REGISTER's value for 0b${firstByte.toString(2)} returned wrong value")
        }
    }
}