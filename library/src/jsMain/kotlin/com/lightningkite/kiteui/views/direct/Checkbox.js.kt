package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.ViewDsl
import ViewWriter
import org.w3c.dom.HTMLInputElement

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NCheckbox = HTMLInputElement

@ViewDsl
actual inline fun ViewWriter.checkboxActual(crossinline setup: Checkbox.() -> Unit): Unit {
    transitionNextView = ViewWriter.TransitionNextView.Yes
    themedElementClickable<HTMLInputElement>("input") {
        this.type = "checkbox"
        classList.add("checkbox")
        classList.add("checkResponsive")
        setup(Checkbox(this))
    }
}

actual inline var Checkbox.enabled: Boolean
    get() = !native.disabled
    set(value) {
        native.disabled = !value
    }
actual val Checkbox.checked: Writable<Boolean> get() = native.vprop("input", { checked }, { checked = it })