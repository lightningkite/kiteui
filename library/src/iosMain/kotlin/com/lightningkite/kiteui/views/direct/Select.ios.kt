package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.reactive.reactiveScope
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIPickerView
import platform.UIKit.UIPickerViewDataSourceProtocol
import platform.UIKit.UIPickerViewDelegateProtocol
import platform.UIKit.UIView
import platform.darwin.NSInteger
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
actual class Select actual constructor(context: RContext): RView(context) {
    override val native = WrapperView()
    val textField = TextFieldInput(this)
    init {
        native.addSubview(textField)
        textField.inputView = UIPickerView()
    }

    actual fun <T> bind(
        edits: Writable<T>,
        data: Readable<List<T>>,
        render: (T) -> String
    ) {
        val picker = (textField.inputView as UIPickerView)
        val source = object: NSObject(), UIPickerViewDataSourceProtocol, UIPickerViewDelegateProtocol {
            var list: List<T> = listOf()

            init {
                reactiveScope {
                    list = data.await()
                    picker.reloadAllComponents()
                }
                reactiveScope { textField.text = render(edits.await()) }
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

    override fun applyForeground(theme: Theme) {
        super.applyForeground(theme)
//        textField.apply(theme)
    }

    actual var enabled: Boolean
        get() = textField.enabled
        set(value) {
            textField.enabled = value
            refreshTheming()
        }
    init {
        onRemove(textField.observe("highlighted", { refreshTheming() }))
        onRemove(textField.observe("selected", { refreshTheming() }))
        onRemove(textField.observe("enabled", { refreshTheming() }))
    }
    override fun hasAlternateBackedStates(): Boolean = true
    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if(!enabled) t = t[DisabledSemantic]
        if(textField.highlighted) t = t[DownSemantic]
        if(textField.focused) t = t[FocusSemantic]
        return t
    }
}
