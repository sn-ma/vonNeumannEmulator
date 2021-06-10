package snma.neumann.gui

import com.github.thomasnield.rxkotlinfx.toObservableChanges
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleIntegerProperty
import javafx.event.EventHandler
import javafx.event.EventTarget
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.paint.Color
import javafx.util.StringConverter
import javafx.util.converter.NumberStringConverter
import snma.neumann.model.MemoryCell
import tornadofx.*
import kotlin.reflect.KProperty1

object GuiUtils {
    private fun intToHexString(intVal: Int?, bytesCount: Int): String? {
        if (intVal == null) return null
        var answ = intVal.toString(16).uppercase()
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

    fun hexStringToInt(str: String?): Int? =
        str?.replace(" ", "")?.replace("""^0+""".toRegex(), "")?.toIntOrNull(16)

    fun<M> EventTarget.positiveIntTextField(model: M, propertyExtractor: KProperty1<M, SimpleIntegerProperty>, op: (TextField.() -> Unit)?): TextField {
        val numberStringConverter: StringConverter<Number> = NumberStringConverter()
        val viewModel = IntViewModel(model, propertyExtractor, numberStringConverter)
        return textfield(viewModel.stringProperty) {
            filterInput { it.controlNewText.isInt() && !it.controlNewText.startsWith('0') }

            onKeyPressed = EventHandler { event ->
                if (event.code == KeyCode.ESCAPE) {
                    viewModel.rollback()
                    parent.requestFocus()
                }
            }
            action {
                parent.requestFocus()
            } // On press any Enter key
            focusedProperty().addListener(ChangeListener { _, _, focused -> if(!focused) viewModel.commit() })
            validator { str ->
                val intVal = str?.toIntOrNull()
                if (intVal == null || intVal <= 0) {
                    error("Please enter the positive integer")
                } else {
                    if (str.startsWith('0')) {
                        propertyExtractor(model).set(intVal)
                    }
                    null
                }
            }

            op?.invoke(this)
        }
    }

    // FIXME get rid of code duplication
    fun EventTarget.memCellTextField(model: MemoryCell, op: (TextField.() -> Unit)? = null): TextField {
        val bytesCount = model.bytesCount
        val numStringConverter = object : StringConverter<Number>() {
            override fun toString(number: Number?): String? = intToHexString(number?.toInt(), bytesCount)
            override fun fromString(string: String?): Number? = hexStringToInt(string)
        }
        val viewModel = IntViewModel(model, MemoryCell::valueProperty, numStringConverter)
        return textfield(viewModel.stringProperty).apply {
            filterInput { it.text.uppercase().all{ ch -> ch in '0'..'9' || ch in 'A'..'F' } }
            model.wasRecentlyModifiedProperty.toObservableChanges().subscribe {
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
            action {
                parent.requestFocus()
            } // On press any Enter key
            focusedProperty().addListener(ChangeListener { _, _, focused -> if(!focused) viewModel.commit() })
            validator { str ->
                if (hexStringToInt(str) == null) {
                    error("Please enter the correct HEX number")
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
    propertyExtractor: KProperty1<M, SimpleIntegerProperty>,
    numberStringConverter: StringConverter<Number>
) : ItemViewModel<M>(model) {
    val intProperty = bind(propertyExtractor) // We must use this binding for rollback to work

    val stringProperty by lazy { // but this binding is obvious for value transformation
        BindingAwareSimpleStringProperty(this, intProperty.name).also { sp ->
            Bindings.bindBidirectional(sp, intProperty, numberStringConverter)
        }
    }
}