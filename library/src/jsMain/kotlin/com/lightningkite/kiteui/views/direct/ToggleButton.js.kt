package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.ViewDsl
import ViewWriter
import com.lightningkite.kiteui.views.element
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLLabelElement
import org.w3c.dom.HTMLSpanElement
import org.w3c.dom.events.KeyboardEvent

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NToggleButton = HTMLSpanElement

@ViewDsl
actual inline fun ViewWriter.toggleButtonActual(crossinline setup: ToggleButton.() -> Unit): Unit = element<HTMLLabelElement>("label") {
    classList.add("toggle-button")
    lateinit var input: HTMLInputElement
    element<HTMLInputElement>("input") {
        input = this
        this.type = "checkbox"
        classList.add("checkResponsive")
        this.hidden = true
        this.style.display = "none"
    }
    themedElementClickable<HTMLSpanElement>("span") {
        classList.add("checkResponsive", "kiteui-stack")
        tabIndex = 0
        setup(ToggleButton(this))
        addEventListener("keydown", { ev ->
            ev as KeyboardEvent
            if (ev.key == KeyCodes.space || ev.key == KeyCodes.enter) {
                ev.preventDefault()
            }
        })
        addEventListener("keyup", { ev ->
            ev as KeyboardEvent
            if (ev.key == KeyCodes.space || ev.key == KeyCodes.enter) {
                input.click()
                ev.preventDefault()
            }
        })
    }
}

actual inline var ToggleButton.enabled: Boolean
    get() = !(this.native.previousElementSibling as HTMLInputElement).disabled
    set(value) {
        (this.native.previousElementSibling as HTMLInputElement).disabled = !value
    }
actual val ToggleButton.checked: Writable<Boolean>
    get() = (this.native.previousElementSibling as HTMLInputElement).vprop(
        "input",
        { checked },
        { checked = it })