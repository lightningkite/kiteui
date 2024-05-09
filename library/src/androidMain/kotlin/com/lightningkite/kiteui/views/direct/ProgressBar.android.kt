package com.lightningkite.kiteui.views.direct

import android.content.res.ColorStateList
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import kotlin.math.roundToInt


actual class ProgressBar actual constructor(context: RContext): RView(context) {
    override val native = android.widget.ProgressBar(context.activity, null, android.R.attr.progressBarStyleHorizontal).apply {
        min = 0
        max = 10000
    }
    override fun applyForeground(theme: Theme) {
        native.progressTintList = ColorStateList.valueOf(theme.foreground.colorInt())
        native.progressBackgroundTintList = ColorStateList.valueOf(theme.background.colorInt())
        native.setPaddingAll(0)
    }

    actual var ratio: Float
        get() = native.progress / 10000f
        set(value) { native.progress = (value * 10000).roundToInt() }
}