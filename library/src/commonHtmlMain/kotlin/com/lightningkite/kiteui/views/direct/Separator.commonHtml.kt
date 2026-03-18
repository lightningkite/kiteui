package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.RView

actual class Separator actual constructor(context: ElementContext) : RView(context) {
    init {
        native.tag = "div"
        native.classes.add("kiteui-separator")
    }
}