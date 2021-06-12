package snma.neumann.model


class MemoryModel (
    busModel: BusModel,
    addressRange: IntRange,
) : PeripheralHardwareItem(busModel, addressRange) {
    override val memoryCells = addressRange.map { MemoryCellModel(type = MemoryCellModel.Type.DATA_CELL) }

    fun memoryCellByAddress(address: Int): MemoryCellModel {
        check(address in addressRange) { "Address $address is not in range $addressRange" }
        return memoryCells[address - addressRange.first]
    }

    override fun read(address: Int): Int {
        return memoryCellByAddress(address).value
    }

    override fun write(address: Int, value: Int) {
        memoryCellByAddress(address).value = value
    }
}