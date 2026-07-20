package com.lightningkite.kiteui.views.direct

import android.content.res.ColorStateList
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.view.Gravity
import android.view.View
import com.lightningkite.kiteui.models.CornerRadii
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import kotlin.math.min
import kotlin.math.roundToInt

public actual class ProgressBar actual constructor(context: ElementContext) : NativeElement(context) {
    public val shapeDrawable = ShapeDrawable().apply {
        shape = RoundRectShape(floatArrayOf(999f, 999f, 999f, 999f, 999f, 999f, 999f, 999f), null, null)
    }
    public val clipDrawable = ClipDrawable(shapeDrawable, Gravity.START, ClipDrawable.HORIZONTAL)
    override val native =
        android.widget.ProgressBar(context.activity, null, android.R.attr.progressBarStyleHorizontal).apply {
//        min = 0
            max = 10000

            // The default drawable uses a fixed height; use a custom drawable to support progress bars of any height
            progressDrawable = clipDrawable
            clipToOutline = true
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }

    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.nativeApplyTheme(theme)
        val theme = theme.theme
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            native.setProgressTintList(ColorStateList.valueOf(theme.foreground.colorInt()))
        }
        run {
            val cr = when (val it = theme.cornerRadii) {
                is CornerRadii.RatioOfSize -> if (it.ratio >= 0.5f) 9999f else it.ratio * min(
                    native.width,
                    native.height
                )

                is CornerRadii.AdaptiveToSpacing -> min((parent?.underlyingNativeElement?.spacingForChildCornerRadii ?: 0.px).value, it.value.value)
                is CornerRadii.Fixed -> it.value.value
                is CornerRadii.RatioOfSpacing -> it.value * (parent?.underlyingNativeElement?.spacingForChildCornerRadii ?: 0.px).value
                is CornerRadii.PerCorner -> it.value.value
            }

            val asPerCorner = theme.cornerRadii as? CornerRadii.PerCorner
            val topLeft = if (asPerCorner?.topLeft != false) cr else 0f
            val topRight = if (asPerCorner?.topRight != false) cr else 0f
            val bottomRight = if (asPerCorner?.bottomRight != false) cr else 0f
            val bottomLeft = if (asPerCorner?.bottomLeft != false) cr else 0f
            shapeDrawable.shape = RoundRectShape(
                floatArrayOf(
                    topLeft,
                    topLeft,
                    topRight,
                    topRight,
                    bottomRight,
                    bottomRight,
                    bottomLeft,
                    bottomLeft
                ), null, null
            )
        }
    }

    public actual var ratio: Float
        get() = native.progress / 10000f
        set(value) {
            native.progress = (value * 10000).roundToInt()
        }
}