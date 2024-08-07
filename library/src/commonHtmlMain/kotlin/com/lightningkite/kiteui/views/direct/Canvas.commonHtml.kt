package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.*
import com.lightningkite.kiteui.views.*


actual class Canvas actual constructor(context: RContext): RView(context) {
    actual var delegate: CanvasDelegate? = null
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
        native.addEventListener("pointerdown") { event ->
            event as PointerEvent
            val b = (event.target as Element).getBoundingClientRect()
            if(delegate?.onPointerDown(event.pointerId, event.pageX - b.x, event.pageY - b.y, b.width, b.height) == true)
                event.preventDefault()
        }
        native.addEventListener("pointermove") { event ->
            event as PointerEvent
            val b = (event.target as Element).getBoundingClientRect()
            if(delegate?.onPointerMove(event.pointerId, event.pageX - b.x, event.pageY - b.y, b.width, b.height) == true)
                event.preventDefault()
        }
        native.addEventListener("pointerup") { event ->
            event as PointerEvent
            val b = (event.target as Element).getBoundingClientRect()
            if(delegate?.onPointerUp(event.pointerId, event.pageX - b.x, event.pageY - b.y, b.width, b.height) == true)
                event.preventDefault()
        }
        native.addEventListener("pointercancel") { event ->
            event as PointerEvent
            val b = (event.target as Element).getBoundingClientRect()
            if(delegate?.onPointerCancel(event.pointerId, event.pageX - b.x, event.pageY - b.y, b.width, b.height) == true)
                event.preventDefault()
        }
        native.addEventListener("pointerleave") { event ->
            event as PointerEvent
            val b = (event.target as Element).getBoundingClientRect()
            if(delegate?.onPointerCancel(event.pointerId, event.pageX - b.x, event.pageY - b.y, b.width, b.height) == true)
                event.preventDefault()
        }
        setupResizeListener()
    }
}

expect fun Canvas.onDelegateSet(delegate: CanvasDelegate?)

expect fun Canvas.setupResizeListener()

//actual var Canvas.delegate: CanvasDelegate?
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

actual typealias KeyCode = String
actual object KeyCodes {
    actual val left: KeyCode get() = "ArrowLeft"
    actual val right: KeyCode get() = "ArrowRight"
    actual val up: KeyCode get() = "ArrowUp"
    actual val down: KeyCode get() = "ArrowDown"
    actual fun letter(char: Char): KeyCode = "Key" + char.uppercase()
    actual fun num(digit: Int): KeyCode = "Digit$digit"
    actual fun numpad(digit: Int): KeyCode = "Numpad$digit"
    actual val space: KeyCode get() = "Space"
    actual val enter: KeyCode get() = "Enter"
    actual val tab: KeyCode get() = "Tab"
    actual val escape: KeyCode get() = "Escape"
    actual val leftCtrl: KeyCode get() = "ControlLeft"
    actual val rightCtrl: KeyCode get() = "ControlRight"
    actual val leftShift: KeyCode get() = "ShiftLeft"
    actual val rightShift: KeyCode get() = "ShiftRight"
    actual val leftAlt: KeyCode get() = "AltLeft"
    actual val rightAlt: KeyCode get() = "AltRight"
    actual val equals: KeyCode get() = "Equal"
    actual val dash: KeyCode get() = "Minus"
    actual val backslash: KeyCode get() = "Backslash"
    actual val leftBrace: KeyCode get() = "BracketLeft"
    actual val rightBrace: KeyCode get() = "BracketRight"
    actual val semicolon: KeyCode get() = "Semicolon"
    actual val comma: KeyCode get() = "Comma"
    actual val period: KeyCode get() = "Period"
}