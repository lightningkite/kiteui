package com.lightningkite.kiteui.views.direct

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
    open fun RView.fallbackView() = { text("Rich content here that doesn't support accessibility.") }
}

expect class KeyCode
expect object KeyCodes {
    val left: KeyCode
    val right: KeyCode
    val up: KeyCode
    val down: KeyCode
    fun letter(char: Char): KeyCode
    fun num(digit: Int): KeyCode
    fun numpad(digit: Int): KeyCode
    val space: KeyCode
    val enter: KeyCode
    val tab: KeyCode
    val escape: KeyCode
    val leftCtrl: KeyCode
    val rightCtrl: KeyCode
    val leftShift: KeyCode
    val rightShift: KeyCode
    val leftAlt: KeyCode
    val rightAlt: KeyCode
    val equals: KeyCode
    val dash: KeyCode
    val backslash: KeyCode
    val leftBrace: KeyCode
    val rightBrace: KeyCode
    val semicolon: KeyCode
    val comma: KeyCode
    val period: KeyCode
}