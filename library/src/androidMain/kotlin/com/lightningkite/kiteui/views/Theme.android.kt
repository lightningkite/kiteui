package com.lightningkite.kiteui.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Build.VERSION_CODES
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.animation.doOnEnd
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.direct.colorInt
import kotlin.math.roundToInt
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
        animator = ValueAnimator.ofFloat(0f, 1f).also {
            it.duration = duration.inWholeMilliseconds
            it.interpolator = AccelerateDecelerateInterpolator()
            it.addUpdateListener { it ->
                if(setInstance > myInstance) return@addUpdateListener
                val f = it.animatedFraction
                set(IntArray(animationStartColors.size) { index ->
                    Color.interpolate(
                        Color.fromInt(animationStartColors[index]),
                        Color.fromInt(animationGoalColors[index]),
                        f
                    ).toInt()
                }, ratios)
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

/**
 * Custom drawable for handling ImagePaint backgrounds
 */
class ImagePaintDrawable(
    private val context: Context,
    val imagePaint: ImagePaint,
    val strokeWidth: Dimension,
    val stroke: Paint
) : GradientDrawable() {
    private var imageDrawable: Drawable? = null
    private var isLoading = true
    private var loadFailed = false
    private var cornerRadii: FloatArray? = null

    init {
        // Set initial appearance
        shape = RECTANGLE
        setStroke(strokeWidth.value.toInt(), stroke.colorInt())
        setColor(imagePaint.closestColor().toInt())

        // Load the image
        when (val source = imagePaint.source) {
            is ImageResource -> {
                Glide.with(context)
                    .load(source.resource)
                    .into(object : CustomTarget<Drawable>() {
                        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                            imageDrawable = resource
                            isLoading = false
                            invalidateSelf()
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            imageDrawable = null
                            isLoading = false
                            loadFailed = true
                            invalidateSelf()
                        }
                    })
            }
            is ImageRemote -> {
                Glide.with(context)
                    .load(source.url)
                    .into(object : CustomTarget<Drawable>() {
                        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                            imageDrawable = resource
                            isLoading = false
                            invalidateSelf()
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            imageDrawable = null
                            isLoading = false
                            loadFailed = true
                            invalidateSelf()
                        }
                    })
            }
            is ImageRaw -> {
                Glide.with(context)
                    .load(source.data.data)
                    .into(object : CustomTarget<Drawable>() {
                        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                            imageDrawable = resource
                            isLoading = false
                            invalidateSelf()
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            imageDrawable = null
                            isLoading = false
                            loadFailed = true
                            invalidateSelf()
                        }
                    })
            }
            is ImageLocal -> {
                Glide.with(context)
                    .load(source.file.uri)
                    .into(object : CustomTarget<Drawable>() {
                        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                            imageDrawable = resource
                            isLoading = false
                            invalidateSelf()
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            imageDrawable = null
                            isLoading = false
                            loadFailed = true
                            invalidateSelf()
                        }
                    })
            }
            else -> {
                isLoading = false
                loadFailed = true
            }
        }
    }

    override fun draw(canvas: Canvas) {
        // Draw the background color first
        super.draw(canvas)

        // If we have an image drawable, draw it on top
        imageDrawable?.let { drawable ->
            val bounds = bounds

            // Save the canvas state before clipping
            canvas.save()

            // Apply clipping with rounded corners if cornerRadii is set
            cornerRadii?.let { radii ->
                val path = Path()
                val rectF = RectF(bounds)
                path.addRoundRect(rectF, radii, Path.Direction.CW)
                canvas.clipPath(path)
            }

            // Configure the drawable based on the ImagePaintMode
            if (imagePaint.mode == ImagePaintMode.Repeating) {
                // For repeating mode, we need to create a tiled bitmap
                if (drawable is BitmapDrawable) {
                    drawable.setTileModeXY(
                        android.graphics.Shader.TileMode.REPEAT,
                        android.graphics.Shader.TileMode.REPEAT
                    )
                    drawable.bounds = bounds
                    drawable.draw(canvas)
                } else {
                    // If it's not a BitmapDrawable, we'll draw it once
                    drawable.bounds = bounds
                    drawable.draw(canvas)
                }
            } else { // Crop mode
                // For crop mode, we'll center-crop the image (maintain aspect ratio)
                val drawableBounds = calculateCenterCropBounds(drawable, bounds)
                drawable.bounds = drawableBounds
                drawable.draw(canvas)
            }

            // If there's an overlay color, draw it on top with the specified alpha
            if (imagePaint.overlayColor.alpha > 0) {
                canvas.drawColor(imagePaint.overlayColor.toInt())
            }

            // Restore the canvas state after drawing
            canvas.restore()
        }
    }

    // Override setCornerRadii to ensure it's applied to both the base drawable and the image
    override fun setCornerRadii(radii: FloatArray?) {
        super.setCornerRadii(radii)
        // Store the corner radii for use in the draw method
        this.cornerRadii = radii
    }

    // Calculate bounds for center-crop scaling (maintain aspect ratio while filling)
    private fun calculateCenterCropBounds(drawable: Drawable, containerBounds: Rect): Rect {
        val containerWidth = containerBounds.width()
        val containerHeight = containerBounds.height()

        if (containerWidth == 0 || containerHeight == 0 || 
            drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            return containerBounds
        }

        val drawableAspect = drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight
        val containerAspect = containerWidth.toFloat() / containerHeight

        val scale: Float
        var dx = 0
        var dy = 0

        // Determine which dimension to fill completely
        if (drawableAspect > containerAspect) {
            // Image is wider than container (relative to height)
            // Fill height completely and center horizontally
            scale = containerHeight.toFloat() / drawable.intrinsicHeight
            dx = ((containerWidth - drawable.intrinsicWidth * scale) / 2).toInt()
        } else {
            // Image is taller than container (relative to width)
            // Fill width completely and center vertically
            scale = containerWidth.toFloat() / drawable.intrinsicWidth
            dy = ((containerHeight - drawable.intrinsicHeight * scale) / 2).toInt()
        }

        val scaledWidth = (drawable.intrinsicWidth * scale).toInt()
        val scaledHeight = (drawable.intrinsicHeight * scale).toInt()

        return Rect(
            containerBounds.left + dx,
            containerBounds.top + dy,
            containerBounds.left + dx + scaledWidth,
            containerBounds.top + dy + scaledHeight
        )
    }
}

internal fun drawableWithoutCorners(fill: Paint, stroke: Paint, strokeWidth: Dimension, existing: GradientDrawable? = null): GradientDrawable {
    // For ImagePaint, we'll need a context to load the image
    // This will be handled by RView.android.kt which has access to a context
    if (fill is ImagePaint) {
        // We'll return a regular GradientDrawable here
        // The actual image loading will be done in RView.applyTheme
    }

    // Otherwise, use the default implementation
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

            is ImagePaint -> {
                // Use the closest color as a fallback
                animateColorsTo(intArrayOf(useFill.closestColor().toInt(), useFill.closestColor().toInt()), floatArrayOf(0f, 1f), 300.milliseconds)
            }
        }
    }
}
