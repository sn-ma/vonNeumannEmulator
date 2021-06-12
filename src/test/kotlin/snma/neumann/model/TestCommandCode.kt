package snma.neumann.model

import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class TestCommandCode {
    @Test
    fun consistency() {
        CommandCode.values().forEach {
            assertTrue(it.argsCount in 0..2, "$it has an unexpected number of arguments")
        }
    }
}