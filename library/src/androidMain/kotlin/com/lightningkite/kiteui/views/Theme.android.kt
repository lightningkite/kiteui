package com.lightningkite.kiteui.views

import android.animation.ValueAnimator
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Build.VERSION_CODES
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.animation.doOnEnd
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.direct.colorInt
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.ranges.coerceAtMost
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal fun Theme.backgroundClippingDrawableWithoutCorners(): GradientDrawable {
    return MyGradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        val barelyColor = 0x01808080
        setStroke(0, barelyColor)
        colors = intArrayOf(barelyColor, barelyColor)
    }
}

private class MyGradientDrawable(): GradientDrawable() {
    var colorsOverTime: Array<Pair<IntArray, FloatArray>>? = null
    private var animator: ValueAnimator? = null
    private var setInstance = 0
    private var lastSetColors: IntArray? = null
    fun set(goal: IntArray, ratios: FloatArray) {
        if(Build.VERSION.SDK_INT >= VERSION_CODES.Q) {
            setColors(goal, ratios)
        } else {
            colors = goal
        }
        lastSetColors = goal
    }
    fun animateColorsTo(goal: IntArray, ratios: FloatArray, duration: Duration) {
        val myInstance = ++setInstance
        animator?.cancel()
        if(!animationsEnabled || lastSetColors?.size != goal.size) {
            set(goal, ratios)
            afterTimeout(100) {
                if(setInstance > myInstance) return@afterTimeout
                colorsOverTime?.let {
                    animateColorsTo(it[1].first, it[1].second, 2.seconds)
                }
            }
            return
        }
        val animationStartColors = lastSetColors ?: intArrayOf(0, 0)
        val animationGoalColors = goal
        // by Claude - pre-allocate frame buffer and cache Color conversions to avoid per-frame allocations at 60fps
        val frameBuffer = IntArray(animationStartColors.size)
        val startColors = Array(animationStartColors.size) { Color.fromInt(animationStartColors[it]) }
        val goalColors = Array(animationGoalColors.size) { Color.fromInt(animationGoalColors[it]) }
        animator = ValueAnimator.ofFloat(0f, 1f).also {
            it.duration = duration.inWholeMilliseconds
            it.interpolator = AccelerateDecelerateInterpolator()
            it.addUpdateListener { it ->
                if(setInstance > myInstance) return@addUpdateListener
                val f = it.animatedFraction
                for (index in frameBuffer.indices) {
                    frameBuffer[index] = Color.interpolate(startColors[index], goalColors[index], f).toInt()
                }
                set(frameBuffer, ratios)
            }
            it.doOnEnd {
                if(setInstance > myInstance) return@doOnEnd
                set(goal, ratios)
                colorsOverTime?.let {
                    val diffToFirst = Color.fromInt(goal[0]).channelDifferenceSum(Color.fromInt(it[0].first[0]))
                    val diffToSecond = Color.fromInt(goal[0]).channelDifferenceSum(Color.fromInt(it[1].first[0]))
                    if(diffToFirst < diffToSecond) animateColorsTo(it[1].first, it[1].second, 2.seconds)
                    else animateColorsTo(it[0].first, it[0].second, 2.seconds)
                }
            }
            it.start()
        }
    }
}

internal fun Theme.backgroundDrawableWithoutCorners(existing: GradientDrawable? = null): GradientDrawable
    = drawableWithoutCorners(background, outline, outlineWidth, existing)

internal fun drawableWithoutCorners(fill: Paint, stroke: Paint, strokeWidth: Dimension, existing: GradientDrawable? = null): GradientDrawable {
    return (existing as? MyGradientDrawable ?: MyGradientDrawable()).apply {
        shape = GradientDrawable.RECTANGLE
        setStroke(strokeWidth.value.toInt(), stroke.colorInt())

        val useFill = fill
        colorsOverTime = null

        when (useFill) {
            is Color -> {
                animateColorsTo(intArrayOf(useFill.toInt(), useFill.toInt()), floatArrayOf(0f, 1f), 300.milliseconds)
            }

            is FadingColor -> {
                colorsOverTime = arrayOf(
                    intArrayOf(useFill.base.toInt(), useFill.base.toInt()) to floatArrayOf(0f, 1f),
                    intArrayOf(useFill.alternate.toInt(), useFill.alternate.toInt()) to floatArrayOf(0f, 1f),
                )
                animateColorsTo(intArrayOf(useFill.base.toInt(), useFill.base.toInt()), floatArrayOf(0f, 1f), 300.milliseconds)
            }

            is LinearGradient -> {
                animateColorsTo(useFill.stops.map { it.color.toInt() }.toIntArray(), useFill.stops.map { it.ratio }.toFloatArray(), 300.milliseconds)
                orientation = when ((useFill.angle angleTo Angle.zero).turns.times(8).roundToInt()) {
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
                animateColorsTo(useFill.stops.map { it.color.toInt() }.toIntArray(), useFill.stops.map { it.ratio }.toFloatArray(), 300.milliseconds)
                gradientType = GradientDrawable.RADIAL_GRADIENT
            }
        }
    }
}

fun GradientDrawable.applyGradientRadiusListener(native: View): (() -> Unit)? {
    this.apply {
        if (Build.VERSION.SDK_INT >= VERSION_CODES.Q && gradientType == GradientDrawable.RADIAL_GRADIENT) {
            val l = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                // Use the farthest-corner radius (half the diagonal) so the gradient bleeds
                // to all edges/corners, matching the web platform's `farthest-corner` behavior.
                val halfW = native.width / 2f
                val halfH = native.height / 2f
                gradientRadius = sqrt(halfW * halfW + halfH * halfH)
            }
            native.addOnLayoutChangeListener(l)
            return { native.removeOnLayoutChangeListener(l) }
        } else {
            return null
        }
    }
}