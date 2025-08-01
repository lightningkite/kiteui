package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.KeyboardEvent
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*


public actual class TextInput public actual constructor(context: RContext) : RViewWithAction(context) {
    init {
        native.tag = "input"
        native.classes.add("editable")
    }
    actual val content: MutableReactiveValue<String> = native.vprop("input", { attributes.valueString ?: "" }, { attributes.valueString = it })
    actual var keyboardHints: KeyboardHints = KeyboardHints()
        set(value) {
            field = value
            native.applyKeyboardHints(value)

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