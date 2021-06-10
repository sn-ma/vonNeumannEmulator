package snma.neumann.model

import com.github.thomasnield.rxkotlinfx.toBinding
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import javafx.application.Platform
import javafx.beans.property.IntegerProperty
import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.ReadOnlyBooleanWrapper
import javafx.beans.property.SimpleIntegerProperty
import kotlin.math.ceil

import tornadofx.*


class MemoryCell(val bitsCount: Int) {
    val bytesCount: Int
        get() = ceil(bitsCount / 8.0).toInt()

    private val bitmask: Int = (-1 shl bitsCount).inv()

    val valueProperty = object : SimpleIntegerProperty(0) {
        override fun set(newValue: Int) {
            super.set(newValue and bitmask)
            Platform.runLater { wasRecentlyModifiedPropertyRW.set(true) } // FIXME do we need this delay?
        }
    }
    var value by valueProperty

    private val wasRecentlyModifiedPropertyRW = ReadOnlyBooleanWrapper(false)
    val wasRecentlyModifiedProperty: ReadOnlyBooleanProperty = wasRecentlyModifiedPropertyRW.readOnlyProperty
    val wasRecentlyModified get() = wasRecentlyModifiedProperty.get()

    fun cleanWasRecentlyModified() {
        wasRecentlyModifiedPropertyRW.set(false)
    }
}