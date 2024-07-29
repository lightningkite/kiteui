package com.lightningkite.kiteui.views.direct

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.*
import android.widget.FrameLayout
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.canvas.DrawingContext2DImpl
import kotlin.math.min


actual class Canvas actual constructor(context: RContext): RView(context) {
    override val native = NCanvas(context.activity)

    actual var delegate: CanvasDelegate?
        get() = native.delegate
        set(value) { native.delegate = value }
}

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual class NCanvas @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var delegate: CanvasDelegate? = null
        set(value) {
            field?.invalidate = {}
            field = value
            field?.invalidate = {
                this.invalidate()
            }
        }

    init {
        setWillNotDraw(false)
    }

//    var accessibilityView: View? = null

    private data class Touch(
        var x: Float,
        var y: Float,
        var id: Int
    )

    @SuppressLint("UseSparseArrays")
    private val touches = HashMap<Int, Touch>()

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return delegate?.onKeyDown(keyCode) ?: false
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return delegate?.onKeyUp(keyCode) ?: false
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if(event.source and InputDevice.SOURCE_CLASS_POINTER != 0) {
            if(event.action == MotionEvent.ACTION_SCROLL) {
                return delegate?.onWheel(
                    event.getAxisValue(MotionEvent.AXIS_HSCROLL).toDouble(),
                    event.getAxisValue(MotionEvent.AXIS_VSCROLL).toDouble(),
                    event.getAxisValue(MotionEvent.AXIS_SCROLL).toDouble(),
                ) ?: false
            }
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        parent.requestDisallowInterceptTouchEvent(true)
        var takenCareOf = false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val pointerId = event.getPointerId(event.actionIndex)
                val touch = Touch(
                    x = event.getX(event.actionIndex),
                    y = event.getY(event.actionIndex),
                    id = pointerId
                )
                touches[pointerId] = touch
                delegate?.onPointerDown(touch.id, touch.x.toDouble(), touch.y.toDouble(), width.toDouble(), height.toDouble())?.let { takenCareOf = takenCareOf || it }
            }

            MotionEvent.ACTION_MOVE -> {
                for (pointerIndex in 0 until event.pointerCount) {
                    val pointerId = event.getPointerId(pointerIndex)
                    val touch = touches[pointerId]
                    if (touch != null) {
                        touch.x = event.getX(pointerIndex)
                        touch.y = event.getY(pointerIndex)
                        delegate?.onPointerMove(touch.id, touch.x.toDouble(), touch.y.toDouble(), width.toDouble(), height.toDouble())?.let { takenCareOf = takenCareOf || it }
                    }
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                val pointerId = event.getPointerId(event.actionIndex)
                touches.remove(pointerId)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val pointerId = event.getPointerId(event.actionIndex)
                val touch = touches.remove(pointerId)
                if (touch != null) {
                    delegate?.onPointerUp(touch.id, touch.x.toDouble(), touch.y.toDouble(), width.toDouble(), height.toDouble())?.let { takenCareOf = takenCareOf || it }
                }
            }
        }
        return takenCareOf
    }

    private val metrics = context.resources.displayMetrics
    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)
        delegate?.draw(DrawingContext2DImpl(canvas))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            else -> min(
                100,
                widthSize
            )
        }
        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            else -> min(
                100,
                heightSize
            )
        }
        setMeasuredDimension(
            width,
            height
        )
    }
}

actual typealias KeyCode = Int
actual object KeyCodes {
    actual val left: KeyCode get() = KeyEvent.KEYCODE_DPAD_LEFT
    actual val right: KeyCode get() = KeyEvent.KEYCODE_DPAD_RIGHT
    actual val up: KeyCode get() = KeyEvent.KEYCODE_DPAD_UP
    actual val down: KeyCode get() = KeyEvent.KEYCODE_DPAD_DOWN
    actual fun letter(char: Char): KeyCode = KeyEvent.KEYCODE_A + char.code
    actual fun num(digit: Int): KeyCode = KeyEvent.KEYCODE_0 + digit
    actual fun numpad(digit: Int): KeyCode = KeyEvent.KEYCODE_NUMPAD_0 + digit
    actual val space: KeyCode get() = KeyEvent.KEYCODE_SPACE
    actual val enter: KeyCode get() = KeyEvent.KEYCODE_ENTER
    actual val tab: KeyCode get() = KeyEvent.KEYCODE_TAB
    actual val escape: KeyCode get() = KeyEvent.KEYCODE_ESCAPE
    actual val leftCtrl: KeyCode get() = KeyEvent.KEYCODE_CTRL_LEFT
    actual val rightCtrl: KeyCode get() = KeyEvent.KEYCODE_CTRL_RIGHT
    actual val leftShift: KeyCode get() = KeyEvent.KEYCODE_SHIFT_LEFT
    actual val rightShift: KeyCode get() = KeyEvent.KEYCODE_SHIFT_RIGHT
    actual val leftAlt: KeyCode get() = KeyEvent.KEYCODE_ALT_LEFT
    actual val rightAlt: KeyCode get() = KeyEvent.KEYCODE_ALT_RIGHT
    actual val equals: KeyCode get() = KeyEvent.KEYCODE_EQUALS
    actual val dash: KeyCode get() = KeyEvent.KEYCODE_MINUS
    actual val backslash: KeyCode get() = KeyEvent.KEYCODE_BACKSLASH
    actual val leftBrace: KeyCode get() = KeyEvent.KEYCODE_LEFT_BRACKET
    actual val rightBrace: KeyCode get() = KeyEvent.KEYCODE_RIGHT_BRACKET
    actual val semicolon: KeyCode get() = KeyEvent.KEYCODE_SEMICOLON
    actual val comma: KeyCode get() = KeyEvent.KEYCODE_COMMA
    actual val period: KeyCode get() = KeyEvent.KEYCODE_PERIOD
}