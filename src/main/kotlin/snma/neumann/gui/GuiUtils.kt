package snma.neumann.gui

import com.github.thomasnield.rxkotlinfx.toObservableChanges
import javafx.beans.property.SimpleIntegerProperty
import javafx.event.EventHandler
import javafx.event.EventTarget
import javafx.scene.control.TextField
import javafx.scene.control.TextFormatter
import javafx.scene.input.KeyCode
import javafx.scene.paint.Color
import javafx.util.StringConverter
import snma.neumann.model.MemoryCell
import tornadofx.*
import kotlin.reflect.KProperty1

object GuiUtils {
    private fun createIntTextFormatter(initialValue: Int) = TextFormatter(object: StringConverter<Int>() {
        override fun toString(int: Int?) = int?.toString()
        override fun fromString(str: String?) = str?.toIntOrNull()
    }, initialValue, CustomTextFilter { it.controlNewText.isInt() })

    private fun createMemCellTextFormatter(initialValue: Int, bytesCount: Int) = TextFormatter(object: StringConverter<Int>() {
        override fun toString(int: Int?): String? {
            int ?: return null
            var answ = int.toString(16).uppercase()
            val zeroesToAdd = 2 * bytesCount - answ.length
            if (zeroesToAdd > 0) {
                answ = "0".repeat(zeroesToAdd) + answ
            }
            val charIterator = answ.iterator()
            return buildString {
                while (true) {
                    append(charIterator.nextChar())
                    append(charIterator.nextChar())
                    if (!charIterator.hasNext()) {
                        break
                    } else {
                        append(' ')
                    }
                }
            }
        }
        override fun fromString(str: String?) =
            str?.replace(" ", "")?.replace("""^0+""".toRegex(), "")?.toInt(16)
    }, initialValue, CustomTextFilter { it.text.uppercase().all{ ch -> ch in '0'..'9' || ch in 'A'..'F' }})

    fun<M> EventTarget.positiveIntTextField(model: M, propertyExtractor: KProperty1<M, SimpleIntegerProperty>, op: (TextField.() -> Unit)?): TextField {
        val viewModel = IntViewModel(model, propertyExtractor)
        return textfield(viewModel.intProperty) {
            textFormatter = createIntTextFormatter(viewModel.intProperty.value)
            onKeyPressed = EventHandler { event ->
                if (event.code == KeyCode.ESCAPE) {
                    viewModel.rollback()
                    parent.requestFocus()
                }
            }
            action { parent.requestFocus() } // On press any Enter key
            focusedProperty().addListener(ChangeListener { _, _, focused -> if(!focused) viewModel.commit() })
            validator { str ->
                val intVal = str?.toIntOrNull()
                if (intVal == null || intVal <= 0) {
                    error("Please enter the positive integer")
                } else {
                    null
                }
            }

            op?.invoke(this)
        }
    }

    // FIXME get rid of code duplication
    fun EventTarget.memCellTextField(model: MemoryCell, op: (TextField.() -> Unit)? = null): TextField {
        val bytesCount = model.bytesCount
        val viewModel = MemoryCellViewModel(model)
        return textfield(viewModel.intProperty).apply {
            textFormatter = createMemCellTextFormatter(viewModel.intProperty.value, bytesCount)
            model.wasRecentlyModifiedProperty.toObservableChanges().subscribe {
//            viewModel.wasRecentlyModifiedProperty.toObservableChanges().subscribe { // FIXME
                if (it.newVal) {
                    style {
                        backgroundColor += Color.DARKORANGE
                    }
                } else {
                    style {
                        backgroundColor.elements.remove(Color.DARKORANGE) // FIXME see output
                    }
                }
            }

            onKeyPressed = EventHandler { event ->
                if (event.code == KeyCode.ESCAPE) {
                    viewModel.rollback()
                    parent.requestFocus()
                }
            }
            action { parent.requestFocus() } // On press any Enter key
            focusedProperty().addListener(ChangeListener { _, _, focused -> if(!focused) viewModel.commit() })
            validator { str ->
                if (str == null || str.replace(" ", "").toIntOrNull(16) == null) {
                    error("Please enter the correct HEX digit")
                } else {
                    null
                }
            }

            op?.invoke(this)
        }
    }
}

private open class IntViewModel<M>(
    model: M,
    propertyExtractor: KProperty1<M, SimpleIntegerProperty>
) : ItemViewModel<M>(model) {
//    val intProperty = bind(propertyExtractor)
    val intProperty by lazy {
        val upperProperty = propertyExtractor(model)
        BindingAwareSimpleIntegerProperty(this, upperProperty.name).apply { this.bindBidirectional(upperProperty) }
    }
//    val intProperty = propertyExtractor(model).stringBinding { it?.toInt()?.toString(16) }
}

private class MemoryCellViewModel( // FIXME mb this class could be removed
    model: MemoryCell,
) : IntViewModel<MemoryCell>(model, MemoryCell::valueProperty) {
    val wasRecentlyModifiedProperty = bind { model.wasRecentlyModified.toProperty() }
}