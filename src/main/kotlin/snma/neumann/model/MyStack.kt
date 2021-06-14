package snma.neumann.model

import java.util.*

class MyStack<T> {
    private val deque: Deque<T> = LinkedList()

    fun push(item: T) {
        deque.addFirst(item)
    }

    fun push(vararg items: T) {
        for (item in items) {
            deque.addFirst(item)
        }
    }

    fun pop(): T = deque.removeFirst()

    fun poll(): T? = deque.pollFirst()

    fun clear() {
        deque.clear()
    }

    fun isEmpty(): Boolean = deque.isEmpty()

    override fun toString() = deque.toString()
}