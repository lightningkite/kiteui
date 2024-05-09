package com.lightningkite.kiteui.views.direct

import ViewWriter
import com.lightningkite.kiteui.launchManualCancel
import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.views.*
import platform.UIKit.UIControlEventTouchUpInside

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NButton = FrameLayoutButton

@ViewDsl
actual inline fun ViewWriter.buttonActual(crossinline setup: Button.() -> Unit): Unit = element(FrameLayoutButton()) {
    val l = iosCalculationContext.loading
    handleThemeControl(this) {
        setup(Button(this))
        activityIndicator {
            ::opacity.invoke { if(l.await()) 1.0 else 0.0 }
            native.extensionSizeConstraints = SizeConstraints(minWidth = null, minHeight = null)
        }
    }
}

actual fun Button.onClick(action: suspend () -> Unit): Unit {
    native.onEvent(UIControlEventTouchUpInside) {
        if (enabled) {
            native.calculationContext.launchManualCancel {
                enabled = false
                try { action() } finally { enabled = true }
            }
        }
    }
}

actual inline var Button.enabled: Boolean
    get() = native.enabled
    set(value) {
        native.enabled = value
    }