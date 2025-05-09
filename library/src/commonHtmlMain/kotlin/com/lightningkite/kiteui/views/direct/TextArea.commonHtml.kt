package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.KeyboardEvent
import com.lightningkite.kiteui.models.AutoComplete
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.readable.ImmediateWritable
import com.lightningkite.readable.Writable
import com.lightningkite.kiteui.views.*

actual class TextArea actual constructor(context: RContext) : RViewWithAction(context) {
    init {
        native.tag = "div"
        native.classes.add("textarea-container")
    }
    val textarea = FutureElement().apply {
        tag = "textarea"
        classes.add("editable")
        style.resize = "none"
        addEventListener("keydown") { ev ->
            ev as KeyboardEvent
            if (ev.code == KeyCodes.enter && !ev.shiftKey && !ev && action != null) {
                action?.startAction(this@TextArea)
                ev.preventDefault()
                ev.stopImmediatePropagation()
            }
        }
        native.appendChild(this)
    }
    actual val content: ImmediateWritable<String> = textarea.vprop("input", { attributes.valueString ?: "" }, { attributes.valueString = it })
    init {
        content.addListener {
            native.setAttribute("data-replicated-value", content.value)
        }
    }
    actual var keyboardHints: KeyboardHints = KeyboardHints()
        set(value) {
            field = value
            when (value.autocomplete) {
                AutoComplete.Email -> {
                    textarea.attributes.autocomplete = "email"
                }

                AutoComplete.Password -> {
                    textarea.attributes.autocomplete = "current-password"
                }

                AutoComplete.NewPassword -> {
                    textarea.attributes.autocomplete = "new-password"
                }

                AutoComplete.Phone -> {
                    textarea.attributes.autocomplete = "tel"
                }

                AutoComplete.OneTimeCode, null -> {
                    textarea.attributes.autocomplete = "off"
                }
            }
        }
    actual var hint: String = ""
        set(value) {
            field = value
            textarea.attributes.placeholder = value
        }
    actual var enabled: Boolean
        get() = !(textarea.attributes.disabled ?: false)
        set(value) { textarea.attributes.disabled = !value }
}