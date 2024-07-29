@file:OptIn(ExperimentalForeignApi::class)

package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.WeakReference
import com.lightningkite.kiteui.launch
import com.lightningkite.kiteui.models.Action
import com.lightningkite.kiteui.reactive.CalculationContext
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.UIKit.*
import platform.objc.sel_registerName
import com.lightningkite.kiteui.reactive.Property
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class, ExperimentalNativeApi::class)
class TextFieldInput(calculationContext: CalculationContext): UITextField(CGRectZero.readValue()) {
    val calculationContextWeak = WeakReference(calculationContext)

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
        calculationContextWeak.get()?.launch { action?.onSelect?.invoke() }
    }

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
