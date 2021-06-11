package snma.neumann.model

import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.ReadOnlyBooleanWrapper
import javafx.beans.property.SimpleIntegerProperty
import tornadofx.getValue
import tornadofx.setValue
import kotlin.math.ceil


class MemoryCellModel(val bitsCount: Int) {
    val bytesCount: Int
        get() = ceil(bitsCount / 8.0).toInt()

    private val bitmask: Int = (-1 shl bitsCount).inv()

    val valueProperty = object : SimpleIntegerProperty(0) {
        override fun set(newValue: Int) {
            super.set(newValue and bitmask)
            wasRecentlyModifiedPropertyRW.set(true)
        }
    }
    var value by valueProperty

    private val wasRecentlyModifiedPropertyRW = ReadOnlyBooleanWrapper(false)
    val wasRecentlyModifiedProperty: ReadOnlyBooleanProperty = wasRecentlyModifiedPropertyRW.readOnlyProperty
    val wasRecentlyModified get() = wasRecentlyModifiedProperty.get()

    fun cleanWasRecentlyModified() {
        wasRecentlyModifiedPropertyRW.set(false)
    }
}