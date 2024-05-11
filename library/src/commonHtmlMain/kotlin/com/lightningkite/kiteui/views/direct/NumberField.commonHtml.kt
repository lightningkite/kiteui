package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.KeyboardEvent
import com.lightningkite.kiteui.launchGlobal
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.BaseListenable
import com.lightningkite.kiteui.reactive.ReadableState
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.reactive.asDouble
import com.lightningkite.kiteui.utils.commaString
import com.lightningkite.kiteui.utils.numberAutocommaRepair
import com.lightningkite.kiteui.views.*

actual class NumberField actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "input"
        native.classes.add("editable")
    }
    actual val content: Writable<Double?> = object : Writable<Double?>, BaseListenable() {
        init {
            native.addEventListener("input") {
                numberAutocommaRepair(
                    dirty = native.attributes.valueString ?: "",
                    selectionStart = selectionStart,
                    selectionEnd = selectionEnd,
                    setResult = {
                        native.attributes.valueString = it
                    },
                    setSelectionRange = {start, end, -> setSelectionRange(start, end)}
                )
                invokeAll()
            }
        }
        override val state: ReadableState<Double?> get() = ReadableState(native.attributes.valueString?.filter { it.isDigit() || it == '.' }?.toDoubleOrNull())
        override suspend fun set(value: Double?) {
            native.attributes.valueString = value?.commaString()
        }
    }
    actual inline var keyboardHints: KeyboardHints
        get() = TODO()
        set(value) {
            native.attributes.type = when (value.type) {
                KeyboardType.Text -> "text"
                KeyboardType.Decimal -> "text"
                KeyboardType.Integer -> "text"
                KeyboardType.Phone -> "tel"
                KeyboardType.Email -> "text"
            }
            native.attributes.inputMode = when (value.type) {
                KeyboardType.Text -> "text"
                KeyboardType.Decimal -> "decimal"
                KeyboardType.Integer -> "numeric"
                KeyboardType.Phone -> "tel"
                KeyboardType.Email -> "email"
            }

            when (value.autocomplete) {
                AutoComplete.Email -> {
                    native.attributes.type = "email"
                    native.attributes.autocomplete = "email"
                }

                AutoComplete.Password -> {
                    native.attributes.type = "password"
                    native.attributes.autocomplete = "current-password"
                }

                AutoComplete.NewPassword -> {
                    native.attributes.type = "password"
                    native.attributes.autocomplete = "new-password"
                }

                AutoComplete.Phone -> {
                    native.attributes.autocomplete = "tel"
                }

                null -> {
                    native.attributes.autocomplete = "off"
                }
            }
        }
    actual var action: Action? = null
        set(value) {
            field = value
             if (value != null) native.addEventListener("keyup") { ev ->
                ev as KeyboardEvent
                if (ev.code == KeyCodes.enter) {
                    launchGlobal {
                        value.onSelect()
                    }
                }
            }
        }
    actual inline var hint: String
        get() = native.attributes.placeholder ?: ""
        set(value) {
            native.attributes.placeholder = value
        }
    actual var align: Align = Align.Start
        set(value) {
            native.style.textAlign = when (value) {
                Align.Start -> "start"
                Align.Center -> "center"
                Align.End -> "end"
                Align.Stretch -> "justify"
            }
        }
    actual var textSize: Dimension = 1.rem
        set(value) {
            field = value
            native.style.fontSize = value.value
        }

    actual var range: ClosedRange<Double>? = null
        set(value) {
            field = value
            value?.let {
                native.attributes.maxDouble = it.start
                native.attributes.maxDouble = it.endInclusive
            } ?: run {
                native.attributes.maxDouble = null
                native.attributes.maxDouble = null
            }
        }
}

expect val NumberField.selectionStart: Int?
expect val NumberField.selectionEnd: Int?
expect fun NumberField.setSelectionRange(start: Int, end: Int)