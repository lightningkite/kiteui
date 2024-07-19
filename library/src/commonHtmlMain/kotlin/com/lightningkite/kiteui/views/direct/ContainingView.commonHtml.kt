package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.views.*

actual class Stack actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "div"
        native.classes.add("kiteui-stack")
    }

    override fun internalAddChild(index: Int, view: RView) {
        super.internalAddChild(index, view)
        native.children.forEachIndexed { index, futureElement ->
            futureElement.style.zIndex = index.toString()
        }
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

actual class RowCollapsingToColumn actual constructor(context: RContext, breakpoints: List<Dimension>) : RView(context) {
    init {
        native.tag = "div"
        native.classes.add(context.kiteUiCss.rowCollapsingToColumn(breakpoints))
        native.classes.add("rowCollapsing")
    }
}
