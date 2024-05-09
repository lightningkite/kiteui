package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.launchManualCancel
import com.lightningkite.kiteui.views.ViewDsl
import ViewWriter
import com.lightningkite.kiteui.views.calculationContext
import org.w3c.dom.HTMLButtonElement

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NButton = HTMLButtonElement

@ViewDsl
actual inline fun ViewWriter.buttonActual(crossinline setup: Button.() -> Unit): Unit =
    themedElementClickable<NButton>("button") {
        classList.add("kiteui-stack")
        setup(Button(this))
    }

actual fun Button.onClick(action: suspend () -> Unit): Unit {
    var virtualDisable: Boolean = false
    native.onclick = {
        if(!virtualDisable) {
            native.calculationContext.launchManualCancel {
                try {
                    virtualDisable = true
                    action()
                } finally {
                    virtualDisable = false
                }
            }
        }
    }
}

actual inline var Button.enabled: Boolean
    get() = !native.disabled
    set(value) {
        native.disabled = !value
    }