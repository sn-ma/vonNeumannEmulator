package snma.neumann.model

import kotlin.test.Test
import kotlin.test.assertEquals

class TestMemoryCellModel {
    @Test
    fun bitmask() {
        val mc = MemoryCellModel(3)
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