package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.launchManualCancel
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.*

actual class Button actual constructor(context: RContext): RView(context) {
    init {
        native.tag = "button"
        native.classes.add("kiteui-stack")
        native.classes.add("clickable")
    }

    init { if(useBackground == UseBackground.No) useBackground = UseBackground.IfChanged }

    actual fun onClick(action: suspend () -> Unit): Unit {
        var virtualDisable: Boolean = false
        native.events["click"] = {
            if(!virtualDisable) {
                launchManualCancel {
                    try {
                        virtualDisable = true
                        action()
                    } finally {
                        virtualDisable = false
                    }
                }
            }
        }
    }

    actual inline var enabled: Boolean
        get() = native.attributes["disabled"] == null
        set(value) {
            native.attributes["disabled"] = "true".takeUnless { value }
        }
}
