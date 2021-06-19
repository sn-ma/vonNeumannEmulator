package snma.neumann.gui.basics

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import snma.neumann.Constants
import snma.neumann.model.CpuModel
import snma.neumann.model.Simulation
import snma.neumann.utils.rx.getValue
import snma.neumann.utils.rx.setValue
import java.util.concurrent.TimeUnit

class AppViewStateModel(private val scheduler: Scheduler) {
    val simulation = Simulation()

    val tickPeriodBehaviorSubject: BehaviorSubject<Int> = BehaviorSubject.createDefault(Constants.View.TICK_PERIOD_DEFAULT)
    var tickPeriod by tickPeriodBehaviorSubject

    private var subscription: Disposable? = null

    val isRunningBehaviorSubject: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(false).apply {
        distinctUntilChanged()
            .subscribeOn(scheduler)
            .subscribe { newValue ->
                subscription = if (newValue) {
                    Observable.interval(tickPeriod.toLong(), TimeUnit.MILLISECONDS)
                        .observeOn(scheduler)
                        .subscribe { step() }
                } else {
                    subscription?.dispose()
                    null
                }
            }
    }
    var isRunning by isRunningBehaviorSubject

    init {
        simulation.cpuModel.isStoppedObservable.subscribeOn(scheduler).subscribe { newValue ->
            if (newValue != CpuModel.CommonStopMode.NOT_STOPPED) {
                isRunning = false
            }
        }
    }

    fun step() {
        simulation.cpuModel.isStopped = CpuModel.CommonStopMode.NOT_STOPPED
        simulation.tick()
    }

    fun reset() {
        simulation.reset()
    }
}
