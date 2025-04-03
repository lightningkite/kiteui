package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.viewDebugTarget
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import kotlin.math.roundToInt

actual class ProgrammaticLayout actual constructor(context: RContext) : RView(context) {
    override val cannotBeCovered: Boolean get() = false
    override val native: NProgrammaticLayout = NProgrammaticLayout(context.activity).apply {
        rview = this@ProgrammaticLayout
    }
    actual var delegate: ProgrammaticLayoutDelegate by native::delegate
    actual fun invalidateLayout() {
        native.silentRequestLayout()
    }

    override var paddingByEdge: Edges?
        get() = super.paddingByEdge
        set(value) {
            super.paddingByEdge = value
            native.paddingTopCurrentPx = (value ?: theme.padding.takeIf { themeAndBack.padding })?.top?.px ?: 0.0
            native.paddingLeftCurrentPx = (value ?: theme.padding.takeIf { themeAndBack.padding })?.left?.px ?: 0.0
            native.paddingRightCurrentPx = (value ?: theme.padding.takeIf { themeAndBack.padding })?.right?.px ?: 0.0
            native.paddingBottomCurrentPx = (value ?: theme.padding.takeIf { themeAndBack.padding })?.bottom?.px ?: 0.0
        }
    override var spacing: Dimension?
        get() = super.spacing
        set(value) {
            super.spacing = value
            native.spacingCurrentPx = spacing?.px ?: theme.gap.px
        }

    override fun applyTheme(theme: ThemeAndBack) { super.applyTheme(theme); val theme = theme.theme
        native.spacingCurrentPx = spacing?.px ?: theme.gap.px
        native.paddingTopCurrentPx = (paddingByEdge ?: theme.padding.takeIf { themeAndBack.padding })?.top?.px ?: 0.0
        native.paddingLeftCurrentPx = (paddingByEdge ?: theme.padding.takeIf { themeAndBack.padding })?.left?.px ?: 0.0
        native.paddingRightCurrentPx = (paddingByEdge ?: theme.padding.takeIf { themeAndBack.padding })?.right?.px ?: 0.0
        native.paddingBottomCurrentPx = (paddingByEdge ?: theme.padding.takeIf { themeAndBack.padding })?.bottom?.px ?: 0.0
    }
}

class NProgrammaticLayout(context: Context) : ViewGroup(context) {
    var spacingCurrentPx: Double = 0.0
    var paddingTopCurrentPx: Double = 0.0
    var paddingLeftCurrentPx: Double = 0.0
    var paddingRightCurrentPx: Double = 0.0
    var paddingBottomCurrentPx: Double = 0.0

    var delegate: ProgrammaticLayoutDelegate = ProgrammaticLayoutDelegate.AllFull
        set(value) {
            field = value
            silentRequestLayout()
        }
    lateinit var rview: ProgrammaticLayout
    private val inProgress = object : ProgrammingLayoutInProgress {
        override val spacing: Double get() = spacingCurrentPx
        override val padding: Double get() = paddingLeftCurrentPx
        override val paddingTop: Double get() = paddingTopCurrentPx
        override val paddingLeft: Double get() = paddingLeftCurrentPx
        override val paddingRight: Double get() = paddingRightCurrentPx
        override val paddingBottom: Double get() = paddingBottomCurrentPx

        override fun measure(child: RView, sizeConstraint: Size): Size {
            child.native.measure(
                MeasureSpec.makeMeasureSpec(sizeConstraint.width.roundToInt(), MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(sizeConstraint.height.roundToInt(), MeasureSpec.AT_MOST)
            )
            return Size(child.native.measuredWidth.toDouble(), child.native.measuredHeight.toDouble())
        }

        override fun place(child: RView, left: Double, top: Double, right: Double, bottom: Double) {
            placed += child.native
            child.native.measure(
                MeasureSpec.makeMeasureSpec((right - left).roundToInt(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec((bottom - top).roundToInt(), MeasureSpec.EXACTLY),
            )
            child.native.layout(left.roundToInt(), top.roundToInt(), right.roundToInt(), bottom.roundToInt())
        }

        override fun existingPosition(child: RView): Rect = Rect(
            child.native.left.toDouble(),
            child.native.top.toDouble(),
            child.native.right.toDouble(),
            child.native.bottom.toDouble()
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if(viewDebugTarget?.native == this) println("onMeasure on ProgrammaticLayout")
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
        val r = delegate.measure(rview, inProgress, Size(newWidth, newHeight))
        setMeasuredDimension(r.width.roundToInt(), r.height.roundToInt())
    }
    val placed = HashSet<View>()

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if(r - l == 0 || b - t == 0) return
        if(viewDebugTarget?.native == this) println("onLayout on ProgrammaticLayout")
        placed.clear()
        delegate.layout(rview, inProgress, Size((r - l).toDouble(), (b - t).toDouble()))
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
        if(viewDebugTarget?.native == this) println("requestLayout on ProgrammaticLayout")
        if(isInLayout) {
            return
        }
        super.requestLayout()
    }
}