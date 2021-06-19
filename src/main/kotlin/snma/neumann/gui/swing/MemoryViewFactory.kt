package snma.neumann.gui.swing

import net.miginfocom.swing.MigLayout
import snma.neumann.Constants
import snma.neumann.model.MemoryCellModel
import snma.neumann.model.MemoryModel
import snma.neumann.utils.CommonUtils
import snma.neumann.utils.MemoryModelLoader
import snma.neumann.utils.MemoryModelSaver
import java.io.File
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.ceil

class MemoryViewFactory(model: MemoryModel): ComponentViewFactory<MemoryModel>("RAM", model) {
    private val fileChooser = JFileChooser().apply {
        currentDirectory = File(System.getProperty("user.home"))
        isAcceptAllFileFilterUsed = false
        addChoosableFileFilter(FileNameExtensionFilter(".neumann files", "neumann"))
    }

    override fun createSubtitle() =
        JLabel("Address range ${model.addressRange.first}..${model.addressRange.last}")

    override fun createBody(): JScrollPane {
        val rowsCount = ceil(model.addressRange.count() / Constants.View.MEMORY_CELLS_PER_ROW.toDouble()).toInt()
        val panel = JPanel(MigLayout("ins 0, wrap ${Constants.View.MEMORY_CELLS_PER_ROW + 1}")).apply {
            fun addMemCell(address: Int) {
                add(MySwingTools.createMemoryCellTextField(model.getRequiredMemoryCellByAddress(address),
                    "Address: ${CommonUtils.intToHexString(address, MemoryCellModel.Type.ADDRESS_CELL.bytesCount)}"))
            }
            val addressIterator = model.addressRange.iterator()
            for (rowIdx in 0 until rowsCount) {
                val lineStartingAddress = addressIterator.nextInt()
                add(JLabel(CommonUtils.intToHexString(lineStartingAddress,
                    MemoryCellModel.Type.ADDRESS_CELL.bytesCount)))
                addMemCell(lineStartingAddress)

                for (colIdx in 1 until Constants.View.MEMORY_CELLS_PER_ROW) {
                    if (!addressIterator.hasNext()) {
                        break
                    }
                    addMemCell(addressIterator.nextInt())
                }
            }
        }
        return JScrollPane(panel)
    }

    override fun JPanel.attachMoreButtons() {
        addButton("Save") {
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    MemoryModelSaver.save(model, fileChooser.selectedFile)
                    JOptionPane.showMessageDialog(this, "Successfully saved")
                } catch (ex: Exception) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Error while saving: ${ex.message}",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }
        addButton("Load") {
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    model.reset()
                    MemoryModelLoader.load(model, fileChooser.selectedFile)
                    JOptionPane.showMessageDialog(this, "Successfully loaded")
                } catch (ex: Exception) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Error while loading: ${ex.message}",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }
    }
}