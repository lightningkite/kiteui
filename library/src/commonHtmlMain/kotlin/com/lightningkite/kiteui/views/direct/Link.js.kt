package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.launchManualCancel
import com.lightningkite.kiteui.navigation.*
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.DynamicCss.basePath
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.navigator


actual class Link actual constructor(context: RContext) : RView(context) {

    init {
        native.tag = "a"
        native.classes.add("kiteui-stack")
        native.events["click"] = {
            it.preventDefault()
            if(resetsStack) {
                navigator.reset(to())
            } else {
                navigator.navigate(to())
            }
            onNavigate?.let {
                launchManualCancel(it)
            }
        }
    }

    actual var navigator: KiteUiNavigator = (this as RView).navigator
    actual var to: ()->Screen = { Screen.Empty }
        set(value) {
            field = value
            navigator.routes.render(value())?.let {
                native.attributes["href"] = basePath + it.urlLikePath.render()
            }
        }
    actual inline var newTab: Boolean
        get() = native.attributes["target"] == "_blank"
        set(value) {
            native.attributes["target"] = if (value) "_blank" else "_self"
        }
    actual var resetsStack: Boolean = false

    private var onNavigate: suspend () -> Unit = {}
    actual fun onNavigate(action: suspend () -> Unit): Unit {
        onNavigate = action
    }
}
