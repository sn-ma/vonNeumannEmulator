package snma.neumann.model

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.assertEquals

class TestParsingCommandWord {
    data class SampleParsingCommandWord(
        val commandWord: Int,
        val expectedCode: CommandCode,
        val expectedAddressingModeA: AddressingMode,
        val expectedAddressingModeB: AddressingMode,
    )

    companion object {
        @JvmStatic
        @Suppress("UNUSED")
        fun getDataSampleStream(): Stream<SampleParsingCommandWord> = Stream.of(
            SampleParsingCommandWord(
                0b0000_0001_0000_0000,
                CommandCode.DLY,
                AddressingMode.CONSTANT,
                AddressingMode.CONSTANT
            ),
            SampleParsingCommandWord(
                0b0000_0001_0001_0000,
                CommandCode.DLY,
                AddressingMode.REGISTER,
                AddressingMode.CONSTANT
            ),
            SampleParsingCommandWord(
                0b0000_0001_0010_0000,
                CommandCode.DLY,
                AddressingMode.DIRECT,
                AddressingMode.CONSTANT
            ),
            SampleParsingCommandWord(
                0b0000_0001_0011_0000,
                CommandCode.DLY,
                AddressingMode.REGISTER_INDIRECT,
                AddressingMode.CONSTANT
            ),
        )
    }

    @ParameterizedTest
    @MethodSource("getDataSampleStream")
    fun parseCommandWord(sample: SampleParsingCommandWord) {
        val commandCode = CommandCode.parse(sample.commandWord)
        val addressingModeA = AddressingMode.parse(sample.commandWord, 0)
        val addressingModeB = AddressingMode.parse(sample.commandWord, 1)

        assertEquals(sample.expectedCode, commandCode)
        assertEquals(sample.expectedAddressingModeA, addressingModeA)
        assertEquals(sample.expectedAddressingModeB, addressingModeB)
    }
}