package snma.neumann.model

import javafx.beans.property.SimpleIntegerProperty
import snma.neumann.Constants
import tornadofx.getValue
import tornadofx.setValue
import kotlin.math.ceil


class MemoryCellModel(
    val type: Type
) : AbstractCellModel() {
    val valueProperty = object : SimpleIntegerProperty(0) {
        override fun set(newValue: Int) {
            super.set(newValue and type.bitmask)
            wasRecentlyModifiedPropertyRW.set(true)
        }
    }
    var value: Int by valueProperty

    enum class Type(val bitsCount: Int) {
        DATA_CELL(Constants.Model.BITS_IN_NORMAL_CELL),
        ADDRESS_CELL(Constants.Model.BITS_IN_NORMAL_CELL),
        FLAGS_CELL(Constants.Model.BITS_IN_FLAGS_MEM_CELL),
        ;

        val bytesCount = ceil(bitsCount / 8.0).toInt()
        val bitmask = (-1 shl bitsCount).inv()
    }
}