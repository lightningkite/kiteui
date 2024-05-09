package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.view.View
import android.widget.Space
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.times
import com.lightningkite.kiteui.views.*
import kotlin.math.min
import kotlin.math.roundToInt


actual class Space actual constructor(context: RContext, val multiplier: Double): RView(context) {
    override val native = NSpace(context.activity)
    override fun applyForeground(theme: Theme) {
        native.mySuggestedMinimumWidth = (theme.spacing * multiplier).value.roundToInt()
        native.mySuggestedMinimumHeight = (theme.spacing * multiplier).value.roundToInt()
    }

    override fun applyBackground(theme: Theme, fullyApply: Boolean) {
        super.applyBackground(theme, fullyApply)
    }
}

class NSpace(context: Context): View(context) {
    var mySuggestedMinimumWidth = 1
    override fun getSuggestedMinimumWidth(): Int {
        return mySuggestedMinimumWidth
    }

    var mySuggestedMinimumHeight = 1
    override fun getSuggestedMinimumHeight(): Int {
        return mySuggestedMinimumHeight
    }
    /**
     * Compare to: [View.getDefaultSize]
     * If mode is AT_MOST, return the child size instead of the parent size
     * (unless it is too big).
     */
    private fun getDefaultSize2(size: Int, measureSpec: Int): Int {
        var result = size
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)

        when (specMode) {
            MeasureSpec.UNSPECIFIED -> result = size
            MeasureSpec.AT_MOST -> result = min(size.toDouble(), specSize.toDouble()).toInt()
            MeasureSpec.EXACTLY -> result = specSize
        }
        return result
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            getDefaultSize2(suggestedMinimumWidth, widthMeasureSpec),
            getDefaultSize2(suggestedMinimumHeight, heightMeasureSpec)
        )
    }
}