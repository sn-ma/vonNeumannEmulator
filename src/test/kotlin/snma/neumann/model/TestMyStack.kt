package snma.neumann.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestMyStack {
    @Test
    fun test() {
        val stack = MyStack<Int>()
        stack.push(0)
        stack.push(1)
        stack.push(2, 3)

        assertEquals(3, stack.pop())
        assertEquals(2, stack.poll())
        assertEquals(1, stack.poll())
        assertFalse(stack.isEmpty())
        stack.clear()
        assertTrue(stack.isEmpty())
        assertEquals(null, stack.poll())
        assertFails { stack.pop() }
    }
}