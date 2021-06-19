package snma.neumann.model

import org.junit.jupiter.api.BeforeEach
import kotlin.test.*

class TestMemoryModel {
    lateinit var busModel: BusModel
    lateinit var memoryModel: MemoryModel

    @BeforeEach
    fun init() {
        busModel = BusModel()
        memoryModel = MemoryModel(busModel, 256..511)
    }

    @Test
    fun memoryCellByAddress() {
        assertTrue("") {
            memoryModel.getRequiredMemoryCellByAddress(256) === memoryModel.memoryCells[0]
        }
        assertTrue("") {
            memoryModel.getRequiredMemoryCellByAddress(300) === memoryModel.memoryCells[300 - 256]
        }
        assertFails {
            memoryModel.getRequiredMemoryCellByAddress(255)
        }
    }

    @Test
    fun `read on tick`() {
        val address = 300
        val data = 123

        memoryModel.getRequiredMemoryCellByAddress(address).value = data

        busModel.apply {
            addressBus.value = address
            dataBus.value = 0
            modeBus.value = BusModel.Mode.READ
        }

        assertNotEquals(data, busModel.dataBus.value)

        memoryModel.tick()

        assertEquals(data, busModel.dataBus.value)
    }

    @Test
    fun `write on tick`() {
        val address = 300
        val data = 123

        memoryModel.getRequiredMemoryCellByAddress(address).value = 0

        busModel.apply {
            addressBus.value = address
            dataBus.value = data
            modeBus.value = BusModel.Mode.WRITE
        }

        assertNotEquals(data, memoryModel.getRequiredMemoryCellByAddress(address).value)

        memoryModel.tick()

        assertEquals(data, memoryModel.getRequiredMemoryCellByAddress(address).value)
    }

    @Test
    fun `idle on tick`() {
        val address = 300
        val busData = 123
        val memData = 45

        memoryModel.getRequiredMemoryCellByAddress(address).value = memData

        busModel.apply {
            addressBus.value = address
            dataBus.value = busData
            modeBus.value = BusModel.Mode.IDLE
        }

        memoryModel.tick()

        assertEquals(busData, busModel.dataBus.value)
        assertEquals(memData, memoryModel.getRequiredMemoryCellByAddress(address).value)
    }
}