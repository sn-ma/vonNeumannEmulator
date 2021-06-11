package snma.neumann.model

abstract class HardwareItem(val busModel: BusModel) {
    abstract fun tick()

    abstract val memoryCells: Iterable<MemoryCellModel>
}