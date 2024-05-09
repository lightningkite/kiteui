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
actual typealias NRadioToggleButton = HTMLSpanElement

@ViewDsl
actual inline fun ViewWriter.radioToggleButtonActual(crossinline setup: RadioToggleButton.() -> Unit): Unit =
    element<HTMLLabelElement>("label") {
        classList.add("toggle-button")
        lateinit var input: HTMLInputElement
        element<HTMLInputElement>("input") {
            input = this
            this.type = "radio"
            classList.add("checkResponsive")
            this.hidden = true
            this.style.display = "none"
        }
        themedElementClickable<HTMLSpanElement>("span") {
            classList.add("checkResponsive", "kiteui-stack")
            tabIndex = 0
            setup(RadioToggleButton(this))
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

actual inline var RadioToggleButton.enabled: Boolean
    get() = !(this.native.previousElementSibling as HTMLInputElement).disabled
    set(value) {
        (this.native.previousElementSibling as HTMLInputElement).disabled = !value
    }
actual val RadioToggleButton.checked: Writable<Boolean>
    get() {
        return (this.native.previousElementSibling as HTMLInputElement).vprop(
            "input",
            { checked },
            { checked = it })
    }