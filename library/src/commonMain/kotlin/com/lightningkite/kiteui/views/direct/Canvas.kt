package com.lightningkite.kiteui.views.direct

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
    public open fun RView.fallbackView() = { text("Rich content here that doesn't support accessibility.") }
}

public class KeyCodeWithModifiers(public val code: KeyCode, public val alt: Boolean, public val ctrl: Boolean, public val shift: Boolean, public val meta: Boolean)

public expect class KeyCode
public expect object KeyCodes {
    public val left: KeyCode
    public val right: KeyCode
    public val up: KeyCode
    public val down: KeyCode
    public fun letter(char: Char): KeyCode
    public fun num(digit: Int): KeyCode
    public fun numpad(digit: Int): KeyCode
    public val space: KeyCode
    public val enter: KeyCode
    public val tab: KeyCode
    public val escape: KeyCode
    public val leftCtrl: KeyCode
    public val rightCtrl: KeyCode
    public val leftShift: KeyCode
    public val rightShift: KeyCode
    public val leftAlt: KeyCode
    public val rightAlt: KeyCode
    public val equals: KeyCode
    public val dash: KeyCode
    public val backslash: KeyCode
    public val leftBrace: KeyCode
    public val rightBrace: KeyCode
    public val semicolon: KeyCode
    public val comma: KeyCode
    public val period: KeyCode
}