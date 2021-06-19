package snma.neumann.utils

import snma.neumann.model.MemoryModel
import java.io.File

object MemoryModelLoader {
    private val parseRe = """^([^:]+):\s*([^\s].*)$""".toRegex()
    private val splitRe = """,\s*""".toRegex()

    private class ParsedLine(line: String) {
        val range: IntRange
        val data: List<Int>

        init {
            val match = parseRe.find(line) ?: throw Exception("Wrong line format: '$line'")
            val rangeStartStr = match.groupValues[1]
            val rangeStart = CommonUtils.prefixedStringToInt(rangeStartStr) ?:
                throw Exception("Failed to parse address '$rangeStartStr'")
            val numberStrings = match.groupValues[2].split(splitRe)
            range = rangeStart until (rangeStart + numberStrings.size)
            data = numberStrings.map { CommonUtils.prefixedStringToInt(it.trim()) ?: throw Exception("Can't parse cell data '$it'") }
        }
    }

    fun load(memoryModel: MemoryModel, file: File) {
        val parsedLines = mutableListOf<ParsedLine>()
        file.bufferedReader().use { reader ->
            reader.forEachLine { it ->
                val line = it.replace("""#.*$""".toRegex(), "").trim()
                if (line.isNotBlank()) {
                    parsedLines.add(ParsedLine(line))
                }
            }
        }
        val cellNumbersSet = mutableSetOf<Int>()
        for (line in parsedLines) {
            for (address in line.range) {
                if (!cellNumbersSet.add(address)) {
                    throw Exception("Address $address is duplicating")
                }
            }
        }
        for (line in parsedLines) {
            for ((address, value) in line.range.asSequence().zip(line.data.asSequence())) {
                val cell = memoryModel.getRequiredMemoryCellByAddress(address)
                cell.value = value
            }
        }
    }

    class Exception(message: String): kotlin.Exception(message)
}