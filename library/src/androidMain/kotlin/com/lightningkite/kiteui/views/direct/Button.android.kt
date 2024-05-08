package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.launchManualCancel
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.views.*

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NButton = SlightlyModifiedFrameLayout

@ViewDsl
actual inline fun ViewWriter.buttonActual(crossinline setup: Button.() -> Unit) {
    viewElement(factory = ::SlightlyModifiedFrameLayout, wrapper = ::ContainingView) {
        val frame = native as SlightlyModifiedFrameLayout
        native.isClickable = true
        val l = native.androidCalculationContext.loading
        handleThemeControl(frame) {
            setup(Button(frame))
            activityIndicator {
                ::exists.invoke { l.await() }
                native.minimumWidth = 0
                native.minimumHeight = 0
            }
        }
    }
}

actual fun Button.onClick(action: suspend () -> Unit) {
    native.setOnClickListener { view ->
        if (enabled) {
            view.calculationContext.launchManualCancel {
                enabled = false
                try { action() } finally { enabled = true }
            }
        }
    }
}

actual var Button.enabled: Boolean
    get() {
        return native.isEnabled
    }
    set(value) {
        native.isEnabled = value
        native.maybeCalculationContext?.enabledListeners?.value = value
    }
