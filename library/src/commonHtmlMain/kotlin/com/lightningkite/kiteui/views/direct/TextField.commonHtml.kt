package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.KeyboardEvent
import com.lightningkite.kiteui.models.*
import com.lightningkite.signal.ImmediateWritable
import com.lightningkite.signal.Writable
import com.lightningkite.kiteui.views.*


public actual class TextInput public actual constructor(context: RContext) : RViewWithAction(context) {
    init {
        native.tag = "input"
        native.classes.add("editable")
    }
    public actual val content: ImmediateWritable<String> = native.vprop("input", { attributes.valueString ?: "" }, { attributes.valueString = it })
    public actual var keyboardHints: KeyboardHints = KeyboardHints()
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

            val primaryAutocompleteValue: String?
            when (value.autocomplete) {
                AutoComplete.Email -> {
                    native.attributes.type = "email"
                    primaryAutocompleteValue = "email"
                }

                AutoComplete.Password -> {
                    native.attributes.type = "password"
                    primaryAutocompleteValue = "current-password"
                }

                AutoComplete.NewPassword -> {
                    native.attributes.type = "password"
                    primaryAutocompleteValue = "new-password"
                }

                AutoComplete.Phone -> {
                    primaryAutocompleteValue = "tel"
                }

                AutoComplete.OneTimeCode, null -> {
                    primaryAutocompleteValue = null
                }
            }

            native.attributes.autocomplete = listOfNotNull(
                primaryAutocompleteValue,
                "webauthn".takeIf { value.includePasskeys }
            ).joinToString(" ").takeIf { it.isNotEmpty() } ?: "off"
        }
    init {
        native.addEventListener("keyup") { ev ->
            ev as KeyboardEvent
            if (ev.code == KeyCodes.enter) {
                action?.startAction(this)
            }
        }
    }
    public actual inline var hint: String
        get() = native.attributes.placeholder ?: ""
        set(value) {
            native.attributes.placeholder = value
        }
    public actual var align: Align = Align.Start
        set(value) {
            field = value
            native.style.textAlign = when (value) {
                Align.Start -> "start"
                Align.Center -> "center"
                Align.End -> "end"
                Align.Stretch -> "justify"
            }
        }
    public actual var enabled: Boolean
        get() = !(native.attributes.disabled ?: false)
        set(value) { native.attributes.disabled = !value }

}