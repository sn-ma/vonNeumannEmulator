package snma.neumann.model

class Simulation {
    val busModel = BusModel()

    val cpuModel = CpuModel(busModel)

    val allHardware: List<HardwareItem> = listOf(
        cpuModel, // CPU should be the first in this list
        MemoryModel(busModel, 0..256)
    )

    fun tick() {
        allHardware
            .asSequence()
            .flatMap { it.memoryCells }
            .forEach { it.cleanWasRecentlyModified() }
        allHardware
            .forEach { it.tick() }
    }
}