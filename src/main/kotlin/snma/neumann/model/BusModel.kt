package snma.neumann.model

class BusModel: HardwareItem() {
    val addressBus = MemoryCellModel(MemoryCellModel.Type.ADDRESS_CELL)
    val dataBus = MemoryCellModel(MemoryCellModel.Type.DATA_CELL)
    val modeBus = EnumCellModel(Mode.IDLE)

    override val memoryCells = listOf(addressBus, dataBus, modeBus)

    enum class Mode {
        READ, WRITE, IDLE
    }
}