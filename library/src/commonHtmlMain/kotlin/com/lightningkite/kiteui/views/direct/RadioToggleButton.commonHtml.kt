package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.KeyboardEvent
import com.lightningkite.kiteui.reactive.ImmediateWritable
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.*


actual class RadioToggleButton actual constructor(context: RContext) : RView(context) {
    val input = FutureElement().apply {
        tag = "input"
        attributes.type = "radio"
        classes.add("checkResponsive")
        attributes.hidden = true
        style.display = "none"
    }

    init {
        native.tag = "label"
        native.classes.add("kiteui-stack")
        native.classes.add("checkResponsive")
        native.classes.add("clickable")
        native.attributes.tabIndex = 0
        native.addEventListener("keydown", { ev ->
            ev as KeyboardEvent
            if (ev.code == KeyCodes.space || ev.code == KeyCodes.enter) {
                ev.preventDefault()
            }
        })
        native.addEventListener("keyup", { ev ->
            ev as KeyboardEvent
            if (ev.code == KeyCodes.space || ev.code == KeyCodes.enter) {
                input.click()
                ev.preventDefault()
            }
        })
        native.appendChild(input)
    }

    override fun hasAlternateBackedStates(): Boolean = true

    actual val checked: ImmediateWritable<Boolean> = input.vprop(
        "input",
        { attributes.checked == true },
        { value -> attributes.checked = value })
    init {
        checked.addListener {
            if(checked.value)
                native.classes.add("checked")
            else
                native.classes.remove("checked")
        }
    }

    actual inline var enabled: Boolean
        get() = input.attributes.disabled != true
        set(value) {
            input.attributes.disabled = !value
        }
}
