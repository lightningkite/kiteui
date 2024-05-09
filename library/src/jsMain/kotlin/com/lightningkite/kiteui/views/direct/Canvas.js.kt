package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ViewDsl
import ViewWriter
import com.lightningkite.kiteui.views.canvas.DrawingContext2D
import com.lightningkite.kiteui.views.element
import org.w3c.dom.CanvasLineCap
import org.w3c.dom.CanvasLineJoin
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.ROUND
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.WheelEvent

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NCanvas = HTMLCanvasElement

@ViewDsl
actual inline fun ViewWriter.canvasActual(crossinline setup: Canvas.() -> Unit): Unit = element<HTMLCanvasElement>("canvas") {
    val c = Canvas(this)
    setup(c)
    tabIndex = 1
    onkeydown = { event: KeyboardEvent ->
        if(c.delegate?.onKeyDown(event.code) == true)
            event.preventDefault()
    }
    onkeyup = { event: KeyboardEvent ->
        if(c.delegate?.onKeyUp(event.code) == true)
            event.preventDefault()
    }
    onwheel = { event: WheelEvent ->
        if(c.delegate?.onWheel(event.deltaX, event.deltaY, event.deltaZ) == true)
            event.preventDefault()
    }
    onpointerdown = { event ->
        val b = getBoundingClientRect()
        if(c.delegate?.onPointerDown(event.pointerId, event.pageX - b.x, event.pageY - b.y, b.width, b.height) == true)
            event.preventDefault()
    }
    onpointermove = { event ->
        val b = getBoundingClientRect()
        if(c.delegate?.onPointerMove(event.pointerId, event.pageX - b.x, event.pageY - b.y, b.width, b.height) == true)
            event.preventDefault()
    }
    onpointerup = { event ->
        val b = getBoundingClientRect()
        if(c.delegate?.onPointerUp(event.pointerId, event.pageX - b.x, event.pageY - b.y, b.width, b.height) == true)
            event.preventDefault()
    }
    onpointercancel = { event ->
        val b = getBoundingClientRect()
        if(c.delegate?.onPointerCancel(event.pointerId, event.pageX - b.x, event.pageY - b.y, b.width, b.height) == true)
            event.preventDefault()
    }
    onpointerleave = { event ->
        val b = getBoundingClientRect()
        if(c.delegate?.onPointerCancel(event.pointerId, event.pageX - b.x, event.pageY - b.y, b.width, b.height) == true)
            event.preventDefault()
    }
    ResizeObserver { _, _ ->
        if (width != scrollWidth || height != scrollHeight) {
            width = scrollWidth
            height = scrollHeight
        }
        c.delegate?.onResize(scrollWidth.toDouble(), scrollHeight.toDouble())
        c.delegate?.invalidate?.invoke()
    }.observe(this)
}

actual var Canvas.delegate: CanvasDelegate?
    get() = this.native.asDynamic().__ROCK_delegate__ as? CanvasDelegate
    set(value) {
        this.native.asDynamic().__ROCK_delegate__ = value
        value?.let { value ->
            value.invalidate = {
                native.getContext("2d").apply {
                    this as DrawingContext2D
                    this.lineCap = CanvasLineCap.ROUND
                    this.lineJoin = CanvasLineJoin.ROUND
                    value.draw(this)
                }
            }
            value.invalidate()
        }
    }

actual typealias KeyCode = String
actual object KeyCodes {
    actual val left: KeyCode get() = "ArrowLeft"
    actual val right: KeyCode get() = "ArrowRight"
    actual val up: KeyCode get() = "ArrowUp"
    actual val down: KeyCode get() = "ArrowDown"
    actual fun letter(char: Char): KeyCode = "Key" + char.uppercase()
    actual fun num(digit: Int): KeyCode = "Digit$digit"
    actual fun numpad(digit: Int): KeyCode = "Numpad$digit"
    actual val space: KeyCode get() = " "
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