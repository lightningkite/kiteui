

package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.WeakReference
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.experimental.ExperimentalNativeApi
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.UIKit.*
import platform.objc.sel_registerName

@OptIn(ExperimentalNativeApi::class)
@InternalKiteUi
public class TextFieldInput(calculationContext: CalculationContext): UITextField(CGRectZero.readValue()) {
    public val calculationContextWeak: WeakReference<CalculationContext> = WeakReference(calculationContext)

    // Explicit frame prevents UnsatisfiableConstraints error when automatic constraints are set by the system
    // https://stackoverflow.com/questions/54284029/uitoolbar-with-uibarbuttonitem-layoutconstraint-issue
    public val toolbar: UIToolbar = UIToolbar(CGRectMake(0.0, 0.0, UIScreen.mainScreen.bounds.useContents { size.width }, 35.0)).apply {
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
    public fun done() {
        resignFirstResponder()
        calculationContextWeak.get()?.let { action?.startAction(it) }
    }

    public var action: Action? = null
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

    public override fun caretRectForPosition(position: UITextPosition): CValue<CGRect> = CGRectMake(0.0, 0.0, 0.0, 0.0)
}
