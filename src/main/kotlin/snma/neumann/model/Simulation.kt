package snma.neumann.model

class Simulation {
    val cpuModel = CpuModel()

    val allHardware: List<HardwareItem> = listOf(cpuModel) // CPU should be the first in this list

    fun tick() {
        allHardware
            .asSequence()
            .flatMap { it.memoryCells }
            .forEach { it.cleanWasRecentlyModified() }
        allHardware
            .forEach { it.tick() }
    }
}