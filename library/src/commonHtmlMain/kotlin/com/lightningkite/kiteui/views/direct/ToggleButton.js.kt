package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.KeyboardEvent
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.*


actual class ToggleButton actual constructor(context: RContext) : RView(context) {
    val input = FutureElement().apply {
        tag = "input"
        attributes["type"] = "radio"
        classes.add("checkResponsive")
        attributes["hidden"] = "true"
        style.display = "none"
    }
    val mainSpan = FutureElement().apply {
        tag = "span"
        classes.add("kiteui-stack")
        attributes["tab-index"] = "0"
        events["keydown"] = ({ ev ->
            ev as KeyboardEvent
            if (ev.code == KeyCodes.space || ev.code == KeyCodes.enter) {
                input.attributes["checked"] = null
                ev.preventDefault()
            }
        })
        events["keyup"] = ({ ev ->
            ev as KeyboardEvent
            if (ev.code == KeyCodes.space || ev.code == KeyCodes.enter) {
                input.attributes["checked"] = "true"
                ev.preventDefault()
            }
        })
    }

    init {
        native.tag = "label"
        native.classes.add("toggle-button")
        native.classes.add("checkResponsive")
        native.classes.add("clickable")
        native.children.addAll(listOf(input, mainSpan))
    }

    init {
        useBackground = UseBackground.Yes
    }

    actual val checked: Writable<Boolean> = input.vprop(
        "input",
        { attributes["checked"] != null },
        { value -> attributes["checked"] = "true".takeIf { value } })

    actual inline var enabled: Boolean
        get() = input.attributes["disabled"] == null
        set(value) {
            input.attributes["disabled"] = "true".takeUnless { value }
        }
}
