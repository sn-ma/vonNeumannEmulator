package snma.neumann.gui

import javafx.event.EventHandler
import javafx.scene.image.Image
import javafx.stage.Stage
import jfxtras.styles.jmetro.JMetro
import jfxtras.styles.jmetro.JMetroStyleClass
import jfxtras.styles.jmetro.Style
import org.slf4j.bridge.SLF4JBridgeHandler
import snma.neumann.Constants
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
        spacing = 20.0
        paddingAll = 10.0

        val simulation = (app as MyApp).appStateModel.simulation
        add(BusView(simulation.busModel))
        add(CpuView(simulation.cpuModel))
        add(MemoryView(simulation.memoryModel, Constants.View.MEMORY_CELLS_PER_ROW))
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
    // Set up logging bridge
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()

    launch<MyApp>()
}