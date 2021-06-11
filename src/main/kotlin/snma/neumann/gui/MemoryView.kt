package snma.neumann.gui

import javafx.event.EventTarget
import javafx.scene.control.ScrollPane
import snma.neumann.CommonUtils.intToHexString
import snma.neumann.gui.GuiUtils.hardwareItemTitle
import snma.neumann.gui.GuiUtils.memCellTextField
import snma.neumann.model.MemoryCellModel
import snma.neumann.model.MemoryModel
import tornadofx.*
import kotlin.math.ceil

class MemoryView(
    private val memoryModel: MemoryModel,
    private val cellsPerRow: Int = 16,
) : View("Memory") {
    private val rowsCount = ceil(memoryModel.addressRange.count() / cellsPerRow.toDouble()).toInt()

    override val root = vbox {
        hardwareItemTitle("Memory", "address range: ${memoryModel.addressRange.first}..${memoryModel.addressRange.last}")
        scrollpane(fitToHeight = true) {
            hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
            vbarPolicy = ScrollPane.ScrollBarPolicy.ALWAYS

            gridpane {
                val addressIterator = memoryModel.addressRange.iterator()
                for (i in 0 until rowsCount) {
                    row {
                        val lineStartingAddress = addressIterator.nextInt()
                        text(intToHexString(lineStartingAddress, MemoryCellModel.Type.ADDRESS_CELL.bytesCount))
                        addMemCell(lineStartingAddress)

                        for (j in 1 until cellsPerRow) {
                            if (!addressIterator.hasNext()) {
                                break
                            }
                            addMemCell(addressIterator.nextInt())
                        }
                    }
                }
            }
        }
    }

    private fun EventTarget.addMemCell(address: Int) {
        memCellTextField(memoryModel.memoryCellByAddress(address)) {
            tooltip("Address: " + intToHexString(address, MemoryCellModel.Type.ADDRESS_CELL.bytesCount))
        }
    }
}