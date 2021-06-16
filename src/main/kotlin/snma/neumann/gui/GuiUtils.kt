package snma.neumann.gui

import com.github.thomasnield.rxkotlinfx.toObservableChanges
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.IntegerProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.event.EventHandler
import javafx.event.EventTarget
import javafx.scene.control.Control
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.util.StringConverter
import javafx.util.converter.NumberStringConverter
import snma.neumann.Constants
import snma.neumann.model.AbstractCellModel
import snma.neumann.model.EnumCellModel
import snma.neumann.model.HardwareItem
import snma.neumann.model.MemoryCellModel
import snma.neumann.utils.CommonUtils
import tornadofx.*
import kotlin.reflect.KProperty1

object GuiUtils {
    private fun<M> EventTarget.customFormattedTextField(
        model: M,
        propertyExtractor: KProperty1<M, IntegerProperty>,
        numberStringConverter: StringConverter<Number>,
        op: (TextField.() -> Unit)?,
    ): TextField {
        val viewModel = FormattedIntViewModel(model, propertyExtractor, numberStringConverter)
        return textfield(viewModel.stringProperty) {
            onKeyPressed = EventHandler { event ->
                if (event.code == KeyCode.ESCAPE) {
                    viewModel.rollback()
                    parent.requestFocus()
                }
            }

            action { parent.requestFocus() } // On press any Enter key

            focusedProperty().addListener(ChangeListener { _, _, focused -> if(!focused) {
                viewModel.commit()
                viewModel.fixFormatting()
            } })

            op?.invoke(this)
        }
    }

    fun<M> EventTarget.positiveIntTextField(model: M, propertyExtractor: KProperty1<M, SimpleIntegerProperty>, op: (TextField.() -> Unit)?): TextField {
        val numberStringConverter: StringConverter<Number> = NumberStringConverter()
        return customFormattedTextField(
            model = model,
            propertyExtractor = propertyExtractor,
            numberStringConverter = numberStringConverter,
        ) {
            filterInput { it.controlNewText.isInt() && !it.controlNewText.startsWith('0') }

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

    fun EventTarget.memCellTextField(model: MemoryCellModel, op: (TextField.() -> Unit)? = null): TextField {
        val bytesCount = model.type.bytesCount
        val numberStringConverter = object : StringConverter<Number>() {
            override fun toString(number: Number?): String? = CommonUtils.intToHexString(number?.toInt(), bytesCount)
            override fun fromString(string: String?): Number? = CommonUtils.hexStringToInt(string)
        }
        return customFormattedTextField(
            model = model,
            propertyExtractor = MemoryCellModel::valueProperty,
            numberStringConverter = numberStringConverter,
        ) {
            prefWidth = bytesCount * 20.0 + 30.0
            minWidth = prefWidth
            maxWidth = prefWidth

            filterInput {
                !it.text.isNullOrEmpty() &&
                        it.text.uppercase().all{ ch -> ch in '0'..'9' || ch in 'A'..'F' || ch == ' ' } &&
                        (it.controlNewText.replace(" ", "").length / 2.0) <= bytesCount.toDouble()
            }

            @Suppress("DEPRECATION")
            addRecentlyModifiedStyling(model)

            validator { str ->
                if (CommonUtils.hexStringToInt(str) == null) {
                    error("Please enter the correct HEX number")
                } else {
                    null
                }
            }

            op?.invoke(this)
        }
    }

    private class FormattedIntViewModel<M>(
        model: M,
        propertyExtractor: KProperty1<M, IntegerProperty>,
        private val numberStringConverter: StringConverter<Number>
    ) : ItemViewModel<M>(model) {
        val intProperty = bind(propertyExtractor) // We must use this binding for rollback to work

        val stringProperty by lazy { // but this binding is obvious for value transformation
            BindingAwareSimpleStringProperty(this, intProperty.name).also { sp ->
                Bindings.bindBidirectional(sp, intProperty, numberStringConverter)
            }
        }

        fun fixFormatting() {
            val formattedStringValue = numberStringConverter.toString(intProperty.value)
            if (stringProperty.value != formattedStringValue) {
                Platform.runLater { stringProperty.value = formattedStringValue }
            }
        }
    }

    @Deprecated("Not supposed to be used from outside of the class, but should be made public")
    fun <T : Any> Control.addRecentlyModifiedStyling(cellModel: AbstractCellModel<T>) {
        cellModel.wasRecentlyModifiedProperty.toObservableChanges().subscribe {
            if (it.newVal) {
                check(style == "") { "Style of non-changed memory cell isn't empty! Probably, styling logic has changed" }
                style {
                    backgroundColor += Color.DARKORANGE
                }
            } else {
                style = ""
            }
        }
    }

    inline fun <reified T: Enum<T>> EventTarget.enumCellView(enumCellModel: EnumCellModel<T>): Control {
        return combobox(enumCellModel.valueProperty, enumValues<T>().toList()) {
            @Suppress("DEPRECATION")
            addRecentlyModifiedStyling(enumCellModel)
        }
    }

    fun EventTarget.hardwareItemView(
        hardwareItem: HardwareItem,
        title: String,
        subtitleAppender: (Pane.() -> Unit),
        additionalButtons: List<Pair<String, () -> Unit>>? = null,
        op: (VBox.() -> Unit)
    ) = vbox {
        spacing = 10.0

        textflow {
            text(title) {
                font = Font("System bold", Constants.View.FONT_SIZE_BIG)
            }
            text("\n")
            subtitleAppender()
        }
        hbox {
            spacing = Constants.View.BUTTONS_SPACING
            button("Reset").action { hardwareItem.reset() }
            if (additionalButtons != null) {
                for ((name, action) in additionalButtons) {
                    button(name).action(action)
                }
            }
        }

        op()
    }

    fun EventTarget.hardwareItemView(
        hardwareItem: HardwareItem,
        title: String,
        subtitle: String = "",
        additionalButtons: List<Pair<String, () -> Unit>>? = null,
        op: (VBox.() -> Unit)
    ) = hardwareItemView(
        hardwareItem, title, { text(subtitle) }, additionalButtons, op
    )
}