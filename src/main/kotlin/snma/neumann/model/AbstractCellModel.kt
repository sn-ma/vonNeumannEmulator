package snma.neumann.model

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import snma.neumann.utils.rx.FilteredBehaviorSubject
import snma.neumann.utils.rx.getValue
import snma.neumann.utils.rx.setValue

abstract class AbstractCellModel<T: Any>(private val defaultValue: T, filter: ((T) -> T)?) {
    val valueBehaviorSubject: FilteredBehaviorSubject<T> = FilteredBehaviorSubject(defaultValue, filter)
    var value: T by valueBehaviorSubject

    private val wasRecentlyModifiedBehaviorSubject: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(false)
    val wasRecentlyModifiedObservable: Observable<Boolean> get() = wasRecentlyModifiedBehaviorSubject
    var wasRecentlyModified: Boolean by wasRecentlyModifiedBehaviorSubject
        private set

    fun cleanWasRecentlyModified() {
        wasRecentlyModified = false
    }

    fun reset() {
        value = defaultValue
        cleanWasRecentlyModified()
    }

    init {
        valueBehaviorSubject.observable.skip(1).subscribe {
            wasRecentlyModified = true
        }
    }
}