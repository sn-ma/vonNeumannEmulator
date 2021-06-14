package snma.neumann.gui

import javafx.scene.paint.Color
import snma.neumann.gui.GuiUtils.hardwareItemView
import snma.neumann.gui.GuiUtils.memCellTextField
import snma.neumann.model.CpuModel
import tornadofx.*

class CpuView(val cpuModel: CpuModel) : View("CPU") {
    override val root = hardwareItemView(cpuModel, "CPU", {
        text {
            style {
                fill = Color.RED
            }
            visibleWhen(cpuModel.isStoppedProperty.isNotEqualTo(CpuModel.CommonStopMode.NOT_STOPPED))
            cpuModel.isStoppedProperty.addListener { _, _, newValue ->
                text = when(newValue) {
                    CpuModel.CommonStopMode.NOT_STOPPED -> "Working" // This message for now is supposed to be never shown
                    CpuModel.CommonStopMode.HALTED -> "Halted"
                    is CpuModel.ErrorStopMode -> newValue.description
                }
            }
        }
    }) {
        gridpane {
            for (description in CpuModel.RegisterDescription.values()) {
                val memCell = cpuModel.registers[description]!!

                row {
                    label(when {
                        description.isInternal -> "${description.regName}"
                        description.regName == null -> "R${description.ordinal}"
                        else -> "${description.regName} (R${description.ordinal})"
                    })
                    memCellTextField(memCell) {
                        if (description.isInternal) {
                            isEditable = false
                            tooltip("Internal register (not editable)")
                        }
                    }
                }
            }
        }
    }
}