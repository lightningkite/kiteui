package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.NView2
import com.lightningkite.kiteui.views.ViewWriter
import org.w3c.dom.HTMLInputElement

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual class NCheckbox(override val js: HTMLInputElement): NView2<HTMLInputElement>()

@ViewDsl
actual inline fun ViewWriter.checkboxActual(crossinline setup: Checkbox.() -> Unit): Unit {
    transitionNextView = ViewWriter.TransitionNextView.Yes
    themedElementClickable("input", ::NCheckbox) {
        js.type = "checkbox"
        js.classList.add("checkbox")
        js.classList.add("checkResponsive")
        setup(Checkbox(this))
    }
}

actual inline var Checkbox.enabled: Boolean
    get() = !native.js.disabled
    set(value) {
        native.js.disabled = !value
    }
actual val Checkbox.checked: Writable<Boolean> get() = native.js.vprop("input", { checked }, { checked = it })