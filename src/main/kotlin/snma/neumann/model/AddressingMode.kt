package snma.neumann.model

import snma.neumann.Constants
import snma.neumann.utils.CommonUtils.countValuableBits

/**
 * Addressing mode for operands are coded by the last bits of the command word.
 * To get addressing mode, use function [AddressingMode.parse]
 */
enum class AddressingMode(val shortRepresentation: String) {
    CONSTANT("CONST"),
    REGISTER("REG"),
    DIRECT("DIR"),
    REGISTER_INDIRECT("(REG)"),
    ;

    private val bitmask: Int get() = ordinal

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