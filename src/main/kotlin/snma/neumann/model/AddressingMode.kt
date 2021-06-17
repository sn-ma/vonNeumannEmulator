package snma.neumann.model

import snma.neumann.Constants
import snma.neumann.utils.CommonUtils.countValuableBits

/**
 * Addressing mode for operands are coded by the last bits of the command word.
 * To get addressing mode, use function [AddressingMode.parse]
 */
enum class AddressingMode(val shortRepresentation: String, val description: String) {
    CONSTANT("CONST", "Use the address and/or the data of the given cell"),
    REGISTER("REG", "Use the register by the number in the given cell"),
    DIRECT("DIR", "Use the data in the cell by the given address"),
    REGISTER_INDIRECT("(REG)", "Use the data in the cell by the register by the given address"),
    ;

    val bitmask: Int get() = ordinal

    init {
        check(bitmask.countValuableBits() <= Constants.Model.BITS_IN_COMMAND_FOR_EACH_ADDRESSING) {
            "Too much addressing modes for given number of addressing bits"
        }
    }

    companion object {
        private val bitmaskBitmask = "1".repeat(Constants.Model.BITS_IN_COMMAND_FOR_EACH_ADDRESSING).toInt(2)

        /**
         * @param argumentIndex 0 for the first argument, 1 for second
         */
        fun parse(commandWord: Int, argumentIndex: Int): AddressingMode? {
            val bitmask = (commandWord shr (Constants.Model.BITS_IN_COMMAND_FOR_EACH_ADDRESSING * (1 - argumentIndex))) and
                    bitmaskBitmask
            val values = values()
            if (bitmask !in values.indices) {
                return null
            }
            return values[bitmask]
        }
    }
}