package snma.neumann.model

import snma.neumann.CommonUtils.countValuableBits
import snma.neumann.Constants

sealed class AddressingMode(private val bitmask: Int) { // TODO make enum again
    init {
        check(bitmask.countValuableBits() <= Constants.Model.BITS_IN_ADDRESS_FOR_ADDRESSING) {
            "Too much addressing modes for given number of addressing bits"
        }
    }

    companion object {
        object CONSTANT: AddressingMode(0b00)
        object REGISTER: AddressingMode(0b01) {
            fun getRegisterByFirstByte(cpuModel: CpuModel, firstByte: Int) =
                cpuModel.getOpenRegisterByIndex(firstByte and dataBitmask)
        }
        object DIRECT: AddressingMode(0b10)
        object INDIRECT: AddressingMode(0b11)

        private val dataBitmask = "1".repeat(8 - Constants.Model.BITS_IN_ADDRESS_FOR_ADDRESSING).toInt(2)

        private val allAddressingModes: List<AddressingMode> by lazy {
            listOf(CONSTANT, REGISTER, DIRECT, INDIRECT).apply {
                check(this
                    .withIndex()
                    .all { (idx, mode) ->
                        mode.bitmask == idx
                    })
            }
        }

        fun getByFirstByte(firstByte: Int): AddressingMode? {
            val address = firstByte shr (Constants.Model.BITS_IN_ADDRESS_MEM_CELL - 8)
            return if (address in allAddressingModes.indices) allAddressingModes[address] else null
        }
    }
}