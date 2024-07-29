package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView

actual class Separator actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "div"
        native.classes.add("kiteui-separator")
    }
}