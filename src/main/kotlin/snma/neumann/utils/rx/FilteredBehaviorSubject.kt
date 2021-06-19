package snma.neumann.utils.rx

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableSource
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import kotlin.reflect.KProperty

class FilteredBehaviorSubject<T: Any>(defaultValue: T, private val filter: ((T) -> T)?): ObservableSource<T>, Observer<T> {
    private val backedBehaviorSubject: BehaviorSubject<T> = BehaviorSubject.createDefault(defaultValue)

    var value: T
        get() = backedBehaviorSubject.value!!
        set(value) { onNext(value) }

    val observable: Observable<T> get() = backedBehaviorSubject

    override fun subscribe(observer: Observer<in T>) {
        backedBehaviorSubject.subscribe(observer)
    }

    override fun onSubscribe(d: Disposable) {
        backedBehaviorSubject.onSubscribe(d)
    }

    override fun onNext(value: T) {
        backedBehaviorSubject.onNext(filter?.let { it(value) } ?: value)
    }

    override fun onError(e: Throwable) {
        backedBehaviorSubject.onError(e)
    }

    override fun onComplete() {
        backedBehaviorSubject.onComplete()
    }

    operator fun getValue(thisRef: Any, property: KProperty<*>): T = value

    operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        onNext(value)
    }
}