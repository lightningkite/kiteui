package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.launchGlobal
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.NView2
import com.lightningkite.kiteui.views.ViewWriter
import kotlinx.browser.window
import org.w3c.dom.HTMLInputElement

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual class NTextField(override val js: HTMLInputElement): NView2<HTMLInputElement>()

@ViewDsl
actual inline fun ViewWriter.textFieldActual(crossinline setup: TextField.() -> Unit): Unit =
    themedElementEditable("input", ::NTextField) {
        setup(TextField(this))
    }

actual val TextField.content: Writable<String> get() = native.js.vprop("input", { value }, { value = it })
actual inline var TextField.keyboardHints: KeyboardHints
    get() = TODO()
    set(value) {
        native.js.type = when (value.type) {
            KeyboardType.Text -> "text"
            KeyboardType.Decimal -> "number"
            KeyboardType.Integer -> "number"
            KeyboardType.Phone -> "tel"
            KeyboardType.Email -> "text"
        }
        native.js.inputMode = when (value.type) {
            KeyboardType.Text -> "text"
            KeyboardType.Decimal -> "decimal"
            KeyboardType.Integer -> "numeric"
            KeyboardType.Phone -> "tel"
            KeyboardType.Email -> "email"
        }

        when (value.autocomplete) {
            AutoComplete.Email -> {
                native.js.type = "email"
                native.js.autocomplete = "email"
            }

            AutoComplete.Password -> {
                native.js.type = "password"
                native.js.autocomplete = "current-password"
            }

            AutoComplete.NewPassword -> {
                native.js.type = "password"
                native.js.autocomplete = "new-password"
            }

            AutoComplete.Phone -> {
                native.js.autocomplete = "tel"
            }

            null -> {
                native.js.autocomplete = "off"
            }
        }
    }
actual var TextField.action: Action?
    get() = TODO()
    set(value) {
        native.js.onkeyup = if (value == null) null else { ev ->
            if (ev.keyCode == 13) {
                launchGlobal {
                    value.onSelect()
                }
            }
        }
    }
actual inline var TextField.hint: String
    get() = native.js.placeholder
    set(value) {
        native.js.placeholder = value
    }
actual inline var TextField.align: Align
    get() = when (window.getComputedStyle(native.js).textAlign) {
        "start" -> Align.Start
        "center" -> Align.Center
        "end" -> Align.End
        "justify" -> Align.Stretch
        else -> Align.Start
    }
    set(value) {
        native.js.style.textAlign = when (value) {
            Align.Start -> "start"
            Align.Center -> "center"
            Align.End -> "end"
            Align.Stretch -> "justify"
        }
    }
actual inline var TextField.textSize: Dimension
    get() = Dimension(window.getComputedStyle(native.js).fontSize)
    set(value) {
        native.js.style.fontSize = value.value
    }