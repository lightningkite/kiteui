package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.views.*
import platform.UIKit.UIPickerView
import platform.UIKit.UIPickerViewDataSourceProtocol
import platform.UIKit.UIPickerViewDelegateProtocol
import platform.darwin.NSInteger
import platform.darwin.NSObject

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NSelect = TextFieldInput

@ViewDsl
actual inline fun ViewWriter.selectActual(crossinline setup: Select.() -> Unit): Unit = element(FrameLayout()) {
    handleTheme(this, viewDraws = false,) {
        element(TextFieldInput()) {
            handleTheme(this, foreground = { textColor = it.foreground.closestColor().toUiColor() }) {
                inputView = UIPickerView().apply {
                }
                setup(Select(this))
            }
        }
    }
}

actual fun <T> Select.bind(
    edits: Writable<T>,
    data: Readable<List<T>>,
    render: (T) -> String
) {
    val picker = (native.inputView as UIPickerView)
    val source = object: NSObject(), UIPickerViewDataSourceProtocol, UIPickerViewDelegateProtocol {
        var list: List<T> = listOf()

        init {
            reactiveScope {
                list = data.await()
                picker.reloadAllComponents()
            }
            reactiveScope { native.text = render(edits.await()) }
        }

        override fun numberOfComponentsInPickerView(pickerView: UIPickerView): NSInteger = 1L
        @Suppress("CONFLICTING_OVERLOADS", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")
        override fun pickerView(pickerView: UIPickerView, numberOfRowsInComponent: NSInteger): NSInteger = list.size.toLong()
        @Suppress("CONFLICTING_OVERLOADS", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")
        override fun pickerView(pickerView: UIPickerView, titleForRow: NSInteger, forComponent: NSInteger): String? {
            return render(list[titleForRow.toInt()])
        }
        @Suppress("CONFLICTING_OVERLOADS", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")
        override fun pickerView(pickerView: UIPickerView, didSelectRow: NSInteger, inComponent: NSInteger) {
            launch {
                val item = list[didSelectRow.toInt()]
                edits set item
            }
        }
    }
    picker.setDataSource(source)
    picker.setDelegate(source)
    native.extensionStrongRef = source
}