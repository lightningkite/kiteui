package com.lightningkite.kiteui.views.direct

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.*
import android.widget.FrameLayout
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.canvas.DrawingContext2DImpl
import kotlin.math.min


public actual class Canvas actual constructor(context: ElementContext): NativeElement(context) {
    override val native: NCanvas = NCanvas(context.activity)

    public actual var delegate: CanvasDelegate?
        get() = native.delegate
        set(value) {
            native.delegate = value
            value?.theme = themeAndBack.theme
            delegate?.invalidate?.invoke()
        }

    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.nativeApplyTheme(theme)
        delegate?.theme = theme.theme
        delegate?.invalidate?.invoke()
    }
}

public class NCanvas @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    internal var delegate: CanvasDelegate? = null
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