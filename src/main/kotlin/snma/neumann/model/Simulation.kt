package snma.neumann.model

class Simulation {
    val busModel = BusModel()
    val cpuModel = CpuModel(busModel)
    val memoryModel = MemoryModel(busModel, 0..255)

    private val allHardware: List<HardwareItem> = listOf(
        cpuModel, // CPU should be the first in this list
        memoryModel,
    )

    fun tick() {
        busModel.cleanCellsWasRecentlyModified()
        allHardware
            .asSequence()
            .flatMap { it.memoryCells }
            .forEach { it.cleanWasRecentlyModified() }
        allHardware
            .forEach { it.tick() }
    }
}