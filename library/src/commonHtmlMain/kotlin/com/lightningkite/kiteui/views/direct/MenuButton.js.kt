package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.UseBackground
import com.lightningkite.kiteui.views.ViewWriter


actual class MenuButton actual constructor(context: RContext): RView(context) {
    val floating = FloatingInfoHolder(this)
    init {
        native.tag = "button"
        native.classes.add("kiteui-stack")
        native.classes.add("clickable")
        native.events["click"] = {
            floating.open()
        }
        native.events["mouseenter"] = {
            floating.open()
        }
    }

    init { if(useBackground == UseBackground.No) useBackground = UseBackground.IfChanged }

    actual inline var enabled: Boolean
        get() = native.attributes["disabled"] == null
        set(value) {
            native.attributes["disabled"] = "true".takeUnless { value }
        }
    actual var requireClick: Boolean = false
    actual var preferredDirection: PopoverPreferredDirection by floating::preferredDirection
    actual fun opensMenu(action: ViewWriter.() -> Unit) {
        floating.menuGenerator = action
    }
}

