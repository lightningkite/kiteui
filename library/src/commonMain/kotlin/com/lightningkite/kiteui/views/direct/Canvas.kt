package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.KeyCode
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.canvas.DrawingContext2D
import kotlin.jvm.JvmInline
import kotlin.contracts.*


public expect class Canvas(context: RContext) : RView {
    public var delegate: CanvasDelegate?
}

public abstract class CanvasDelegate {
    public open fun onResize(width: Double, height: Double) {}
    public open fun draw(context: DrawingContext2D) {}
    public open fun onPointerDown(id: Int, x: Double, y: Double, width: Double, height: Double): Boolean = false
    public open fun onPointerMove(id: Int, x: Double, y: Double, width: Double, height: Double): Boolean = false
    public open fun onPointerCancel(id: Int, x: Double, y: Double, width: Double, height: Double): Boolean = false
    public open fun onPointerUp(id: Int, x: Double, y: Double, width: Double, height: Double): Boolean = false
    public open fun onKeyDown(key: KeyCode): Boolean = false
    public open fun onKeyUp(key: KeyCode): Boolean = false
    public open fun onWheel(x: Double, y: Double, z: Double): Boolean = false

    //    open fun onAccelerometer(x: Double, y: Double, z: Double): Boolean = false
    public open fun sizeThatFitsWidth(width: Double, height: Double): Double = width
    public open fun sizeThatFitsHeight(width: Double, height: Double): Double = height
    public var invalidate: () -> Unit = {}
    public var theme: Theme = Theme.placeholder
    public open fun RView.fallbackView() = { text("Rich content here that doesn't support accessibility.") }
}
