package snma.neumann.model

interface HardwareItem {
    fun tick()

    val memoryCells: Iterable<MemoryCell>
}