package snma.neumann.gui

import javafx.event.EventHandler
import javafx.scene.image.Image
import javafx.stage.Stage
import jfxtras.styles.jmetro.JMetro
import jfxtras.styles.jmetro.JMetroStyleClass
import jfxtras.styles.jmetro.Style
import tornadofx.*


class MyRootView: View() {
    override val root = borderpane {
        title = "von Neumann VM"
        styleClass.add(JMetroStyleClass.BACKGROUND)

        top<AppMenu>()
        center<SimulationView>()

        onMouseClicked = EventHandler { requestFocus() }
    }
}

class SimulationView: View() {
    override val root = hbox {
        add(CpuView((app as MyApp).appStateModel.simulation.cpuModel))
    }
}

class MyApp : App(MyRootView::class) {
    val appStateModel = AppStateModel()

    override fun shouldShowPrimaryStage() = false // Hack to "fix" the bug

    override fun start(stage: Stage) {
        super.start(stage)

        FX.primaryStage.icons += Image("img/icon.png")

        stage.minWidth = 640.0
        stage.minHeight = 480.0

        JMetro(stage.scene, Style.DARK)

        stage.show()
    }
}

fun main() {
    launch<MyApp>()
}