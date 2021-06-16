package snma.neumann.gui

import javafx.scene.control.TextField
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.text.Text
import snma.neumann.Constants
import snma.neumann.gui.GuiUtils.hardwareItemView
import snma.neumann.gui.GuiUtils.memCellTextField
import snma.neumann.model.CpuModel
import tornadofx.*

class CpuView(private val cpuModel: CpuModel) : View("CPU") {
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
            hgap = Constants.View.HGAP
            row {
                addNameAndRegister(CpuModel.RegisterDescription.R0)
                addNameAndRegister(CpuModel.RegisterDescription.R4)
            }
            row {
                addNameAndRegister(CpuModel.RegisterDescription.R1)
                addNameAndRegister(CpuModel.RegisterDescription.R5)
            }
            row {
                addNameAndRegister(CpuModel.RegisterDescription.R2)
                addNameAndRegister(CpuModel.RegisterDescription.R6)
            }
            row {
                addNameAndRegister(CpuModel.RegisterDescription.R3)
                addNameAndRegister(CpuModel.RegisterDescription.R7)
            }
            row {
                text()
            }
            row {
                addNameAndRegister(CpuModel.RegisterDescription.R_STACK_POINTER, true)
            }
            row {
                addNameAndRegister(CpuModel.RegisterDescription.R_PROGRAM_COUNTER, true)
            }
            row {
                addNameAndRegister(CpuModel.RegisterDescription.R_FLAGS, true)
            }
            row {
                text()
            }
            row {
                addNameAndRegister(CpuModel.RegisterDescription.R_A)
                addNameAndRegister(CpuModel.RegisterDescription.R_B)
            }
            row {
                text()
            }
            row {
                text(cpuModel.currCommandProperty.stringBinding { triple ->
                    if (triple == null) "" else buildString {
                        val (commandCode, addressingModeA, addressingModeB) = triple
                        append("Command:\n")
                        append(commandCode?.name ?: "-")
                        if (addressingModeA != null || addressingModeB != null) {
                            append("(")
                            addressingModeA?.let { append(it.name) }
                            if (addressingModeA != null && addressingModeB != null) {
                                append(", ")
                            }
                            addressingModeB?.let { append(it.name) }
                            append(")")
                        }
                    }
                }).gridpaneConstraints { columnSpan = 4 }
            }
        }
    }

    private fun getText(description: CpuModel.RegisterDescription) = when {
        description.isInternal -> "${description.regName}"
        description.regName == null -> "R${description.ordinal}"
        else -> "${description.regName} (R${description.ordinal})"
    }

    private fun Pane.addCellLabel(description: CpuModel.RegisterDescription, op: (Text.() -> Unit)? = null) = text(getText(description)) {
            op?.invoke(this)
        }

    private fun Pane.addCell(description: CpuModel.RegisterDescription, op: (TextField.() -> Unit)? = null): TextField {
        val memCell = cpuModel.registers[description]!!
        return memCellTextField(memCell) {
            if (description.isInternal) {
                isEditable = false
                tooltip("Internal register (not editable)")
            }
            op?.invoke(this)
        }
    }

    private fun Pane.addNameAndRegister(description: CpuModel.RegisterDescription, span: Boolean = false) {
        if (!span) {
            addCellLabel(description)
        } else {
            addCellLabel(description).gridpaneConstraints { columnSpan = 3 }
            text()
            text()
        }
        addCell(description)
    }
}