package snma.neumann.model

import snma.neumann.CommonUtils.countValuableBits
import snma.neumann.Constants

enum class AddressingMode {
    CONSTANT,
    REGISTER,
    DIRECT,
    INDIRECT,
    ;

    private val bitmask get() = ordinal

    init {
        check(bitmask.countValuableBits() <= Constants.Model.BITS_IN_ADDRESS_FOR_ADDRESSING) {
            "Too much addressing modes for given number of addressing bits"
        }
    }

    companion object {
        fun getByFirstByte(firstByte: Int): AddressingMode? {
            val bitmask = firstByte shr (8 - Constants.Model.BITS_IN_ADDRESS_FOR_ADDRESSING)
            val values = values()
            return if (bitmask in values.indices) values[bitmask] else null
        }
    }
}