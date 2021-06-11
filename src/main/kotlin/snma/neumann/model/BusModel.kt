package snma.neumann.model

import javafx.beans.property.SimpleObjectProperty
import tornadofx.getValue
import tornadofx.setValue

class BusModel {
    val addressBus = MemoryCellModel(Constants.BITS_IN_ADDRESS_MEM_CELL)
    val dataBus = MemoryCellModel(Constants.BITS_IN_NORMAL_MEM_CELL)

    val modeBusProperty = SimpleObjectProperty(Mode.IDLE)
    var modeBusValue: Mode by modeBusProperty

    enum class Mode {
        READ, WRITE, IDLE
    }
}