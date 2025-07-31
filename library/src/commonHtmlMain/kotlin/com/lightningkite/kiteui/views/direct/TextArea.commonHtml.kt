package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.KeyboardEvent
import com.lightningkite.kiteui.models.AutoComplete
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.signal.ImmediateWritable
import com.lightningkite.signal.Writable
import com.lightningkite.kiteui.views.*

public actual class TextArea public actual constructor(context: RContext) : RViewWithAction(context) {
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
            if (ev.code == KeyCodes.enter && !ev.shiftKey && action != null) {
                action?.startAction(this@TextArea)
                ev.preventDefault()
                ev.stopImmediatePropagation()
            }
        }
        native.appendChild(this)
    }
    public actual val content: ImmediateWritable<String> = textarea.vprop("input", { attributes.valueString ?: "" }, { attributes.valueString = it })
    init {
        content.addListener {
            native.setAttribute("data-replicated-value", content.value)
        }
    }
    public actual var keyboardHints: KeyboardHints = KeyboardHints()
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
    public actual var hint: String = ""
        set(value) {
            field = value
            textarea.attributes.placeholder = value
        }
    public actual var enabled: Boolean
        get() = !(textarea.attributes.disabled ?: false)
        set(value) { textarea.attributes.disabled = !value }
}