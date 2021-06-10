package snma.neumann.gui

import javafx.geometry.Pos
import snma.neumann.gui.GuiUtils.positiveIntTextField
import tornadofx.*

class AppMenu: View() {
    private val appStateModel = (app as MyApp).appStateModel

    override val root = hbox {
        alignment = Pos.CENTER
        spacing = 5.0

        // TODO: add tooltips

        button("Step") {
            enableWhen(appStateModel.isRunningProperty.not())
            action { appStateModel.makeStep() }
        }
        button("Run/Pause") {
            action { appStateModel.isRunning = !appStateModel.isRunning }
        }
//        button("Reset")
        spacer { minWidth = 10.0 }

        label("Tick: ")
        positiveIntTextField((app as MyApp).appStateModel, AppStateModel::tickPeriodProperty) {
            maxWidth = 70.0
        }
        label("ms")
    }
}

private class TickPeriodViewModel(stateModel: AppStateModel): ItemViewModel<AppStateModel>(stateModel) {
    val tickPeriod = bind(AppStateModel::tickPeriodProperty)
}