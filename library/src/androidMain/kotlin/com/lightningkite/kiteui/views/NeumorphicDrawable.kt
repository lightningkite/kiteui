package com.lightningkite.kiteui.views

import android.graphics.*
import android.graphics.drawable.Drawable
import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.Shadow
import kotlin.math.max

/**
 * A custom Drawable that renders multiple shadows for neumorphism effects.
 *
 * Unlike Android's native elevation-based shadows which only support a single shadow,
 * this drawable can render multiple shadows with different colors, offsets, and blur radii.
 * This is essential for neumorphism which requires both a light highlight shadow and a dark shadow.
 *
 * Note: This drawable requires software rendering (LAYER_TYPE_SOFTWARE) for blur effects
 * on Android versions before API 28. Hardware acceleration doesn't support BlurMaskFilter.
 *
 * @property shadows The list of shadows to render.
 * @property cornerRadius The corner radius for rounded corners (same for all corners).
 * @property backgroundColor The background color of the element.
 */
class NeumorphicDrawable(
    private var shadows: List<Shadow>,
    private var cornerRadius: Float,
    private var backgroundColor: Int,
    private var cornerRadii: FloatArray? = null
) : Drawable() {

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = backgroundColor
    }

    private val shadowPaints = mutableListOf<Paint>()
    private val shadowRects = mutableListOf<RectF>()
    private val backgroundRect = RectF()
    private val backgroundPath = Path()

    init {
        updateShadowPaints()
    }

    private fun updateShadowPaints() {
        shadowPaints.clear()
        shadowRects.clear()

        for (shadow in shadows) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = shadow.color.toInt()

                // Apply blur if specified
                if (shadow.blurRadius.value > 0) {
                    maskFilter = BlurMaskFilter(
                        shadow.blurRadius.value.coerceAtLeast(1f),
                        BlurMaskFilter.Blur.NORMAL
                    )
                }
            }
            shadowPaints.add(paint)
            shadowRects.add(RectF())
        }
    }

    fun setShadows(shadows: List<Shadow>) {
        this.shadows = shadows
        updateShadowPaints()
        invalidateSelf()
    }

    fun setCornerRadius(radius: Float) {
        this.cornerRadius = radius
        this.cornerRadii = null
        invalidateSelf()
    }

    fun setCornerRadii(radii: FloatArray) {
        this.cornerRadii = radii
        invalidateSelf()
    }

    fun setBackgroundColor(color: Int) {
        this.backgroundColor = color
        backgroundPaint.color = color
        invalidateSelf()
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)

        // Calculate the maximum shadow extent to ensure proper bounds
        var maxShadowExtent = 0f
        for (shadow in shadows) {
            val extent = max(
                shadow.blurRadius.value + shadow.spreadRadius.value,
                max(
                    kotlin.math.abs(shadow.offsetX.value),
                    kotlin.math.abs(shadow.offsetY.value)
                )
            )
            maxShadowExtent = max(maxShadowExtent, extent)
        }

        // Set up background rect (inset by shadow extent for non-inset shadows)
        backgroundRect.set(bounds)

        // Update shadow rects
        for ((index, shadow) in shadows.withIndex()) {
            val rect = shadowRects.getOrNull(index) ?: continue
            rect.set(backgroundRect)

            if (!shadow.inset) {
                // Outset shadows are drawn at the offset position
                rect.offset(
                    shadow.offsetX.value,
                    shadow.offsetY.value
                )
                // Apply spread
                val spread = shadow.spreadRadius.value
                rect.inset(-spread, -spread)
            } else {
                // Inset shadows need different handling
                rect.offset(
                    shadow.offsetX.value,
                    shadow.offsetY.value
                )
            }
        }

        // Update background path
        backgroundPath.reset()
        val radii = cornerRadii
        if (radii != null) {
            backgroundPath.addRoundRect(backgroundRect, radii, Path.Direction.CW)
        } else {
            backgroundPath.addRoundRect(backgroundRect, cornerRadius, cornerRadius, Path.Direction.CW)
        }
    }

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        if (bounds.isEmpty) return

        // Draw non-inset (outer) shadows first, behind the background
        for ((index, shadow) in shadows.withIndex()) {
            if (shadow.inset) continue

            val paint = shadowPaints.getOrNull(index) ?: continue
            val rect = shadowRects.getOrNull(index) ?: continue

            val radii = cornerRadii
            if (radii != null) {
                val path = Path().apply {
                    addRoundRect(rect, radii, Path.Direction.CW)
                }
                canvas.drawPath(path, paint)
            } else {
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
            }
        }

        // Draw the background
        canvas.drawPath(backgroundPath, backgroundPaint)

        // Draw inset shadows on top of the background
        for ((index, shadow) in shadows.withIndex()) {
            if (!shadow.inset) continue

            val paint = shadowPaints.getOrNull(index) ?: continue

            // For inset shadows, we need to draw inside the background
            // Use clip to restrict drawing to the background area
            canvas.save()
            canvas.clipPath(backgroundPath)

            // Draw a shape that creates an inset shadow effect
            // This is achieved by drawing a larger rectangle outside and using the blur
            // to create the shadow effect inside
            val insetRect = RectF(backgroundRect)
            insetRect.offset(
                shadow.offsetX.value,
                shadow.offsetY.value
            )

            // Draw the shadow shape - for inset, we draw a ring around the edge
            val spread = shadow.spreadRadius.value
            val blur = shadow.blurRadius.value
            val expansion = blur + spread

            // Create inner and outer paths for ring effect
            val outerPath = Path()
            val innerPath = Path()

            val outerRect = RectF(insetRect).apply {
                inset(-expansion * 2, -expansion * 2)
            }
            val innerRect = RectF(insetRect)

            val radii = cornerRadii
            if (radii != null) {
                outerPath.addRoundRect(outerRect, radii, Path.Direction.CW)
                innerPath.addRoundRect(innerRect, radii, Path.Direction.CCW)
            } else {
                outerPath.addRoundRect(outerRect, cornerRadius, cornerRadius, Path.Direction.CW)
                innerPath.addRoundRect(innerRect, cornerRadius, cornerRadius, Path.Direction.CCW)
            }

            outerPath.addPath(innerPath)
            canvas.drawPath(outerPath, paint)

            canvas.restore()
        }
    }

    override fun setAlpha(alpha: Int) {
        backgroundPaint.alpha = alpha
        for (paint in shadowPaints) {
            paint.alpha = alpha
        }
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        backgroundPaint.colorFilter = colorFilter
        for (paint in shadowPaints) {
            paint.colorFilter = colorFilter
        }
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    /**
     * Returns true if this drawable needs software rendering for proper blur effects.
     */
    fun needsSoftwareLayer(): Boolean {
        return shadows.any { it.blurRadius.value > 0 }
    }
}
