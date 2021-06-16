package snma.neumann.utils

import snma.neumann.Constants
import snma.neumann.model.MemoryModel
import java.io.File

object MemoryModelSaver {
    @Deprecated("Supposed to be used only in this object and in tests")
    fun calculateNotEmptyRanges(memoryModel: MemoryModel): List<IntRange> {
        val emptyPredicate: (Int) -> Boolean = { memoryModel.getRequiredMemoryCellByAddress(it).intValue == 0 }
        val notEmptyPredicate: (Int) -> Boolean = { memoryModel.getRequiredMemoryCellByAddress(it).intValue != 0 }

        val firstNotEmptyCell = memoryModel.addressRange.firstOrNull(notEmptyPredicate) ?: return emptyList()
        val lastNotEmptyCell = memoryModel.addressRange.last(notEmptyPredicate)
        val emptyRanges = mutableListOf<IntRange>()
        var ptr: Int = firstNotEmptyCell
        while (ptr < lastNotEmptyCell) {
            val emptiesFirst = (ptr..lastNotEmptyCell).firstOrNull(emptyPredicate) ?: break
            val emptiesLast = ((emptiesFirst + 1)..lastNotEmptyCell).first(notEmptyPredicate) - 1
            if (emptiesLast - emptiesFirst + 1 >= Constants.Utils.Saver.REMOVE_EMPTY_CELLS_MIN_COUNT) {
                emptyRanges.add(emptiesFirst..emptiesLast)
            }
            ptr = emptiesLast + 1
        }

        val rangesToWrite = mutableListOf<IntRange>()
        ptr = firstNotEmptyCell
        for (emptyRange in emptyRanges) {
            val notEmptyRange = ptr until emptyRange.first
            ptr = emptyRange.last + 1
            notEmptyRange.splitToPieces(Constants.Utils.Saver.MAX_CELLS_IN_LINE).forEach { rangesToWrite.add(it) }
        }
        (ptr..lastNotEmptyCell).splitToPieces(Constants.Utils.Saver.MAX_CELLS_IN_LINE).forEach { rangesToWrite.add(it) }

        return rangesToWrite
    }

    private fun IntRange.splitToPieces(maxSize: Int): List<IntRange> {
        val answer = mutableListOf<IntRange>()

        var rangeLeft = this
        while (rangeLeft.count() > maxSize) {
            val nextStep = rangeLeft.first + maxSize
            val smallRange = rangeLeft.first until nextStep
            rangeLeft = nextStep .. rangeLeft.last
            answer.add(smallRange)
        }
        answer.add(rangeLeft)

        return answer
    }

    fun save(memoryModel: MemoryModel, file: File) {
        file.printWriter().use { writer ->
            @Suppress("DEPRECATION")
            for (range in calculateNotEmptyRanges(memoryModel)) {
                writer.print(CommonUtils.intToShortPrefixedHexString(range.first))
                writer.print(": ")
                val dataSequence = range.asSequence()
                    .map { CommonUtils.intToShortPrefixedHexString(
                        memoryModel.getRequiredMemoryCellByAddress(it).intValue) }
                writer.println(dataSequence.joinToString(","))
            }
        }
    }
}