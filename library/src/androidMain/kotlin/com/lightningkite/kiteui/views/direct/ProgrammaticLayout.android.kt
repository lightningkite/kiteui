package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import kotlin.math.roundToInt

actual class ProgrammaticLayout actual constructor(context: ElementContext) : NativeContainerElement(context), LinearLayoutElement {
    override val native: NProgrammaticLayout = NProgrammaticLayout(context.activity).apply {
        rview = this@ProgrammaticLayout
    }
    actual var delegate: ProgrammaticLayoutDelegate by native::delegate
    actual fun invalidateLayout() {
        native.silentRequestLayout()
    }

    actual override var gap: Dimension? = null
        set(value) {
            field = value
            native.spacingCurrentPx = gap?.px ?: theme.gap.px
        }

    actual override val spacingForChildCornerRadii: Dimension
        get() = super<LinearLayoutElement>.spacingForChildCornerRadii

    override fun refreshPadding() {
        val value = appliedPadding
        native.paddingTopCurrentPx = value.top.canvasUnits
        native.paddingLeftCurrentPx = value.left.canvasUnits
        native.paddingRightCurrentPx = value.right.canvasUnits
        native.paddingBottomCurrentPx = value.bottom.canvasUnits
        native.silentRequestLayout()
    }

    override fun nativeApplyTheme(theme: ThemeAndBack) { super.nativeApplyTheme(theme); val theme = theme.theme
        native.spacingCurrentPx = gap?.px ?: theme.gap.px
    }
}

class NProgrammaticLayout(context: Context) : ViewGroup(context) {
    var spacingCurrentPx: Double = 0.0
    var paddingTopCurrentPx: Double = 0.0
    var paddingLeftCurrentPx: Double = 0.0
    var paddingRightCurrentPx: Double = 0.0
    var paddingBottomCurrentPx: Double = 0.0
    private var currentSize: Size = Size.Zero

    var delegate: ProgrammaticLayoutDelegate = ProgrammaticLayoutDelegate.AllFull
        set(value) {
            field = value
            silentRequestLayout()
        }
    lateinit var rview: ProgrammaticLayout
    private val inProgress = object : ProgrammingLayoutInProgress {
        override val within: Size
            get() = currentSize
        override val gap: Double get() = spacingCurrentPx
        override val padding: Double get() = paddingLeftCurrentPx
        override val paddingTop: Double get() = paddingTopCurrentPx
        override val paddingLeft: Double get() = paddingLeftCurrentPx
        override val paddingRight: Double get() = paddingRightCurrentPx
        override val paddingBottom: Double get() = paddingBottomCurrentPx

        override fun measure(child: Element, sizeConstraint: Size): Size {
            child.underlyingNativeElement.native.measure(
                MeasureSpec.makeMeasureSpec(sizeConstraint.width.roundToInt(), MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(sizeConstraint.height.roundToInt(), MeasureSpec.AT_MOST)
            )
            return Size(child.underlyingNativeElement.native.measuredWidth.toDouble(), child.underlyingNativeElement.native.measuredHeight.toDouble())
        }

        override fun place(child: Element, left: Double, top: Double, right: Double, bottom: Double) {
            placed += child.underlyingNativeElement.native
            child.underlyingNativeElement.native.measure(
                MeasureSpec.makeMeasureSpec((right - left).roundToInt(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec((bottom - top).roundToInt(), MeasureSpec.EXACTLY),
            )
            child.underlyingNativeElement.native.layout(left.roundToInt(), top.roundToInt(), right.roundToInt(), bottom.roundToInt())
        }

        override fun existingPosition(child: Element): Rect = Rect(
            child.underlyingNativeElement.native.left.toDouble(),
            child.underlyingNativeElement.native.top.toDouble(),
            child.underlyingNativeElement.native.right.toDouble(),
            child.underlyingNativeElement.native.bottom.toDouble()
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        debugPrint { "onMeasure on ProgrammaticLayout" }
        val newWidth = when (View.MeasureSpec.getMode(widthMeasureSpec)) {
            View.MeasureSpec.AT_MOST -> View.MeasureSpec.getSize(widthMeasureSpec).toDouble()
            View.MeasureSpec.EXACTLY -> View.MeasureSpec.getSize(widthMeasureSpec).toDouble()
            else -> 100000.0
        }
        val newHeight = when (View.MeasureSpec.getMode(heightMeasureSpec)) {
            View.MeasureSpec.AT_MOST -> View.MeasureSpec.getSize(heightMeasureSpec).toDouble()
            View.MeasureSpec.EXACTLY -> View.MeasureSpec.getSize(heightMeasureSpec).toDouble()
            else -> 100000.0
        }
        val s = Size(newWidth, newHeight)
        currentSize = s
        val r = delegate.measure(rview, inProgress, s)
        setMeasuredDimension(r.width.roundToInt(), r.height.roundToInt())
    }
    val placed = HashSet<View>()

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if(r - l == 0 || b - t == 0) return
        debugPrint { "onLayout on ProgrammaticLayout" }
        placed.clear()
        val s = Size((r - l).toDouble(), (b - t).toDouble())
        currentSize = s
        delegate.layout(rview, inProgress, s)
        (children - placed).forEach {
            // Force layout missed cells to satisfy Android
            // If you don't do this, requestLayout won't work.
            it.measure(
                MeasureSpec.makeMeasureSpec((it.right - it.left), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec((it.bottom - it.top), MeasureSpec.EXACTLY),
            )
            it.layout(it.left, it.top, it.right, it.bottom)
        }
    }

    fun silentRequestLayout() {
        if(isInLayout) return
        super.requestLayout()
    }

    override fun requestLayout() {
        debugPrint { "requestLayout on ProgrammaticLayout" }
        if(isInLayout) {
            return
        }
        super.requestLayout()
    }
}