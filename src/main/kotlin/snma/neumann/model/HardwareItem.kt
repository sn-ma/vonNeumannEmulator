package snma.neumann.model

abstract class HardwareItem(val busModel: BusModel) {
    abstract fun tick()

    open fun reset() {
        memoryCells.forEach { it.value = 0 }
    }

    abstract val memoryCells: Iterable<MemoryCellModel>
}