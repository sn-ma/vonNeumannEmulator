package snma.neumann.model

import javafx.beans.property.SimpleObjectProperty
import tornadofx.getValue
import tornadofx.setValue

class EnumCellModel<T: Enum<T>>(
    defaultValue: T,
) : AbstractCellModel<T>(defaultValue) {
    override val valueProperty = object : SimpleObjectProperty<T>(defaultValue) {
        override fun set(newValue: T) {
            super.set(newValue)
            wasRecentlyModifiedPropertyRW.set(true)
        }
    }
    override var value: T by valueProperty
}