package snma.neumann.utils

import io.mockk.mockk
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import snma.neumann.model.BusModel
import snma.neumann.model.MemoryModel
import java.util.stream.Stream
import kotlin.test.assertEquals

class TestMemoryModelSaver {
    companion object {
        data class SampleForRangeCalculation(
            val name: String,
            val modelRange: IntRange,
            val notEmptyRanges: List<IntRange>,
            val expectedOutput: List<IntRange>,
        )

        @Suppress("unused")
        @JvmStatic
        fun genSamplesForRangeCalculation(): Stream<SampleForRangeCalculation> = Stream.of(
            SampleForRangeCalculation(
                "Simple case",
                0..0xFF,
                listOf(
                    0x10..0x2F,
                ),
                listOf(
                    0x10..0x1F,
                    0x20..0x2F,
                )
            ),
            SampleForRangeCalculation(
                "With small range of empty cells",
                0x100..0x1FF,
                listOf(
                    0x110..0x112,
                    0x114..0x11A,
                ),
                listOf(
                    0x110..0x11A,
                )
            ),
            SampleForRangeCalculation(
                "Include first and last cells",
                0x100..0x1FF,
                listOf(
                    0x100..0x102,
                    0x104..0x107,
                    0x1FA..0x1FE,
                ),
                listOf(
                    0x100..0x107,
                    0x1FA..0x1FE,
                )
            ),
            SampleForRangeCalculation(
                "All the range",
                0x300..0x330,
                listOf(
                    0x300..0x330,
                ),
                listOf(
                    0x300..0x30F,
                    0x310..0x31F,
                    0x320..0x32F,
                    0x330..0x330,
                )
            ),
        )
    }

    @ParameterizedTest
    @MethodSource("genSamplesForRangeCalculation")
    fun calculateNotEmptyRanges(sample: SampleForRangeCalculation) {
        val busModel = mockk<BusModel>()
        val memoryModel = MemoryModel(busModel, sample.modelRange)
        for (range in sample.notEmptyRanges) {
            range.forEach { memoryModel.getRequiredMemoryCellByAddress(it).intValue = 0xFF }
        }
        @Suppress("DEPRECATION")
        assertEquals(sample.expectedOutput, MemoryModelSaver.calculateNotEmptyRanges(memoryModel))
    }
}