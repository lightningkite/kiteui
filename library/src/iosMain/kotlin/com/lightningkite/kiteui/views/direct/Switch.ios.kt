package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.ReadableState
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.ViewDsl
import ViewWriter
import com.lightningkite.kiteui.views.handleTheme
import platform.UIKit.UIControlEventValueChanged
import platform.UIKit.UISwitch

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NSwitch = UISwitch

@ViewDsl
actual inline fun ViewWriter.switchActual(crossinline setup: Switch.() -> Unit): Unit = element(UISwitch()) {
    handleTheme(
        this,
        foreground = {

        },
    ) {
        setup(Switch(this))
    }
}

actual inline var Switch.enabled: Boolean
    get() = native.enabled
    set(value) {
        native.enabled = value
    }
actual val Switch.checked: Writable<Boolean>
    get() {
        return object : Writable<Boolean> {
            override val state: ReadableState<Boolean> get() = ReadableState(native.on)
            override fun addListener(listener: () -> Unit): () -> Unit {
                return native.onEvent(UIControlEventValueChanged) { listener() }
            }

            override suspend fun set(value: Boolean) {
                native.on = value
            }
        }
    }