package snma.neumann.gui.swing

import com.formdev.flatlaf.FlatDarkLaf
import net.miginfocom.swing.MigLayout
import org.slf4j.bridge.SLF4JBridgeHandler
import snma.neumann.gui.basics.AppViewStateModel
import snma.neumann.gui.swing.MySwingTools.bindIsEnabled
import snma.neumann.gui.swing.MySwingTools.bindText
import snma.neumann.gui.swing.MySwingTools.setIcon
import java.awt.Dimension
import javax.swing.*

fun main() {
    // Set up logging bridge
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()

    val appViewStateModel = AppViewStateModel(MySwingTools.scheduler)

    SwingUtilities.invokeLater { // because some widgets are updating only on Swing thread

        FlatDarkLaf.setup()

        JFrame("von Neumann machine emulator").apply {
            minimumSize = Dimension(600, 300)
            defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
            setIcon()

            layout = MigLayout("flowy, insets 0, fillx")
            val isNotRunning = appViewStateModel.isRunningBehaviorSubject.map { !it }

            add(JPanel(MigLayout("fillx", "[][grow][][][grow][][][][grow][]")).apply {
                add(JButton("Reset").apply {
                    bindIsEnabled(isNotRunning)
                    addActionListener { appViewStateModel.reset() }
                })

                add(JButton("Step").apply {
                    bindIsEnabled(isNotRunning)
                    addActionListener { appViewStateModel.step() }
                }, "skip 1")
                val tickLengthInput = MySwingTools.createPositiveIntegerTextField(
                    appViewStateModel.tickPeriodBehaviorSubject,
                    isNotRunning
                )
                add(JButton().apply {
                    bindText(appViewStateModel.isRunningBehaviorSubject.map { if (it) "Stop" else "Run" })
                    addActionListener {
                        if (tickLengthInput.isEditValid) {
                            appViewStateModel.isRunning = !appViewStateModel.isRunning
                        }
                    }
                })

                add(JLabel("Tick: "), "skip 1")
                add(tickLengthInput)
                add(JLabel("ms"))
                add(JButton("Help").apply { addActionListener { HelpWindowFactory.show() } }, "skip 1")
            }, "grow")

            add(JPanel(MigLayout("flowy, gapx 20, wrap 4")).apply {
                for (factory in listOf(
                    BusViewFactory(appViewStateModel.simulation.busModel),
                    CpuViewFactory(appViewStateModel.simulation.cpuModel),
                    MemoryViewFactory(appViewStateModel.simulation.memoryModel),
                )) {
                    val title = factory.createTitle()
                    val subtitle = factory.createSubtitle()
                    val buttons = factory.createButtonsPane()
                    val body = factory.createBody()

                    add(title)
                    if (subtitle != null) {
                        add(subtitle)
                        add(buttons)
                    } else {
                        add(buttons, "skip 1")
                    }
                    add(body, "top")
                }
            })

            pack()
            isVisible = true
        }
    }
}