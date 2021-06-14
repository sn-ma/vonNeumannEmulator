package snma.neumann.gui

import com.github.thomasnield.rxkotlinfx.observeOnFx
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import snma.neumann.Defaults
import snma.neumann.model.CpuModel
import snma.neumann.model.Simulation
import tornadofx.getValue
import tornadofx.onChange
import tornadofx.setValue
import java.util.concurrent.TimeUnit

class AppStateModel {
    val simulation = Simulation()

    val tickPeriodProperty = SimpleIntegerProperty(Defaults.tickPeriod)
    var tickPeriod by tickPeriodProperty

    val isRunningProperty = SimpleBooleanProperty(false).apply {
        onChange { shouldRun -> if (shouldRun) start() else stop() }
    }
    var isRunning by isRunningProperty

    private var subscription: Disposable? = null // Should be always not null, when isRunning, and null when not

    init {
        simulation.cpuModel.isStoppedProperty.addListener { _, _, newValue ->
            if (newValue != CpuModel.CommonStopMode.NOT_STOPPED) {
                stop()
            }
        }
    }

    fun makeStep() {
        check (!isRunning) { "Trying to make step when already running" }
        simulation.cpuModel.isStopped = CpuModel.CommonStopMode.NOT_STOPPED
        simulation.tick()
    }

    fun stop() {
        if (subscription != null) {
            isRunning = false
            subscription?.dispose()
            subscription = null
        }
    }

    fun start() {
        simulation.cpuModel.isStopped = CpuModel.CommonStopMode.NOT_STOPPED
        if (subscription == null) {
            isRunning = true
            subscription = Observable.interval(tickPeriod.toLong(), TimeUnit.MILLISECONDS)
                .observeOnFx()
                .subscribe { simulation.tick() }
        }
    }

    fun restart() {
        stop()
        start()
    }

    fun reset() {
        simulation.reset()
    }
}