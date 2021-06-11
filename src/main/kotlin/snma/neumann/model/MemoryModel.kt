package snma.neumann.model

import snma.neumann.CommonUtils.countValuableBits


class MemoryModel (
    busModel: BusModel,
    val addressRange: IntRange,
) : HardwareItem(busModel) {
    override val memoryCells = addressRange.map { MemoryCellModel(Constants.BITS_IN_NORMAL_MEM_CELL) }

    init {
        check(addressRange.first.countValuableBits() <= Constants.BITS_IN_ADDRESS_MEM_CELL
                && addressRange.last.countValuableBits() <= Constants.BITS_IN_ADDRESS_MEM_CELL) {
            "Possible addresses has to much bits"
        }
    }

    fun memoryCellByAddress(address: Int): MemoryCellModel {
        check(address in addressRange) { "Address $address is not in range $addressRange" }
        return memoryCells[address - addressRange.first]
    }

    override fun tick() {
        val currentAddress = busModel.addressBus.value
        if (currentAddress !in addressRange) {
            return
        }
        when (busModel.modeBusValue) {
            BusModel.Mode.READ -> busModel.dataBus.value = memoryCellByAddress(currentAddress).value
            BusModel.Mode.WRITE -> memoryCellByAddress(currentAddress).value = busModel.dataBus.value
            BusModel.Mode.IDLE -> { /* do nothing */}
        }
    }
}