package snma.neumann.model

import javafx.beans.property.SimpleObjectProperty
import tornadofx.getValue
import tornadofx.setValue

class EnumCellModel<T: Enum<*>>(
    initialValue: T,
) : AbstractCellModel() {
    val valueProperty = object : SimpleObjectProperty<T>(initialValue) {
        override fun set(newValue: T) {
            super.set(newValue)
            wasRecentlyModifiedPropertyRW.set(true)
        }
    }
    var value: T by valueProperty
}