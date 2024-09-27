package com.lightningkite.kiteui.views.direct

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RotateDrawable
import android.graphics.drawable.ShapeDrawable
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.lightningkite.kiteui.R
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.dp
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import kotlin.math.roundToInt

actual class CircularProgress actual constructor(context: RContext) : RView(context) {

    override val native = android.widget.ProgressBar(context.activity, null, android.R.attr.progressBarStyleHorizontal).apply {
        min = 0
        max = 10000
        progressDrawable = getContext().getDrawable(R.drawable.progress_circle)
        layoutParams = ConstraintLayout.LayoutParams (1000, 1000)
    }
        override fun applyForeground(theme: Theme) {
            val layerDrawable = native.progressDrawable as LayerDrawable
            val backgroundShape = layerDrawable.getDrawable(0) as GradientDrawable
            backgroundShape.setColor(theme.background.colorInt())
            val forgroundRotateDrawable = layerDrawable.getDrawable(1) as RotateDrawable
            val shape = forgroundRotateDrawable.drawable as GradientDrawable
            shape.setColor(theme.foreground.colorInt())
        native.setPaddingAll(0)
    }

    actual var ratio: Float
        get() = native.progress / 10000f
        set(value) { native.progress = (value * 10000).roundToInt() }
}