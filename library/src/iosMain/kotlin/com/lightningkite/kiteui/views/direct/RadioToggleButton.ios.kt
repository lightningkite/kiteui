package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.views.ViewDsl


@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NRadioToggleButton = FrameLayoutToggleButton

@ViewDsl
actual inline fun ViewWriter.radioToggleButtonActual(crossinline setup: RadioToggleButton.() -> Unit): Unit =
    element(FrameLayoutToggleButton()) {
        handleThemeControl(this, { checkedWritable.await() }) {
            allowUnselect = false
            setup(RadioToggleButton(this))
        }
    }

actual inline var RadioToggleButton.enabled: Boolean
    get() = native.enabled
    set(value) {
        native.enabled = value
    }
actual val RadioToggleButton.checked: Writable<Boolean> get() = native.checkedWritable