package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.dom.*
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.*


public actual class Canvas public actual constructor(context: RContext): RView(context) {
    public actual var delegate: CanvasDelegate? = null
        set(value) {
            field = value
            onDelegateSet(value)
        }
    init {
        native.tag = "canvas"
        native.attributes.tabIndex = 1
        native.addEventListener("keydown") { event ->
            event as KeyboardEvent
            if(delegate?.onKeyDown(event.code) == true)
                event.preventDefault()
        }
        native.addEventListener("keyup") { event ->
            event as KeyboardEvent
            if(delegate?.onKeyUp(event.code) == true)
                event.preventDefault()
        }
        native.addEventListener("wheel") { event ->
            event as WheelEvent
            if(delegate?.onWheel(event.deltaX, event.deltaY, event.deltaZ) == true)
                event.preventDefault()
        }
        val dpr = AppState.windowInfo.value.density
        native.addEventListener("pointerdown") { event ->
            event as PointerEvent
            val b = (event.target as Element).getBoundingClientRect()
            if(delegate?.onPointerDown(
                    event.pointerId,
                    (event.pageX - b.x) * dpr,
                    (event.pageY - b.y) * dpr,
                    (b.width) * dpr,
                    (b.height) * dpr
            ) == true) event.preventDefault()
        }
        native.addEventListener("pointermove") { event ->
            event as PointerEvent
            val b = (event.target as Element).getBoundingClientRect()
            if(delegate?.onPointerMove(
                    event.pointerId,
                    (event.pageX - b.x) * dpr,
                    (event.pageY - b.y) * dpr,
                    (b.width) * dpr,
                    (b.height) * dpr
            ) == true) event.preventDefault()
        }
        native.addEventListener("pointerup") { event ->
            event as PointerEvent
            val b = (event.target as Element).getBoundingClientRect()
            if(delegate?.onPointerUp(
                    event.pointerId,
                    (event.pageX - b.x) * dpr,
                    (event.pageY - b.y) * dpr,
                    (b.width) * dpr,
                    (b.height) * dpr
            ) == true) event.preventDefault()
        }
        native.addEventListener("pointercancel") { event ->
            event as PointerEvent
            val b = (event.target as Element).getBoundingClientRect()
            if(delegate?.onPointerCancel(
                    event.pointerId,
                    (event.pageX - b.x) * dpr,
                    (event.pageY - b.y) * dpr,
                    (b.width) * dpr,
                    (b.height) * dpr
            ) == true) event.preventDefault()
        }
        native.addEventListener("pointerleave") { event ->
            event as PointerEvent
            val b = (event.target as Element).getBoundingClientRect()
            if(delegate?.onPointerCancel(
                    event.pointerId,
                    (event.pageX - b.x) * dpr,
                    (event.pageY - b.y) * dpr,
                    (b.width) * dpr,
                    (b.height) * dpr
            ) == true) event.preventDefault()
        }
        setupResizeListener()
    }
}

public expect fun Canvas.onDelegateSet(delegate: CanvasDelegate?)

@InternalKiteUi
public expect fun Canvas.setupResizeListener()

//public actual var Canvas.delegate: CanvasDelegate?
//    get() = this.native.asDynamic().__ROCK_delegate__ as? CanvasDelegate
//    set(value) {
//        this.native.asDynamic().__ROCK_delegate__ = value
//        value?.let { value ->
//            value.invalidate = {
//                native.getContext("2d").apply {
//                    this as DrawingContext2D
//                    this.lineCap = CanvasLineCap.ROUND
//                    this.lineJoin = CanvasLineJoin.ROUND
//                    value.draw(this)
//                }
//            }
//            value.invalidate()
//        }
//    }

public actual typealias KeyCode = String
public actual object KeyCodes {
    public actual val left: KeyCode get() = "ArrowLeft"
    public actual val right: KeyCode get() = "ArrowRight"
    public actual val up: KeyCode get() = "ArrowUp"
    public actual val down: KeyCode get() = "ArrowDown"
    public actual fun letter(char: Char): KeyCode = "Key" + char.uppercase()
    public actual fun num(digit: Int): KeyCode = "Digit$digit"
    public actual fun numpad(digit: Int): KeyCode = "Numpad$digit"
    public actual val space: KeyCode get() = "Space"
    public actual val enter: KeyCode get() = "Enter"
    public actual val tab: KeyCode get() = "Tab"
    public actual val escape: KeyCode get() = "Escape"
    public actual val leftCtrl: KeyCode get() = "ControlLeft"
    public actual val rightCtrl: KeyCode get() = "ControlRight"
    public actual val leftShift: KeyCode get() = "ShiftLeft"
    public actual val rightShift: KeyCode get() = "ShiftRight"
    public actual val leftAlt: KeyCode get() = "AltLeft"
    public actual val rightAlt: KeyCode get() = "AltRight"
    public actual val equals: KeyCode get() = "Equal"
    public actual val dash: KeyCode get() = "Minus"
    public actual val backslash: KeyCode get() = "Backslash"
    public actual val leftBrace: KeyCode get() = "BracketLeft"
    public actual val rightBrace: KeyCode get() = "BracketRight"
    public actual val semicolon: KeyCode get() = "Semicolon"
    public actual val comma: KeyCode get() = "Comma"
    public actual val period: KeyCode get() = "Period"
}