package snma.neumann.model

abstract class BusConnectedHardwareItem(val busModel: BusModel): HardwareItem() {
    abstract fun tick()

    abstract override val memoryCells: Collection<AbstractCellModel<*>>
}