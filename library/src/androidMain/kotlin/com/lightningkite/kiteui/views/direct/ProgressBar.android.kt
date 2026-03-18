package com.lightningkite.kiteui.views.direct

import android.content.res.ColorStateList
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.view.Gravity
import com.lightningkite.kiteui.current
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import kotlin.math.min
import kotlin.math.roundToInt

actual class ProgressBar actual constructor(context: RContext) : RView(context) {
    val shapeDrawable = ShapeDrawable().apply {
        shape = RoundRectShape(floatArrayOf(999f, 999f, 999f, 999f, 999f, 999f, 999f, 999f), null, null)
    }
    val clipDrawable = ClipDrawable(shapeDrawable, Gravity.START, ClipDrawable.HORIZONTAL)
    override val native =
        android.widget.ProgressBar(context.activity, null, android.R.attr.progressBarStyleHorizontal).apply {
//        min = 0
            max = 10000

            // The default drawable uses a fixed height; use a custom drawable to support progress bars of any height
            progressDrawable = clipDrawable
        }

    init {
        themeChoice += FieldSemantic
    }

    // Progress bars should have no padding — the fill should be flush with the track edges.
    // On web, progress.kui has padding: 0px !important.
    override fun refreshPadding() {
        native.setPadding(0, 0, 0, 0)
    }

    override fun applyTheme(theme: ThemeAndBack) {
        val fieldTheme = theme[FieldSemantic]
        super.applyTheme(fieldTheme)
        val t = fieldTheme.theme
        shapeDrawable.paint.color = t.foreground.colorInt()
        run {
            val cr = when (val it = t.cornerRadii) {
                is CornerRadii.RatioOfSize -> if (it.ratio >= 0.5f) 9999f else it.ratio * min(
                    native.width,
                    native.height
                )

                is CornerRadii.AdaptiveToSpacing -> min((parent?.mySpacingForChildren ?: 0.px).value, it.value.value)
                is CornerRadii.Fixed -> it.value.value
                is CornerRadii.RatioOfSpacing -> it.value * (parent?.mySpacingForChildren ?: 0.px).value
                is CornerRadii.PerCorner -> it.value.value
            }

            val asPerCorner = t.cornerRadii as? CornerRadii.PerCorner
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

    actual var ratio: Float
        get() = native.progress / 10000f
        set(value) {
            native.progress = (value * 10000).roundToInt()
        }
}
