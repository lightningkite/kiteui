package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import com.lightningkite.kiteui.ConsoleRoot
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.reactive.BasicListenable
import com.lightningkite.kiteui.reactive.LateInitProperty
import com.lightningkite.kiteui.reactive.Listenable
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import java.util.WeakHashMap
import kotlin.math.max
import kotlin.math.roundToInt

actual class ProgrammaticLayout actual constructor(context: RContext) : RView(context) {
    override val native: NProgrammaticLayout = NProgrammaticLayout(context.activity).apply {
        rview = this@ProgrammaticLayout
    }
    actual var delegate: ProgrammaticLayoutDelegate by native::delegate
    actual fun invalidateLayout() {
        native.requestLayout()
    }
}

class NProgrammaticLayout(context: Context) : ViewGroup(context) {
    var delegate: ProgrammaticLayoutDelegate = ProgrammaticLayoutDelegate.AllFull
        set(value) {
            field = value
            requestLayout()
        }
    lateinit var rview: ProgrammaticLayout
    private val inProgress = object : ProgrammingLayoutInProgress {
        override fun measure(child: RView, sizeConstraint: Size): Size {
            child.native.measure(
                MeasureSpec.makeMeasureSpec(sizeConstraint.width.roundToInt(), MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(sizeConstraint.height.roundToInt(), MeasureSpec.AT_MOST)
            )
            return Size(child.native.measuredWidth.toDouble(), child.native.measuredHeight.toDouble())
        }

        override fun place(child: RView, left: Double, top: Double, right: Double, bottom: Double) {
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

    override fun requestLayout() {
        ConsoleRoot.tag("ProgrammaticLayout.android").log("Layout requested")
        super.requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
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

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if(r - l == 0 || b - t == 0) return
        delegate.layout(rview, inProgress, Size((r - l).toDouble(), (b - t).toDouble()))
    }
}