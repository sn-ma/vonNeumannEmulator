package snma.neumann.gui

import javafx.scene.control.ScrollPane
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.Font
import jfxtras.styles.jmetro.JMetroStyleClass
import snma.neumann.Constants
import snma.neumann.help.HelpGenerator
import tornadofx.*

class HelpView: View("Help") {
    override val root = scrollpane {
        hbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
        vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
        styleClass.add(JMetroStyleClass.BACKGROUND)

        vbox {
            isFitToWidth = true
            paddingAll = 10.0

            val borderTemplate = Border(BorderStroke(Color.WHITE,
                BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY,
                BorderWidths.DEFAULT))

            fun Pane.myCell(text: String) = stackpane {
                label(text) { isWrapText = true }
                paddingVertical = 7.0
                paddingHorizontal = 15.0
                minWidth = 100.0
                border = borderTemplate
            }

            val titleFont = Font("System bold", Constants.View.FONT_SIZE_BIG)

            for (helpSubject in HelpGenerator.generate()) {
                label(helpSubject.title) { font = titleFont }
                when (helpSubject) {
                    is HelpGenerator.HelpArticle -> label(helpSubject.content) { isWrapText = true }
                    is HelpGenerator.HelpEnumTable<*> -> {
                        gridpane {
                            row {
                                for ((title, _) in helpSubject.columnExtractors) {
                                    myCell(title)
                                }
                            }
                            for (rowSequence in helpSubject.extractAllColumnContents()) {
                                row {
                                    for (str in rowSequence) {
                                        myCell(str)
                                    }
                                }
                            }
                        }
                    }
                }
                text("\n")
            }
        }
    }
}
