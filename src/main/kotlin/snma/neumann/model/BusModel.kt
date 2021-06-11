package snma.neumann.model

class BusModel {
    val addressBus = MemoryCellModel(MemoryCellModel.Type.ADDRESS_CELL)
    val dataBus = MemoryCellModel(MemoryCellModel.Type.DATA_CELL)
    val modeBus = EnumCellModel(Mode.IDLE)

    fun cleanCellsWasRecentlyModified() {
        addressBus.cleanWasRecentlyModified()
        dataBus.cleanWasRecentlyModified()
        modeBus.cleanWasRecentlyModified()
    }

    enum class Mode {
        READ, WRITE, IDLE
    }
}