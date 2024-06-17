package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.KeyboardEvent
import com.lightningkite.kiteui.launchGlobal
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.ImmediateWritable
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.*


actual class TextField actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "input"
        native.classes.add("editable")
    }
    actual val content: ImmediateWritable<String> = native.vprop("input", { attributes.valueString ?: "" }, { attributes.valueString = it })
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
}