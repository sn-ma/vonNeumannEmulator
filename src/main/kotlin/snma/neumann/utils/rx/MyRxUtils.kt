package snma.neumann.utils.rx

import io.reactivex.rxjava3.subjects.BehaviorSubject
import kotlin.reflect.KProperty

operator fun<T: Any> BehaviorSubject<T>.getValue(thisRef: Any, property: KProperty<*>): T =
    this.value ?: error("No default value for ${property.name} was set")

operator fun<T: Any> BehaviorSubject<T>.setValue(thisRef: Any, property: KProperty<*>, value: T) {
    onNext(value)
}