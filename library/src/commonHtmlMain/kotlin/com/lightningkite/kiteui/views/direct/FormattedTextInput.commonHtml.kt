package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.KeyboardEvent
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.BaseListenable
import com.lightningkite.kiteui.reactive.ImmediateWritable
import com.lightningkite.kiteui.utils.repairFormatAndPosition
import com.lightningkite.kiteui.views.*

actual class FormattedTextInput actual constructor(context: RContext) : RViewWithAction(context) {
    init {
        native.tag = "input"
        native.classes.add("editable")
    }

    init {
        native.addEventListener("keyup") { ev ->
            ev as KeyboardEvent
            if (ev.code == KeyCodes.enter) {
                action?.startAction(this)
            }
        }
    }

    private var formatter: (String) -> String = { it }
    private var isRawData: (Char) -> Boolean = { true }
    actual fun format(
        isRawData: (Char) -> Boolean,
        formatter: (clean: String) -> String,
    ) {
        this.formatter = formatter
        this.isRawData = isRawData
    }

    actual val content: ImmediateWritable<String> = object : ImmediateWritable<String>, BaseListenable() {
        init {
            native.addEventListener("input") {
                repairFormatAndPosition(
                    dirty = native.attributes.valueString ?: "",
                    selectionStart = selectionStart,
                    selectionEnd = selectionEnd,
                    setResult = {
                        native.attributes.valueString = it
                    },
                    setSelectionRange = { start, end, -> setSelectionRange(start, end) },
                    isRawData = isRawData,
                    formatter = formatter,
                )
                invokeAllListeners()
            }
        }

        override var value: String
            get() = native.attributes.valueString?.filter(isRawData) ?: ""
            set(value) {
                val clean = value.filter(isRawData)
                val formatted = formatter(clean)
                if (native.attributes.valueString != formatted)
                    native.attributes.valueString = formatted
                    invokeAllListeners()
            }
    }
    actual var keyboardHints: KeyboardHints = KeyboardHints()
        set(value) {
            field = value
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

    actual inline var hint: String
        get() = native.attributes.placeholder ?: ""
        set(value) {
            native.attributes.placeholder = value
        }
    actual var align: Align = Align.Start
        set(value) {
            field = value
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

    actual var enabled: Boolean
        get() = !(native.attributes.disabled ?: false)
        set(value) { native.attributes.disabled = !value }
}

expect val FormattedTextInput.selectionStart: Int?
expect val FormattedTextInput.selectionEnd: Int?
expect fun FormattedTextInput.setSelectionRange(start: Int, end: Int)