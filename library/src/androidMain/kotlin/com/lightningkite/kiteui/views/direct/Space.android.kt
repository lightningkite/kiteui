package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.view.View
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.times
import com.lightningkite.kiteui.views.*
import kotlin.math.min
import kotlin.math.roundToInt


public actual class Space actual constructor(context: ElementContext, public val multiplier: Double): NativeElement(context) {
    override val native: NSpace = NSpace(context.activity)
    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.nativeApplyTheme(theme)
        native.mySuggestedMinimumWidth = (theme.theme.gap * multiplier).value.roundToInt()
        native.mySuggestedMinimumHeight = (theme.theme.gap * multiplier).value.roundToInt()
    }
}

public class NSpace(context: Context): View(context) {
    public var mySuggestedMinimumWidth: Int = 1
    override fun getSuggestedMinimumWidth(): Int {
        return mySuggestedMinimumWidth
    }

    public var mySuggestedMinimumHeight: Int = 1
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