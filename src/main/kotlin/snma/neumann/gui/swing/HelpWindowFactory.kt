package snma.neumann.gui.swing

import snma.neumann.gui.swing.MySwingTools.setIcon
import snma.neumann.help.HelpGenerator
import javax.swing.JEditorPane
import javax.swing.JFrame
import javax.swing.JScrollPane

object HelpWindowFactory {
    private val frame by lazy {
        JFrame("Help").apply {
            setIcon()
            add(JScrollPane(JEditorPane().apply {
                contentType = "text/html"
                isEditable = false
                text = buildString {
                    append("<!doctype html>")
                    append("<html>")
                    append("<style>")
                    append("th, td {")
                    append("  border: 1px solid;")
                    append("  border-spacing: 0px;")
                    append("  padding: 5px;")
                    append("}")
                    append("</style>")
                    for (helpSubject in HelpGenerator.generate()) {
                        append("<h1>${helpSubject.title}</h1>")
                        when (helpSubject) {
                            is HelpGenerator.HelpArticle -> append(helpSubject.content.replace("\n", "<br/>"))
                            is HelpGenerator.HelpEnumTable<*> -> {
                                append("<table cellspacing=\"0\">")
                                append("<tr>")
                                for ((title, _) in helpSubject.columnExtractors) {
                                    append("<th>$title</th>")
                                }
                                append("</tr>")
                                for (rowSequence in helpSubject.extractAllColumnContents()) {
                                    append("<tr>")
                                    for (str in rowSequence) {
                                        append("<td>$str</td>")
                                    }
                                    append("</tr>")
                                }
                                append("</table>")
                            }
                        }
                    }
                    append("</html>")
                }
            }, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED))
        }
    }

    fun show() {
        frame.pack()
        frame.isVisible = true
    }
}