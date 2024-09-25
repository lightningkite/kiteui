package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.view.View
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.dp
import com.lightningkite.kiteui.views.*

actual class Separator actual constructor(context: RContext): RView(context) {
    override val native = NSeparator(context.activity)

    override fun applyForeground(theme: Theme) {
        native.setBackgroundColor(theme.foreground.closestColor().colorInt())
    }

    override fun applyBackground(theme: Theme, fullyApply: Boolean) {

    }
}

class NSeparator(context: Context) : View(context) {
    private var thickness = 1.dp.value.toInt()
    private val containerHorizontal: Boolean get() = (parent as? SimplifiedLinearLayout)?.orientation == SimplifiedLinearLayout.HORIZONTAL
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val measuredWidth = if(containerHorizontal) thickness else measuredWidth
        val measuredHeight = if(containerHorizontal) measuredHeight else thickness
        setMeasuredDimension(measuredWidth, measuredHeight)
    }
}