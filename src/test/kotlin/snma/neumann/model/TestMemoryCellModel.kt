package snma.neumann.model

import kotlin.test.Test
import kotlin.test.assertEquals

class TestMemoryCellModel {
    @Test
    fun bitmask() {
        check(MemoryCellModel.Type.FLAGS_CELL.bitsCount == 3) { "Test's expectations wasn't satisfied" }
        val mc = MemoryCellModel(type = MemoryCellModel.Type.FLAGS_CELL)
        for (value in listOf(0, 0b001, 0b111)) {
            mc.value = value
            assertEquals(value, mc.value)
        }

        mc.value = 0b1010
        assertEquals(0b010, mc.value)

        mc.value = 0b11111111010
        assertEquals(0b010, mc.value)
    }
}