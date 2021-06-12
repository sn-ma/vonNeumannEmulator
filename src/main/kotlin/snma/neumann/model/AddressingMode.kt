package snma.neumann.model

import snma.neumann.CommonUtils.countValuableBits
import snma.neumann.Constants

enum class AddressingMode {
    CONSTANT,
    REGISTER,
    DIRECT,
    INDIRECT,
    ;

    private val bitmask: Int get() = ordinal

    init {
        check(bitmask.countValuableBits() <= Constants.Model.BITS_IN_ADDRESS_FOR_ADDRESSING) {
            "Too much addressing modes for given number of addressing bits"
        }
    }

    companion object {
        /**
         * Returns the register instance by the first byte. Only for [REGISTER] addressing mode
         */
        fun getRegisterByFirstByte(cpuModel: CpuModel, firstByte: Int): MemoryCellModel? {
            check(getByFirstByte(firstByte) == REGISTER) { "Meaningless call" }
            return cpuModel.getOpenRegisterByIndex(firstByte and dataBitmask)
        }

        private val dataBitmask = "1".repeat(8 - Constants.Model.BITS_IN_ADDRESS_FOR_ADDRESSING).toInt(2)

        fun getByFirstByte(firstByte: Int): AddressingMode? {
            val address = firstByte shr (Constants.Model.BITS_IN_ADDRESS_MEM_CELL - 8)
            val allAddressingModes = values()
            return if (address in allAddressingModes.indices) allAddressingModes[address] else null
        }
    }
}