package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.RView

actual class RowWrapping actual constructor(context: ElementContext) : RView(context) {
    init {
        native.tag = "div"
        native.setStyleProperty("display", "flex")
        native.setStyleProperty("flex-direction", "row")
        native.setStyleProperty("flex-wrap", "wrap")
        native.classes += "kiteui-flex"
        native.classes += "kiteui-row-wrap"
    }

    override fun internalAddChild(index: Int, view: RView) {
        super.internalAddChild(index, view)

        // Handle alignment
        val horizontalAlign = view.native.desiredHorizontalGravity
        val verticalAlign = view.native.desiredVerticalGravity

        // Set horizontal alignment
        when(horizontalAlign) {
            Align.Start -> {
                view.native.setStyleProperty("justify-self", "start")
            }
            Align.Center -> {
                view.native.setStyleProperty("justify-self", "center")
            }
            Align.End -> {
                view.native.setStyleProperty("justify-self", "end")
            }
            else -> {
                view.native.setStyleProperty("justify-self", "stretch")
            }
        }

        // Set vertical alignment
        when(verticalAlign) {
            Align.Start -> {
                view.native.setStyleProperty("align-self", "start")
            }
            Align.Center -> {
                view.native.setStyleProperty("align-self", "center")
            }
            Align.End -> {
                view.native.setStyleProperty("align-self", "end")
            }
            else -> {
                view.native.setStyleProperty("align-self", "stretch")
            }
        }
    }
}
