package com.lightningkite.kiteui.views.direct

import android.content.res.ColorStateList
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.ShapeDrawable
import android.view.Gravity
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import kotlin.math.roundToInt

actual class ProgressBar actual constructor(context: RContext): RView(context) {
    override val native = android.widget.ProgressBar(context.activity, null, android.R.attr.progressBarStyleHorizontal).apply {
        min = 0
        max = 10000

        // The default drawable uses a fixed height; use a custom drawable to support progress bars of any height
        progressDrawable = ClipDrawable(ShapeDrawable(), Gravity.START, ClipDrawable.HORIZONTAL)
        clipToOutline = true
    }
    override fun applyForeground(theme: Theme) {
        native.setProgressTintList(ColorStateList.valueOf(theme.foreground.colorInt()))
        native.setPaddingAll(0)
    }

    actual var ratio: Float
        get() = native.progress / 10000f
        set(value) { native.progress = (value * 10000).roundToInt() }
}