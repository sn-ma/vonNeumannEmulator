package snma.neumann.gui

import javafx.event.EventTarget
import javafx.scene.control.Alert
import javafx.scene.control.ScrollPane
import javafx.stage.FileChooser
import snma.neumann.gui.GuiUtils.hardwareItemView
import snma.neumann.gui.GuiUtils.memCellTextField
import snma.neumann.model.MemoryCellModel
import snma.neumann.model.MemoryModel
import snma.neumann.utils.CommonUtils.intToHexString
import snma.neumann.utils.MemoryModelLoader
import snma.neumann.utils.MemoryModelSaver
import tornadofx.*
import java.io.File
import kotlin.math.ceil

class MemoryView(
    private val memoryModel: MemoryModel,
    private val cellsPerRow: Int = 16,
) : View("Memory") {
    private val rowsCount = ceil(memoryModel.addressRange.count() / cellsPerRow.toDouble()).toInt()

    private val fileFilters = arrayOf(FileChooser.ExtensionFilter("Neumann Emulator file", "*.neumann"))

    override val root = hardwareItemView(
        memoryModel, "Memory",
        "address range: ${memoryModel.addressRange.first}..${memoryModel.addressRange.last}",
        listOf(
            "Save" to {
                val files = chooseFile("Save to", filters = fileFilters,
                    mode = FileChooserMode.Save, owner = currentWindow)
                check(files.size <= 1)
                if (files.size == 1) {
                    var file = files[0]
                    if (!file.name.endsWith(".neumann")) {
                        file = File(file.path + ".neumann")
                    }
                    try {
                        MemoryModelSaver.save(memoryModel, file)
                        alert(Alert.AlertType.INFORMATION, "File saved successfully.")
                    } catch (ex: Exception) {
                        alert(Alert.AlertType.ERROR, "Error while saving file: $ex")
                    }
                }
            },
            "Load" to {
                val files = chooseFile("Open", fileFilters, mode = FileChooserMode.Single, owner = currentWindow)
                check(files.size <= 1)
                if (files.size == 1) {
                    val file = files[0]
                    try {
                        memoryModel.reset()
                        MemoryModelLoader.load(memoryModel, file)
                        alert(Alert.AlertType.INFORMATION, "File loaded successfully.")
                    } catch (ex: Exception) {
                        alert(Alert.AlertType.ERROR, "Error while loading file: $ex")
                    }
                }
            },
        )
    ) {
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
        memCellTextField(memoryModel.getRequiredMemoryCellByAddress(address)) {
            tooltip("Address: " + intToHexString(address, MemoryCellModel.Type.ADDRESS_CELL.bytesCount))
        }
    }
}