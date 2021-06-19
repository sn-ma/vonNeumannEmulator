package snma.neumann.gui.swing

import net.miginfocom.swing.MigLayout
import snma.neumann.model.BusModel
import javax.swing.JLabel
import javax.swing.JPanel

class BusViewFactory(model: BusModel): ComponentViewFactory<BusModel>("Bus", model) {
    override fun createSubtitle(): JLabel? = null

    override fun createBody() = JPanel(MigLayout("wrap 2, ins 0")).apply {
        add(JLabel("Address"))
        add(MySwingTools.createMemoryCellTextField(model.addressBus))

        add(JLabel("Data"))
        add(MySwingTools.createMemoryCellTextField(model.dataBus))

        add(JLabel("Mode"))
        add(MySwingTools.createEnumCellComboBox(model.modeBus))
    }
}