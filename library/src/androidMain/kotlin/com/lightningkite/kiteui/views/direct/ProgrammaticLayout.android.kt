package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.reactive.LateInitProperty
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import java.util.WeakHashMap
import kotlin.math.max
import kotlin.math.roundToInt

actual class ProgrammaticLayout actual constructor(context: RContext) : RView(context) {
    override val native: NProgrammaticLayout = NProgrammaticLayout(context.activity)

    actual fun measureChild(
        child: RView,
        sizeConstraint: Size
    ): Size {
        child.native.measure(
            View.MeasureSpec.makeMeasureSpec(sizeConstraint.width.toInt(), View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(sizeConstraint.height.toInt(), View.MeasureSpec.AT_MOST)
        )
        return Size(child.native.measuredWidth.toDouble(), child.native.measuredHeight.toDouble())
    }

    actual fun setChildBounds(child: RView, rect: Rect) {
        native.childBounds[child.native] = rect
        native.requestLayout()
    }

    actual val externalSizeLimit: Readable<Size> = native.externalSizeLimit
}

class NProgrammaticLayout(context: Context): ViewGroup(context) {
    val childBounds = WeakHashMap<View, Rect>()
    val externalSizeLimit = LateInitProperty<Size>()
    private var lastWidth: Double = 0.0
    private var lastHeight: Double = 0.0
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val newWidth = when(View.MeasureSpec.getMode(widthMeasureSpec)) {
            View.MeasureSpec.AT_MOST -> View.MeasureSpec.getSize(widthMeasureSpec).toDouble()
            View.MeasureSpec.EXACTLY -> View.MeasureSpec.getSize(widthMeasureSpec).toDouble()
            else -> 100000.0
        }
        val newHeight = when(View.MeasureSpec.getMode(heightMeasureSpec)) {
            View.MeasureSpec.AT_MOST -> View.MeasureSpec.getSize(heightMeasureSpec).toDouble()
            View.MeasureSpec.EXACTLY -> View.MeasureSpec.getSize(heightMeasureSpec).toDouble()
            else -> 100000.0
        }
        if(newWidth != lastWidth || newHeight != lastHeight) {
            lastWidth = newWidth
            lastHeight = newHeight
            externalSizeLimit.value = Size(
                width = newWidth,
                height = newHeight,
            )
        }
//        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec))
        setMeasuredDimension(
            max(MeasureSpec.getSize(widthMeasureSpec), children.maxOfOrNull { it.width } ?: 0),
            max(MeasureSpec.getSize(heightMeasureSpec), children.maxOfOrNull { it.height } ?: 0),
        )
    }
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        childBounds.forEach { (view, rect) ->
            view.measure(
                MeasureSpec.makeMeasureSpec(rect.width.roundToInt(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(rect.height.roundToInt(), MeasureSpec.EXACTLY),
            )
            view.layout(rect.left.roundToInt(), rect.top.roundToInt(), rect.right.roundToInt(), rect.bottom.roundToInt())
        }
    }
}