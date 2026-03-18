package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.KeyboardEvent
import com.lightningkite.kiteui.models.AutoComplete
import com.lightningkite.kiteui.models.KeyCodes
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*

actual class TextArea actual constructor(context: ElementContext) : RViewWithAction(context) {
    override val driverValue: String? get() = textAreaDriverValue()
    override val driverActions get() = super.driverActions + textAreaDriverActions()
    init {
        native.tag = "div"
        native.classes.add("textarea-container")
    }
    val textarea = FutureElement().apply {
        tag = "textarea"
        classes.add("editable")
        classes.add("kui")
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
    actual val content: MutableReactiveValue<String> = textarea.vprop("input", { attributes.valueString ?: "" }, { attributes.valueString = it })
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