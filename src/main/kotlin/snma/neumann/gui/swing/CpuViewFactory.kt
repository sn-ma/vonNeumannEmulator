package snma.neumann.gui.swing

import net.miginfocom.swing.MigLayout
import snma.neumann.gui.swing.MySwingTools.bindText
import snma.neumann.model.CpuModel
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSeparator

class CpuViewFactory(model: CpuModel): ComponentViewFactory<CpuModel>("CPU", model) {
    override fun createSubtitle() = JLabel().apply {
        bindText(model.isStoppedObservable.observeOn(MySwingTools.scheduler).map { value ->
            when(value) {
                CpuModel.CommonStopMode.NOT_STOPPED -> "<html><font>Ready</font></html>"
                CpuModel.CommonStopMode.HALTED -> "<html><font color=\"yellow\">Halted</font></html>"
                is CpuModel.ErrorStopMode -> "<html><font color=\"red\">${value.description}</font></html>"
            }
        })
    }

    override fun createBody() = JPanel(MigLayout("ins 0, wrap 4")).apply {
        fun getText(description: CpuModel.RegisterDescription) = when {
            description.isInternal -> "${description.regName}"
            description.regName == null -> "R${description.ordinal}"
            else -> "${description.regName} (R${description.ordinal})"
        }

        fun addLittleTitle(text: String) {
            add(JLabel("<html><b>$text</b></html>"), "span")
        }

        addLittleTitle("General purpose registers")

        listOf(
            CpuModel.RegisterDescription.R0,
            CpuModel.RegisterDescription.R4,
            CpuModel.RegisterDescription.R1,
            CpuModel.RegisterDescription.R5,
            CpuModel.RegisterDescription.R2,
            CpuModel.RegisterDescription.R6,
            CpuModel.RegisterDescription.R3,
            CpuModel.RegisterDescription.R7,
        ).forEach { desc ->
            add(JLabel(getText(desc)))
            add(MySwingTools.createMemoryCellTextField(model.registers[desc]!!))
        }

        add(JSeparator(JSeparator.HORIZONTAL), "span")
        addLittleTitle("System registers")

        listOf(
            CpuModel.RegisterDescription.R_PROGRAM_COUNTER,
            CpuModel.RegisterDescription.R_STACK_POINTER,
            CpuModel.RegisterDescription.R_FLAGS,
        ).forEach { desc ->
            add(JLabel(getText(desc)), "span 3")
            add(MySwingTools.createMemoryCellTextField(model.registers[desc]!!))
        }

        add(JSeparator(JSeparator.HORIZONTAL), "span")
        addLittleTitle("Operand registers (RO)")

        listOf(
            CpuModel.RegisterDescription.R_A,
            CpuModel.RegisterDescription.R_B,
        ).forEach { desc ->
            add(JLabel(getText(desc)))
            add(MySwingTools.createMemoryCellTextField(model.registers[desc]!!).apply {
                isEditable = false
            })
        }

        add(JSeparator(JSeparator.HORIZONTAL), "span")
        addLittleTitle("Command")

        add(JLabel().apply {
            model.currCommandObservable
                .distinctUntilChanged()
                .subscribeOn(MySwingTools.scheduler)
                .subscribe { triple ->
                    text = buildString {
                        val (commandCode, addressingModeA, addressingModeB) = triple
                        append(commandCode?.name ?: "-")
                        addressingModeA?.let {
                            append(' ')
                            append(it.shortRepresentation)
                        }
                        addressingModeB?.let {
                            append(' ')
                            append(it.shortRepresentation)
                        }
                    }
                }
        }, "span")
    }
}