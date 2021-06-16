package snma.neumann.model

import kotlin.test.Test
import kotlin.test.assertEquals

class TestMemoryCellModel {
    @Test
    fun bitmask() {
        check(MemoryCellModel.Type.FLAGS_CELL.bitsCount == 2) { "Test's expectations wasn't satisfied" }
        val mc = MemoryCellModel(type = MemoryCellModel.Type.FLAGS_CELL)
        for (value in listOf(0, 0b01, 0b11)) {
            mc.intValue = value
            assertEquals(value, mc.intValue)
        }

        mc.intValue = 0b1010
        assertEquals(0b10, mc.intValue)

        mc.intValue = 0b11111111010
        assertEquals(0b10, mc.intValue)
    }
}