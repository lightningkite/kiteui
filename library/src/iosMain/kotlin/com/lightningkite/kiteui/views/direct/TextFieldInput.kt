@file:OptIn(ExperimentalForeignApi::class)

package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.launch
import com.lightningkite.kiteui.models.Action
import com.lightningkite.kiteui.reactive.CalculationContext
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.UIKit.*
import platform.objc.sel_registerName
import com.lightningkite.kiteui.reactive.Property

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class TextFieldInput(val calculationContext: CalculationContext): UITextField(CGRectZero.readValue()) {

    val toolbar = UIToolbar().apply {
        barStyle = UIBarStyleDefault
        setTranslucent(true)
        sizeToFit()
        setItems(listOf(
            UIBarButtonItem(barButtonSystemItem = UIBarButtonSystemItem.UIBarButtonSystemItemFlexibleSpace, target = null, action = null),
            UIBarButtonItem(title = "Done", style = UIBarButtonItemStyle.UIBarButtonItemStylePlain, target = this@TextFieldInput, action = sel_registerName("done")),
        ), animated = false)
    }
    init {
        inputAccessoryView = toolbar
        onEvent(calculationContext, UIControlEventTouchUpInside) {
            becomeFirstResponder()
        }
    }
    @ObjCAction
    fun done() {
        resignFirstResponder()
        calculationContext.launch { action?.onSelect?.invoke() }
    }

    var currentValue: Property<*>? = null
    var valueRange: ClosedRange<*>? = null
    var action: Action? = null
        set(value) {
            field = value
            toolbar.setItems(listOf(
                UIBarButtonItem(barButtonSystemItem = UIBarButtonSystemItem.UIBarButtonSystemItemFlexibleSpace, target = null, action = null),
                UIBarButtonItem(title = value?.title ?: "Done", style = UIBarButtonItemStyle.UIBarButtonItemStylePlain, target = this@TextFieldInput, action = sel_registerName("done")),
            ), animated = false)
        }

    init {
        setUserInteractionEnabled(true)
    }

    override fun caretRectForPosition(position: UITextPosition): CValue<CGRect> = CGRectMake(0.0, 0.0, 0.0, 0.0)
}
