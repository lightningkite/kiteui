package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.launchManualCancel
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.navigator


actual class ExternalLink actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "a"
        native.classes.add("kiteui-stack")
    }

    actual inline var to: String
        get() = native.attributes["href"] ?: ""
        set(value) {
            native.attributes["href"] = value
        }
    actual inline var newTab: Boolean
        get() = native.attributes["target"] == "_blank"
        set(value) {
            native.attributes["target"] = if (value) "_blank" else "_self"
        }
    actual fun onNavigate(action: suspend () -> Unit): Unit {
        native.events["click"] = {
            launchManualCancel(action)
        }
    }
}
