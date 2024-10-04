package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.views.*


actual class Separator actual constructor(context: RContext): RView(context) {
    override val native = NSeparator(context.activity).apply {
        minimumWidth = 1
        minimumHeight = 1
    }

    override fun applyBackground(theme: Theme, fullyApply: Boolean) {}
    override fun applyForeground(theme: Theme) {
        val c = native.parent as? SimplifiedLinearLayout
        val v = native
        v.setBackgroundColor(theme.foreground.closestColor().colorInt())
        val size = theme.outlineWidth.value.coerceAtLeast(1f).toInt()
        v.thickness = size
        (v.parent as? SimplifiedLinearLayout)?.let {
            lparams.run {
                width = if(it.orientation == SimplifiedLinearLayout.HORIZONTAL) size else ViewGroup.LayoutParams.MATCH_PARENT
                height = if(it.orientation == SimplifiedLinearLayout.HORIZONTAL) ViewGroup.LayoutParams.MATCH_PARENT else size
            }
        } ?: (v.parent as? DesiredSizeView)?.let {
            if(c?.orientation == SimplifiedLinearLayout.HORIZONTAL) {
                it.constraints = it.constraints.copy(width = size.px)
            } else {
                it.constraints = it.constraints.copy(height = size.px)
            }
        }
    }
}

class NSeparator(context: Context) : View(context) {
    var thickness: Int = 1
        set(value) {
            field = value
            requestLayout()
        }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
//        setMeasuredDimension(if(containerHorizontal) thickness else measuredWidth, if(!containerHorizontal) measuredHeight else thickness)
        setMeasuredDimension(
            when(MeasureSpec.getMode(widthMeasureSpec)) {
                MeasureSpec.EXACTLY -> MeasureSpec.getSize(widthMeasureSpec)
                else -> thickness
            },
            when(MeasureSpec.getMode(heightMeasureSpec)) {
                MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec)
                else -> thickness
            }
        )
    }
}