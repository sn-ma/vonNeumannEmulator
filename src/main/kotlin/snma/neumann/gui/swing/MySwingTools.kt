package snma.neumann.gui.swing

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import snma.neumann.model.AbstractCellModel
import snma.neumann.model.EnumCellModel
import snma.neumann.model.MemoryCellModel
import snma.neumann.utils.CommonUtils
import java.awt.Color
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.text.NumberFormat
import java.text.ParseException
import javax.swing.*
import javax.swing.text.NumberFormatter

object MySwingTools {
    val scheduler: Scheduler = Schedulers.from { SwingUtilities.invokeLater(it) }

    fun createPositiveIntegerTextField(
        behaviorSubject: BehaviorSubject<Int>,
        isEditableObservable: Observable<Boolean>,
    ) : JFormattedTextField {
        val numberFormat = NumberFormat.getIntegerInstance()
        val defaultFormatter = object : NumberFormatter(numberFormat) {
            override fun stringToValue(text: String?): Any? {
                val gotValue = (super.stringToValue(text) as Number? ?: return null).toInt()
                if (gotValue <= 0) {
                    throw ParseException("Positive value required", 0)
                }
                return gotValue
            }
        }
        val textField = JFormattedTextField(defaultFormatter)
        textField.inputVerifier = formattedTextFieldVerifier
        textField.bindBidirectional(behaviorSubject)
        textField.bindIsEnabled(isEditableObservable)
        textField.setupLooseFocus()
        return textField
    }

    private fun JFormattedTextField.setupLooseFocus() {
        fun tryLooseFocus() {
            parent.requestFocus()
        }
        addActionListener { tryLooseFocus() }
        addKeyListener(object : KeyListener {
            override fun keyTyped(event: KeyEvent) {}

            override fun keyPressed(event: KeyEvent) {}

            override fun keyReleased(event: KeyEvent) {
                if (event.keyCode == KeyEvent.VK_ESCAPE) {
                    tryLooseFocus()
                }
            }
        })
    }

    private fun<T: Any> JFormattedTextField.bindBidirectional(behaviorSubject: BehaviorSubject<T>) {
        behaviorSubject.subscribeOn(scheduler).subscribe { valueFromBS ->
            if (formatter.stringToValue(formatter.valueToString(valueFromBS)) == valueFromBS) { // If the value is correct
                if (this.value != valueFromBS) {
                    this.value = valueFromBS
                }
            }
        }
        val wasInitialized = CommonUtils.Holder(false)
        addPropertyChangeListener("value") {
            if (wasInitialized.value) { // Hack to prevent value to be marked as changed on init
                @Suppress("UNCHECKED_CAST")
                val value = this.value as T?
                if (value == null) {
                    this.value = behaviorSubject.value // Rollback the value if it was entered invalid
                } else if (value != behaviorSubject.value) {
                    behaviorSubject.onNext(value)
                }
            } else {
                wasInitialized.value = true
            }
        }
    }

    fun createMemoryCellTextField(memoryCellModel: MemoryCellModel): JFormattedTextField {
        val bytesCount = memoryCellModel.type.bytesCount
        val formatter = object : JFormattedTextField.AbstractFormatter() {
            override fun stringToValue(string: String?): Int? = CommonUtils.hexStringToInt(string, bytesCount)

            override fun valueToString(value: Any?): String? = (value as Int?)?.let { CommonUtils.intToHexString(it, bytesCount) }
        }
        val textField = JFormattedTextField(formatter)
        textField.columns = bytesCount * 3 - 1
        textField.inputVerifier = formattedTextFieldVerifier
        textField.bindBidirectional(memoryCellModel.valueBehaviorSubject)
        textField.bindWasRecentlyModified(memoryCellModel)
        textField.setupLooseFocus()
        return textField
    }

    inline fun <reified T: Enum<T>> createEnumCellComboBox(enumCellModel: EnumCellModel<T>): JComboBox<T> {
        val comboBox = JComboBox(enumValues<T>())
        enumCellModel.valueBehaviorSubject.subscribeOn(scheduler).subscribe { value ->
            comboBox.selectedItem = value
        }
        val isInitialized = CommonUtils.Holder(false)
        comboBox.addActionListener {
            if (isInitialized.value) {
                enumCellModel.value = comboBox.selectedItem as T
            } else {
                isInitialized.value = true
            }
        }
        comboBox.bindWasRecentlyModified(enumCellModel)
        return comboBox
    }

    fun JComponent.bindIsEnabled(observable: Observable<Boolean>) {
        observable.subscribeOn(scheduler).subscribe { isEnabled = it }
    }

    fun AbstractButton.bindText(observable: Observable<String>) {
        observable.subscribeOn(scheduler).subscribe { text = it }
    }

    fun JLabel.bindText(observable: Observable<String>) {
        observable.subscribeOn(scheduler).subscribe { text = it }
    }

    fun JComponent.bindWasRecentlyModified(model: AbstractCellModel<*>) {
        model
            .wasRecentlyModifiedObservable
            .distinctUntilChanged()
            .subscribeOn(scheduler)
            .subscribe { wasRecentlyModified ->
                background = if (wasRecentlyModified) Color(0x3e1900) else null
            }
    }

    private val formattedTextFieldVerifier = object : InputVerifier() {
        override fun verify(component: JComponent?): Boolean {
            val ftf = component as? JFormattedTextField ?: error(
                "formattedTextFieldVerifier is supposed to be used with the JFormattedTextField")
            val formatter = ftf.formatter
            if (formatter != null) {
                val text = ftf.text
                return try {
                    formatter.stringToValue(text)
                    true
                } catch (pe: ParseException) {
                    false
                }
            }
            return true
        }

        override fun shouldYieldFocus(source: JComponent?, target: JComponent?): Boolean {
            return verify(source)
        }
    }

    fun JFrame.setIcon() {
        val imageUrl = ClassLoader.getSystemResource("img/icon.png") ?: error("Can't find image resource")
        iconImage = ImageIcon(imageUrl).image
    }
}