package snma.neumann.model

import javafx.beans.property.SimpleObjectProperty
import tornadofx.getValue
import tornadofx.setValue

class BusModel {
    val addressBus = MemoryCellModel(MemoryCellModel.Type.ADDRESS_CELL)
    val dataBus = MemoryCellModel(MemoryCellModel.Type.DATA_CELL)

    val modeBusProperty = SimpleObjectProperty(Mode.IDLE)
    var modeBusValue: Mode by modeBusProperty

    enum class Mode {
        READ, WRITE, IDLE
    }
}