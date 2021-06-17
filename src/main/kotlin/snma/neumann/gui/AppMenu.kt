package snma.neumann.gui

import javafx.geometry.Pos
import jfxtras.styles.jmetro.JMetro
import jfxtras.styles.jmetro.Style
import snma.neumann.Constants
import snma.neumann.gui.GuiUtils.positiveIntTextField
import tornadofx.*

class AppMenu: View() {
    private val appStateModel = (app as MyApp).appStateModel

    private val helpView by lazy { HelpView() }

    override val root = hbox {
        alignment = Pos.CENTER
        spacing = Constants.View.BUTTONS_SPACING

        // TODO: add tooltips

        button("Reset") {
            enableWhen(appStateModel.isRunningProperty.not())
            action { appStateModel.reset() }
        }
        spacer { minWidth = 10.0 }

        button("Step") {
            enableWhen(appStateModel.isRunningProperty.not())
            action { appStateModel.makeStep() }
        }
        button("Run") {
            action { appStateModel.isRunning = !appStateModel.isRunning }
            appStateModel.isRunningProperty.addListener { _, _, isRunningValue ->
                text = if (isRunningValue) "Stop" else "Run"
            }
        }
        spacer { minWidth = 10.0 }

        label("Tick: ")
        positiveIntTextField((app as MyApp).appStateModel, AppStateModel::tickPeriodProperty) {
            maxWidth = 70.0
            enableWhen(appStateModel.isRunningProperty.not())
        }
        label("ms")
        spacer { minWidth = 10.0 }

        button("Help").action { JMetro(helpView.openWindow()?.scene, Style.DARK) }
    }
}