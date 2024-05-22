package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.views.ViewDsl


@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NToggleButton = FrameLayoutToggleButton

@ViewDsl
actual inline fun ViewWriter.toggleButtonActual(crossinline setup: ToggleButton.() -> Unit): Unit = element(FrameLayoutToggleButton()) {
    handleThemeControl(this, { checkedWritable.await() }) {
        setup(ToggleButton(this))
    }
}

actual inline var ToggleButton.enabled: Boolean
    get() = native.enabled
    set(value) {
        native.enabled = value
    }
actual val ToggleButton.checked: Writable<Boolean> get() = native.checkedWritable