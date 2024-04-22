package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.launchGlobal
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.reactive.asDouble
import com.lightningkite.kiteui.utils.numberAutocommaRepair
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import kotlinx.browser.window
import org.w3c.dom.HTMLInputElement

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NNumberField = HTMLInputElement

@ViewDsl
actual inline fun ViewWriter.numberFieldActual(crossinline setup: NumberField.() -> Unit): Unit =
    themedElementEditable<HTMLInputElement>("input") {
        addEventListener("input", {
            numberAutocommaRepair(
                dirty = value,
                selectionStart = selectionStart,
                selectionEnd = selectionEnd,
                setResult = {
                    println("Repairing to $it")
                    value = it
                            },
                setSelectionRange = {start, end, -> setSelectionRange(start, end)}
            )
        })
        setup(NumberField(this).also {
            it.keyboardHints = KeyboardHints.decimal
            it.align = Align.End
        })
    }

actual val NumberField.content: Writable<Double?> get() = native.vprop("input", { value }, { value = it }).asDouble()
actual inline var NumberField.keyboardHints: KeyboardHints
    get() = TODO()
    set(value) {
        native.type = when (value.type) {
            KeyboardType.Text -> "text"
            KeyboardType.Decimal -> "text"
            KeyboardType.Integer -> "text"
            KeyboardType.Phone -> "tel"
            KeyboardType.Email -> "text"
        }
        native.inputMode = when (value.type) {
            KeyboardType.Text -> "text"
            KeyboardType.Decimal -> "decimal"
            KeyboardType.Integer -> "numeric"
            KeyboardType.Phone -> "tel"
            KeyboardType.Email -> "email"
        }

        when (value.autocomplete) {
            AutoComplete.Email -> {
                native.type = "email"
                native.autocomplete = "email"
            }

            AutoComplete.Password -> {
                native.type = "password"
                native.autocomplete = "current-password"
            }

            AutoComplete.NewPassword -> {
                native.type = "password"
                native.autocomplete = "new-password"
            }

            AutoComplete.Phone -> {
                native.autocomplete = "tel"
            }

            null -> {
                native.autocomplete = "off"
            }
        }
    }
actual var NumberField.action: Action?
    get() = TODO()
    set(value) {
        native.onkeyup = if (value == null) null else { ev ->
            if (ev.keyCode == 13) {
                launchGlobal {
                    value.onSelect()
                }
            }
        }
    }
actual inline var NumberField.hint: String
    get() = native.placeholder
    set(value) {
        native.placeholder = value
    }
actual inline var NumberField.range: ClosedRange<Double>?
    get() {
        if (native.min.isBlank()) return null
        if (native.max.isBlank()) return null
        return native.min.toDouble()..native.max.toDouble()
    }
    set(value) {
        value?.let {
            native.min = it.start.toString()
            native.max = it.endInclusive.toString()
        } ?: run {
            native.removeAttribute("min")
            native.removeAttribute("max")
        }
    }
actual inline var NumberField.align: Align
    get() = when (window.getComputedStyle(native).textAlign) {
        "start" -> Align.Start
        "center" -> Align.Center
        "end" -> Align.End
        "justify" -> Align.Stretch
        else -> Align.Start
    }
    set(value) {
        native.style.textAlign = when (value) {
            Align.Start -> "start"
            Align.Center -> "center"
            Align.End -> "end"
            Align.Stretch -> "justify"
        }
    }
actual inline var NumberField.textSize: Dimension
    get() = Dimension(window.getComputedStyle(native).fontSize)
    set(value) {
        native.style.fontSize = value.value
    }