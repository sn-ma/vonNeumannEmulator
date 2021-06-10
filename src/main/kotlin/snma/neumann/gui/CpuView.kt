package snma.neumann.gui

import javafx.scene.Parent
import snma.neumann.gui.GuiUtils.memCellTextField
import snma.neumann.model.CpuModel
import tornadofx.*

class CpuView(val cpuModel: CpuModel) : View("CPU") {
    override val root = gridpane {
        for ((idx, description) in CpuModel.RegisterDescription.values().withIndex()) {
            val memCell = cpuModel.registers[description]!!

            row {
                label(when {
                    description.isInternal -> "${description.regName}"
                    description.regName == null -> "R${description.ordinal}"
                    else -> "${description.regName} (R${description.ordinal})"
                })
                memCellTextField(memCell)
            }
        }
    }
}