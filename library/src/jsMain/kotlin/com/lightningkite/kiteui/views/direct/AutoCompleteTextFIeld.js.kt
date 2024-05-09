package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.ViewDsl
import ViewWriter
import com.lightningkite.kiteui.views.launch
import kotlinx.browser.document
import org.w3c.dom.HTMLDataListElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import kotlin.random.Random

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NAutoCompleteTextField = HTMLInputElement

@ViewDsl
actual inline fun ViewWriter.autoCompleteTextFieldActual(crossinline setup: AutoCompleteTextField.() -> Unit): Unit =
    themedElementEditable<NAutoCompleteTextField>("input") { setup(AutoCompleteTextField(this)) }

actual val AutoCompleteTextField.content: Writable<String>
    get() = native.vprop("input", { value }, {
        value = it
    })
actual inline var AutoCompleteTextField.suggestions: List<String>
    get() = TODO()
    set(value) {
        val listId = native.getAttribute("list") ?: run {
            val newId = "datalist" + Random.nextInt(0, Int.MAX_VALUE)
            document.body!!.appendChild((document.createElement("datalist") as HTMLDataListElement).apply {
                id = newId
            })
            native.setAttribute("list", newId)
            newId
        }
        document.getElementById(listId)?.let { it as? HTMLElement }?.apply {
            __resetContentToOptionList(value.map { WidgetOption(it, it) }, this@suggestions.native.value)
        }
    }
actual inline var AutoCompleteTextField.keyboardHints: KeyboardHints
    get() = TODO()
    set(value) {
        native.type = when (value.type) {
            KeyboardType.Text -> "text"
            KeyboardType.Decimal -> "number"
            KeyboardType.Integer -> "number"
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
actual var AutoCompleteTextField.action: Action?
    get() = TODO()
    set(value) {
        native.onkeyup = if (value == null) null else { ev ->
            if (ev.keyCode == 13) {
                launch {
                    value.onSelect()
                }
            }
        }
    }