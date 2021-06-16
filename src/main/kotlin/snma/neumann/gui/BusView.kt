package snma.neumann.gui

import snma.neumann.Constants
import snma.neumann.gui.GuiUtils.enumCellView
import snma.neumann.gui.GuiUtils.hardwareItemView
import snma.neumann.gui.GuiUtils.memCellTextField
import snma.neumann.model.BusModel
import tornadofx.View
import tornadofx.gridpane
import tornadofx.row
import tornadofx.text

class BusView(private val busModel: BusModel): View("Bus") {
    override val root = hardwareItemView(busModel, "Bus") {
        gridpane {
            hgap = Constants.View.HGAP
            row {
                text("Address")
                memCellTextField(busModel.addressBus)
            }
            row {
                text("Data")
                memCellTextField(busModel.dataBus)
            }
            row {
                text("Mode")
                enumCellView(busModel.modeBus)
            }
        }
    }
}