package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.KeyCode
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.canvas.DrawingContext2D
import kotlin.jvm.JvmInline
import kotlin.contracts.*


expect class Canvas(context: RContext) : RView {
    var delegate: CanvasDelegate?
}

abstract class CanvasDelegate {
    open fun onResize(width: Double, height: Double) {}
    open fun draw(context: DrawingContext2D) {}
    open fun onPointerDown(id: Int, x: Double, y: Double, width: Double, height: Double): Boolean = false
    open fun onPointerMove(id: Int, x: Double, y: Double, width: Double, height: Double): Boolean = false
    open fun onPointerCancel(id: Int, x: Double, y: Double, width: Double, height: Double): Boolean = false
    open fun onPointerUp(id: Int, x: Double, y: Double, width: Double, height: Double): Boolean = false
    open fun onKeyDown(key: KeyCode): Boolean = false
    open fun onKeyUp(key: KeyCode): Boolean = false
    open fun onWheel(x: Double, y: Double, z: Double): Boolean = false

    //    open fun onAccelerometer(x: Double, y: Double, z: Double): Boolean = false
    open fun sizeThatFitsWidth(width: Double, height: Double): Double = width
    open fun sizeThatFitsHeight(width: Double, height: Double): Double = height
    var invalidate: () -> Unit = {}
    var theme: Theme = Theme.placeholder
    open fun RView.fallbackView() = { text("Rich content here that doesn't support accessibility.") }
}
