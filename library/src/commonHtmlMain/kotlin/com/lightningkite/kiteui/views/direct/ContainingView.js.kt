package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.KiteUiCss
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView

actual class Stack actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "div"
        native.classes.add("kiteui-stack")
    }
}

actual class RowOrCol actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "div"
        native.classes.add("kiteui-col")
    }
    actual var vertical: Boolean
        get() = native.classes.contains("kiteui-col")
        set(value) {
            if(value) {
                native.classes.add("kiteui-col")
                native.classes.remove("kiteui-row")
            } else {
                native.classes.remove("kiteui-col")
                native.classes.add("kiteui-row")
            }
        }
}

actual class RowCollapsingToColumn actual constructor(context: RContext, breakpoint: Dimension) : RView(context) {
    init {
        native.tag = "div"
        native.classes.add(KiteUiCss.rowCollapsingToColumn(breakpoint))
    }
}
