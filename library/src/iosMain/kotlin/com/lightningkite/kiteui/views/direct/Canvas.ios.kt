@file:OptIn(ExperimentalForeignApi::class)

package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ViewDsl
import ViewWriter
import com.lightningkite.kiteui.views.canvas.DrawingContext2DImpl
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.QuartzCore.CATransaction
import platform.UIKit.*
import platform.darwin.*

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NCanvas = CanvasView

@ViewDsl
actual inline fun ViewWriter.canvasActual(crossinline setup: Canvas.() -> Unit): Unit = element(CanvasView()) {
    setup(Canvas(this))
}


actual var Canvas.delegate: CanvasDelegate?
    get() = native.delegate
    set(value) {
        native.delegate = value
    }

actual typealias KeyCode = String

actual object KeyCodes {
    actual val left: KeyCode get() = UIKeyInputLeftArrow
    actual val right: KeyCode get() = UIKeyInputRightArrow
    actual val up: KeyCode get() = UIKeyInputUpArrow
    actual val down: KeyCode get() = UIKeyInputDownArrow
    actual fun letter(char: Char): KeyCode = char.lowercase()
    actual fun num(digit: Int): KeyCode = digit.toString()
    actual fun numpad(digit: Int): KeyCode = digit.toString()
    actual val space: KeyCode get() = " "
    actual val enter: KeyCode get() = "\n"
    actual val tab: KeyCode get() = "\t"
    actual val escape: KeyCode get() = UIKeyInputEscape
    actual val leftCtrl: KeyCode get() = ""
    actual val rightCtrl: KeyCode get() = ""
    actual val leftShift: KeyCode get() = ""
    actual val rightShift: KeyCode get() = ""
    actual val leftAlt: KeyCode get() = ""
    actual val rightAlt: KeyCode get() = ""
    actual val equals: KeyCode get() = "="
    actual val dash: KeyCode get() = "-"
    actual val backslash: KeyCode get() = "\\"
    actual val leftBrace: KeyCode get() = "["
    actual val rightBrace: KeyCode get() = "]"
    actual val semicolon: KeyCode get() = ";"
    actual val comma: KeyCode get() = ","
    actual val period: KeyCode get() = "."
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class CanvasView : UIView(CGRectZero.readValue()) {
    init {
        opaque = false
        setUserInteractionEnabled(true)
        setMultipleTouchEnabled(true)
    }


    init {
        clipsToBounds = true
    }

    var delegate: CanvasDelegate? = null
        set(value) {
            field?.invalidate = {}
            field = value
            field?.invalidate = {
//                this.startRefresh()
                setNeedsDisplay()
            }
        }

    // todo mouse wheel

    val touchIds = HashMap<UITouch, Int>()
    var currentTouchId: Int = 0
    override fun touchesBegan(touches: Set<*>, withEvent: UIEvent?) {
        if (!handle(touches as Set<UITouch>))
            super.touchesBegan(touches, withEvent)
    }

    override fun touchesMoved(touches: Set<*>, withEvent: UIEvent?) {
        if (!handle(touches as Set<UITouch>))
            super.touchesMoved(touches, withEvent)
    }

    override fun touchesEnded(touches: Set<*>, withEvent: UIEvent?) {
        if (!handle(touches as Set<UITouch>))
            super.touchesEnded(touches, withEvent)
    }

    override fun touchesCancelled(touches: Set<*>, withEvent: UIEvent?) {
        if (!handle(touches as Set<UITouch>))
            super.touchesCancelled(touches, withEvent)
    }

    override fun pressesBegan(presses: Set<*>, withEvent: UIPressesEvent?) {
        var handled = false
        for (press in presses) {
            press as UIPress
            val key = press.key?.charactersIgnoringModifiers ?: continue
            handled = handled && (delegate?.onKeyDown(key) ?: false)
        }
        if (!handled) super.pressesBegan(presses, withEvent)
    }

    override fun pressesEnded(presses: Set<*>, withEvent: UIPressesEvent?) {
        var handled = false
        for (press in presses) {
            press as UIPress
            val key = press.key?.charactersIgnoringModifiers ?: continue
            handled = handled && (delegate?.onKeyUp(key) ?: false)
        }
        if (!handled) super.pressesBegan(presses, withEvent)
    }

    private fun handle(touches: Set<UITouch>): Boolean {
        var handled = false
        for (touch in touches) {
            val loc = touch.locationInView(this)
            when (touch.phase) {
                UITouchPhase.UITouchPhaseBegan -> {
                    val id = currentTouchId
                    currentTouchId += 1
                    touchIds[touch] = id
                    handled = handled || (delegate?.onPointerDown(
                        id = id,
                        x = (loc.useContents { x }),  // * UIScreen.mainScreen.scale,
                        y = (loc.useContents { y }),  // * UIScreen.mainScreen.scale,
                        width = (frame.useContents { size.width }),  // * UIScreen.mainScreen.scale,
                        height = (frame.useContents { size.height }),  // * UIScreen.mainScreen.scale
                    ) ?: false)
                }

                UITouchPhase.UITouchPhaseMoved -> {
                    touchIds[touch]?.let { id ->
                        handled = handled || (delegate?.onPointerMove(
                            id = id,
                            x = (loc.useContents { x }),  // * UIScreen.mainScreen.scale,
                            y = (loc.useContents { y }),  // * UIScreen.mainScreen.scale,
                            width = (frame.useContents { size.width }),  // * UIScreen.mainScreen.scale,
                            height = (frame.useContents { size.height }),  // * UIScreen.mainScreen.scale
                        ) ?: false)
                    }
                }

                UITouchPhase.UITouchPhaseCancelled -> {
                    touchIds[touch]?.let { id ->
                        handled = handled || (delegate?.onPointerCancel(
                            id = id,
                            x = (loc.useContents { x }),  // * UIScreen.mainScreen.scale,
                            y = (loc.useContents { y }),  // * UIScreen.mainScreen.scale,
                            width = (frame.useContents { size.width }),  // * UIScreen.mainScreen.scale,
                            height = (frame.useContents { size.height }),  // * UIScreen.mainScreen.scale
                        ) ?: false)
                    }
                    touchIds.remove(touch)
                }

                UITouchPhase.UITouchPhaseEnded -> {
                    touchIds[touch]?.let { id ->
                        handled = handled || (delegate?.onPointerUp(
                            id = id,
                            x = (loc.useContents { x }),  // * UIScreen.mainScreen.scale,
                            y = (loc.useContents { y }),  // * UIScreen.mainScreen.scale,
                            width = (frame.useContents { size.width }),  // * UIScreen.mainScreen.scale,
                            height = (frame.useContents { size.height }),  // * UIScreen.mainScreen.scale
                        ) ?: false)
                    }
                    touchIds.remove(touch)
                }

                else -> {}
            }
        }
        return handled
    }

//    val stage = AtomicInt(0)
//    var needsAnotherRender = false
//    private var drawCount = 0
//    private var started = CFAbsoluteTimeGetCurrent()
//    private var lastMessage = CFAbsoluteTimeGetCurrent()
//
//    fun startRefresh() {
//        val width = frame.useContents { size.width }
//        val height = frame.useContents { size.height }
//        if (width == 0.0) return
//        if (height == 0.0) return
//        if (stage.compareAndSet(0, 1)) {
//            dispatch_async(queue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH.toLong(), 0u), block = {
//                this.needsAnotherRender = false
//                this.drawCount += 1
//                val start = CFAbsoluteTimeGetCurrent()
//                UIGraphicsBeginImageContextWithOptions(
//                    CGSizeMake(
//                        width,
//                        height
//                    ), false, 0.0
//                )
//                UIGraphicsGetCurrentContext()?.let {
////                    CGContextScaleCTM(it, 1 / UIScreen.mainScreen.scale, 1 / UIScreen.mainScreen.scale)
//                    with(
//                        DrawingContext2DImpl(
//                            it,
//                            width,
//                            height
//                        )
//                    ) {
//                        delegate?.draw(this)
//                    }
//                    CGContextSetRGBFillColor(it, 1.0, 0.0, 0.0, 1.0)
//                    CGContextFillRect(it, CGRectMake(0.0, 0.0, 100.0, 100.0))
//                } ?: println("NO CONTEXT WARNING")
//                val image = UIGraphicsGetImageFromCurrentImageContext()
//                UIGraphicsEndImageContext()
//
//                if (image != null) {
//                    dispatch_async(queue = dispatch_get_main_queue(), block = {
////                        layer.contents = image.CGImage
////                        CATransactionWithDisabledActions {
////                            imageLayer.contents = image.CGImage
////                        }
//                        debugUIImageView.image = image
////                        layer.setNeedsDisplay()
//                        setNeedsDisplay()
////                        imageLayer.setNeedsDisplay()
//                        stage.value = 0
//                        if (needsAnotherRender) {
//                            startRefresh()
//                        }
//                    })
//                }
//                if (start - this.lastMessage > 5) {
//                    val end = CFAbsoluteTimeGetCurrent()
//                    this.lastMessage = end
//                    println("CustomView draw took ${end - start} seconds, ${drawCount / (end - started)} FPS")
//                }
//            })
//        } else {
//            needsAnotherRender = true
//        }
//    }
//
//    override fun layoutSubviews() {
//        super.layoutSubviews()
////        imageLayer.frame = bounds
//        debugUIImageView.setFrame(bounds)
//        startRefresh()
//    }

    override fun drawRect(rect: CValue<CGRect>) {
        with(DrawingContext2DImpl(UIGraphicsGetCurrentContext()!!, rect.useContents { this.size.width }, rect.useContents { this.size.height })) {
            delegate?.draw(this)
        }
    }

    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> {
        return delegate?.let { delegate ->
            val width = size.useContents { width }
            val height = size.useContents { height }
            return CGSizeMake(
                width = delegate.sizeThatFitsWidth(width, height),
                height = delegate.sizeThatFitsHeight(width, height)
            )
        } ?: super.sizeThatFits(size)
    }
}

inline fun CATransactionWithDisabledActions(action: () -> Unit) {
    CATransaction.begin()
    CATransaction.setDisableActions(true)
    try {
        action()
    } finally {
        CATransaction.commit()
    }
}