package com.lightningkite.kiteui.views

import android.animation.ValueAnimator
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.direct.colorInt
import kotlin.math.min
import kotlin.math.roundToInt

internal fun Theme.backgroundDrawableWithoutCorners(existing: GradientDrawable? = null): GradientDrawable {
    return GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setStroke(outlineWidth.value.toInt(), outline.colorInt())

        when (background) {
            is Color -> {
                val oldColor: Int? =
                    existing?.colors?.get(0)
                val newColor = background.colorInt()

                if (oldColor != null && animationsEnabled) {
                    // Run the animation from old colors to new colors
                    val animator: ValueAnimator = ValueAnimator.ofArgb(oldColor, newColor).apply {
                        repeatMode = ValueAnimator.RESTART
                        repeatCount = 0
                        duration = 300
                    }

                    animator.addUpdateListener {
                        val intermediateColor = it.animatedValue as Int
                        colors = intArrayOf(intermediateColor, intermediateColor)
                    }
                    animator.start()
                } else {
                    // Set new colors immediately
                    colors = intArrayOf(newColor, newColor)
                }
            }

            is LinearGradient -> {
                if(Build.VERSION.SDK_INT >= 29)
                    setColors(background.stops.map { it.color.toInt() }.toIntArray(), background.stops.map { it.ratio }.toFloatArray())
                else
                    colors = background.stops.map { it.color.toInt() }.toIntArray()
                orientation = when ((background.angle angleTo Angle.zero).turns.times(8).roundToInt()) {
                    -3 -> GradientDrawable.Orientation.TR_BL
                    -2 -> GradientDrawable.Orientation.TOP_BOTTOM
                    -1 -> GradientDrawable.Orientation.TL_BR
                    0 -> GradientDrawable.Orientation.LEFT_RIGHT
                    1 -> GradientDrawable.Orientation.BL_TR
                    2 -> GradientDrawable.Orientation.BOTTOM_TOP
                    3 -> GradientDrawable.Orientation.BR_TL
                    else -> GradientDrawable.Orientation.LEFT_RIGHT
                }
                gradientType = GradientDrawable.LINEAR_GRADIENT
            }

            is RadialGradient -> {
                if(Build.VERSION.SDK_INT >= 29)
                    setColors(background.stops.map { it.color.toInt() }.toIntArray(), background.stops.map { it.ratio }.toFloatArray())
                else
                    colors = background.stops.map { it.color.toInt() }.toIntArray()
                gradientType = GradientDrawable.RADIAL_GRADIENT
            }
        }
    }
}