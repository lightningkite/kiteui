package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.AutoComplete
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewDsl

actual class TextArea actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "textarea"
        native.classes.add("editable")
    }
    actual val content: Writable<String> = native.vprop("input", { attributes["value"] ?: "" }, { attributes["value"] = it })
    actual var keyboardHints: KeyboardHints = KeyboardHints()
        set(value) {
            field = value
            when (value.autocomplete) {
                AutoComplete.Email -> {
                    native.attributes["autocomplete"] = "email"
                }

                AutoComplete.Password -> {
                    native.attributes["autocomplete"] = "current-password"
                }

                AutoComplete.NewPassword -> {
                    native.attributes["autocomplete"] = "new-password"
                }

                AutoComplete.Phone -> {
                    native.attributes["autocomplete"] = "tel"
                }

                null -> {
                    native.attributes["autocomplete"] = "off"
                }
            }
        }
    actual var hint: String = ""
        set(value) { field = value }
}