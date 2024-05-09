package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.ViewDsl
import ViewWriter
import org.w3c.dom.HTMLInputElement

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NSwitch = HTMLInputElement

@ViewDsl
actual inline fun ViewWriter.switchActual(crossinline setup: Switch.() -> Unit): Unit = themedElementClickable<HTMLInputElement>("input") {
    this.type = "checkbox"
    this.classList.add("switch")
    this.classList.add("checkResponsive")
    setup(Switch(this))
}

actual inline var Switch.enabled: Boolean
    get() = !native.disabled
    set(value) {
        native.disabled = !value
    }
actual val Switch.checked: Writable<Boolean> get() = native.vprop("input", { checked }, { checked = it })