package snma.neumann.model

import javafx.beans.property.Property
import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.ReadOnlyBooleanWrapper

abstract class AbstractCellModel<T: Any>(private val defaultValue: T) {
    abstract val valueProperty: Property<T>
    abstract var value: T

    protected val wasRecentlyModifiedPropertyRW = ReadOnlyBooleanWrapper(false)
    val wasRecentlyModifiedProperty: ReadOnlyBooleanProperty = wasRecentlyModifiedPropertyRW.readOnlyProperty
    val wasRecentlyModified get() = wasRecentlyModifiedProperty.get()

    fun cleanWasRecentlyModified() {
        wasRecentlyModifiedPropertyRW.set(false)
    }

    fun reset() {
        value = defaultValue
    }
}