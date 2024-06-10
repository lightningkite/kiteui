package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.launchManualCancel
import com.lightningkite.kiteui.views.*


actual class DismissBackground actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "div"
        native.classes.add("dismissBackground")
        native.classes.add("kiteui-stack")
        native.addEventListener("click") { navigator.clear() }
        // TODO
        //native.listNViews().forEach { it.onclick = { ev -> ev.stopImmediatePropagation() } }
    }

    actual fun onClick(action: suspend () -> Unit): Unit {
        native.addEventListener("click") { launchManualCancel(action) }
    }
}