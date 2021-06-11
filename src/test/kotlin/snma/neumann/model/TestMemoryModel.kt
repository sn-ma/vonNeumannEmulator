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
            memoryModel.memoryCellByAddress(256) === memoryModel.memoryCells[0]
        }
        assertTrue("") {
            memoryModel.memoryCellByAddress(300) === memoryModel.memoryCells[300 - 256]
        }
        assertFails {
            memoryModel.memoryCellByAddress(255)
        }
    }

    @Test
    fun `read on tick`() {
        val address = 300
        val data = 123

        memoryModel.memoryCellByAddress(address).value = data

        busModel.apply {
            addressBus.value = address
            dataBus.value = 0
            modeBusValue = BusModel.Mode.READ
        }

        assertNotEquals(data, busModel.dataBus.value)

        memoryModel.tick()

        assertEquals(data, busModel.dataBus.value)
    }

    @Test
    fun `write on tick`() {
        val address = 300
        val data = 123

        memoryModel.memoryCellByAddress(address).value = 0

        busModel.apply {
            addressBus.value = address
            dataBus.value = data
            modeBusValue = BusModel.Mode.WRITE
        }

        assertNotEquals(data, memoryModel.memoryCellByAddress(address).value)

        memoryModel.tick()

        assertEquals(data, memoryModel.memoryCellByAddress(address).value)
    }

    @Test
    fun `idle on tick`() {
        val address = 300
        val busData = 123
        val memData = 45

        memoryModel.memoryCellByAddress(address).value = memData

        busModel.apply {
            addressBus.value = address
            dataBus.value = busData
            modeBusValue = BusModel.Mode.IDLE
        }

        memoryModel.tick()

        assertEquals(busData, busModel.dataBus.value)
        assertEquals(memData, memoryModel.memoryCellByAddress(address).value)
    }
}