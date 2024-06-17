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
    val mainSpan = FutureElement().apply {
        tag = "span"
        classes.add("kiteui-stack")
        attributes.tabIndex = 0
        addEventListener("keydown", { ev ->
            ev as KeyboardEvent
            if (ev.code == KeyCodes.space || ev.code == KeyCodes.enter) {
                ev.preventDefault()
            }
        })
        addEventListener("keyup", { ev ->
            ev as KeyboardEvent
            if (ev.code == KeyCodes.space || ev.code == KeyCodes.enter) {
                input.attributes.checked = true
                ev.preventDefault()
            }
        })
    }

    init {
        native.tag = "label"
        native.classes.add("toggle-button")
        native.classes.add("checkResponsive")
        native.classes.add("clickable")
        native.appendChild(input)
        native.appendChild(mainSpan)
    }

    init {
        useBackground = UseBackground.Yes
    }

    actual val checked: ImmediateWritable<Boolean> = input.vprop(
        "input",
        { attributes.checked == true },
        { value -> attributes.checked = value })

    actual inline var enabled: Boolean
        get() = input.attributes.disabled != true
        set(value) {
            input.attributes.disabled = !value
        }
}
