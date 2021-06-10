package snma.neumann.gui

import com.github.thomasnield.rxkotlinfx.observeOnFx
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.ReadOnlyBooleanWrapper
import javafx.beans.property.SimpleBooleanProperty
import tornadofx.*
import javafx.beans.property.SimpleIntegerProperty
import snma.neumann.Defaults
import snma.neumann.model.Simulation
import java.util.concurrent.TimeUnit
import kotlin.contracts.contract

class AppStateModel {
    val simulation = Simulation()

    val tickPeriodProperty = SimpleIntegerProperty(Defaults.tickPeriod)
    var tickPeriod by tickPeriodProperty

    val isRunningProperty = SimpleBooleanProperty(false).apply {
        onChange { shouldRun -> if (shouldRun) start() else stop() }
    }
    var isRunning by isRunningProperty

    private var subscription: Disposable? = null // Should be always not null, when isRunning, and null when not

    fun makeStep() {
        if (isRunning) {
            kotlin.error("Trying to make step when already running")
        }
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
}