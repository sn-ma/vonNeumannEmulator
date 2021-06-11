package snma.neumann.gui

import snma.neumann.gui.GuiUtils.enumCellView
import snma.neumann.gui.GuiUtils.hardwareItemTitle
import snma.neumann.gui.GuiUtils.memCellTextField
import snma.neumann.model.BusModel
import tornadofx.*

class BusView(val busModel: BusModel): View("Bus") {
    override val root = vbox {
        hardwareItemTitle("Bus")
        gridpane {
            row {
                text("Address: ")
                memCellTextField(busModel.addressBus)
            }
            row {
                text("Data: ")
                memCellTextField(busModel.dataBus)
            }
            row {
                text("Mode: ")
                enumCellView(busModel.modeBus)
            }
        }
    }
}