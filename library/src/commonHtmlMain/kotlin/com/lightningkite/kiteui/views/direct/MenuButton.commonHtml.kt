package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.*


actual class MenuButton actual constructor(context: RContext): RView(context) {
    val floating = FloatingInfoHolder(this)
    init {
        native.tag = "button"
        native.classes.add("kiteui-stack")
        native.classes.add("clickable")
        native.addEventListener("click") {
            floating.open()
        }
        native.addEventListener("mouseenter") {
            floating.open()
        }
    }

    init { if(useBackground == UseBackground.No) useBackground = UseBackground.IfChanged }

    actual inline var enabled: Boolean
        get() = native.attributes.disabled != true
        set(value) {
            native.attributes.disabled = !value
        }
    actual var requireClick: Boolean = false
    actual var preferredDirection: PopoverPreferredDirection by floating::preferredDirection
    actual fun opensMenu(action: ViewWriter.() -> Unit) {
        floating.menuGenerator = action
    }
}

