package snma.neumann.help

import snma.neumann.model.AddressingMode
import snma.neumann.model.CommandCode
import snma.neumann.utils.CommonUtils
import kotlin.reflect.KClass

object HelpGenerator {
    sealed interface HelpSubject { val title: String }
    data class HelpArticle(override val title: String, val content: String): HelpSubject
    data class HelpEnumTable<T: Enum<T>>(
        override val title: String,
        val columnExtractors: List<Pair<String, (T) -> String>>,
        val type: KClass<T>
    ): HelpSubject {
        private val values get() = type.java.enumConstants
        private fun extractColumnsContent(value: T) = columnExtractors.asSequence().map { (_, extractor) -> extractor(value) }
        fun extractAllColumnContents() = values.asSequence().map { extractColumnsContent(it) }
    }
    private inline fun<reified T: Enum<T>> helpEnumTable(
        title: String,
        columns: List<Pair<String, (T) -> String>>,
    ) = HelpEnumTable(title, columns, T::class)

    fun generate() = listOf(
        HelpArticle("Introduction", "Command word is build up of three parts: the command (8 bits) and" +
                "(4 bits for each) addressing modes for first and second argument, if any."),
        helpEnumTable("Command codes", listOf(
            "Code (HEX)" to { CommonUtils.intToHexString(it.intCode, 1)!! },
            "Short Name" to CommandCode::stringCode,
            "Meaning" to CommandCode::meaning,
            "Type" to { it.commandType.description },
            "Comment" to { it.comment ?: "-" },
        )),
        helpEnumTable("Addressing modes", listOf(
            "Code (HEX)" to { it.bitmask.toString() },
            "Name" to AddressingMode::shortRepresentation,
            "Description" to AddressingMode::description,
        ))
    )
}