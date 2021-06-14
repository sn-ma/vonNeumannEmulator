package snma.neumann.model

abstract class HardwareItem {
    abstract val memoryCells: Collection<AbstractCellModel<*>>

    open fun reset() {
        memoryCells.forEach { it.reset() }
    }
}