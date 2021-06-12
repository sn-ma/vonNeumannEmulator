package snma.neumann.model

class Simulation {
    val busModel = BusModel()
    val cpuModel = CpuModel(busModel)
    val memoryModel = MemoryModel(busModel, 0..1023)

    private val peripheralHardware: List<PeripheralHardwareItem> = listOf(
        memoryModel,
    )

    private val allHardware: List<HardwareItem> = listOf(cpuModel) + peripheralHardware

    private var lastTickWasPeripheral = false

    fun tick() {
        cleanWasRecentlyModified()
        lastTickWasPeripheral = if (busModel.modeBus.value == BusModel.Mode.IDLE || lastTickWasPeripheral) {
            cpuModel.tick()
            false
        } else {
            peripheralHardware.forEach { it.tick() }
            true
        }
    }

    fun reset() {
        busModel.reset()
        allHardware.forEach { it.reset() }
        cleanWasRecentlyModified()
    }

    private fun cleanWasRecentlyModified() {
        busModel.cleanCellsWasRecentlyModified()
        allHardware
            .asSequence()
            .flatMap { it.memoryCells }
            .forEach { it.cleanWasRecentlyModified() }
    }
}