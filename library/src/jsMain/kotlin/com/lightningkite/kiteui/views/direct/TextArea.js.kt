package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.AutoComplete
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import org.w3c.dom.HTMLTextAreaElement

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NTextArea = HTMLTextAreaElement

@ViewDsl
actual inline fun ViewWriter.textAreaActual(crossinline setup: TextArea.() -> Unit): Unit =
    themedElementEditable<NTextArea>("textarea") { setup(TextArea(this)) }

actual val TextArea.content: Writable<String> get() = native.vprop("input", { value }, { value = it })
actual inline var TextArea.keyboardHints: KeyboardHints
    get() = TODO()
    set(value) {
        when (value.autocomplete) {
            AutoComplete.Email -> {
                native.autocomplete = "email"
            }

            AutoComplete.Password -> {
                native.autocomplete = "current-password"
            }

            AutoComplete.NewPassword -> {
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
actual inline var TextArea.hint: String
    get() = TODO()
    set(value) {}

actual inline var TextArea.enabled: Boolean
    get() = !native.readOnly
    set(value) {
        native.readOnly = !value
    }