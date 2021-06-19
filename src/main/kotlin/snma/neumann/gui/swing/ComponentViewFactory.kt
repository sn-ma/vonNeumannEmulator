package snma.neumann.gui.swing

import net.miginfocom.swing.MigLayout
import snma.neumann.model.HardwareItem
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

abstract class ComponentViewFactory<T: HardwareItem>(private val title: String, protected val model: T) {
    open fun createTitle() = JLabel("<html><h1 style='margin-bottom: 0px;'>$title</h1></html>")
    abstract fun createSubtitle(): JLabel?
    abstract fun createBody(): JComponent

    fun createButtonsPane() = JPanel(MigLayout("ins 0")).apply {
        addButton("Reset") { model.reset() }
        attachMoreButtons()
    }

    protected open fun JPanel.attachMoreButtons() {}

    protected fun JPanel.addButton(text: String, action: () -> Unit) {
        add(JButton(text).apply {
            addActionListener { action() }
        })
    }
}