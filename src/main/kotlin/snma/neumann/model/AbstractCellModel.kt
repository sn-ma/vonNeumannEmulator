package snma.neumann.model

import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.ReadOnlyBooleanWrapper

abstract class AbstractCellModel {
    protected val wasRecentlyModifiedPropertyRW = ReadOnlyBooleanWrapper(false)
    val wasRecentlyModifiedProperty: ReadOnlyBooleanProperty = wasRecentlyModifiedPropertyRW.readOnlyProperty
    val wasRecentlyModified get() = wasRecentlyModifiedProperty.get()

    fun cleanWasRecentlyModified() {
        wasRecentlyModifiedPropertyRW.set(false)
    }
}