package com.lightningkite.kiteui.views

import android.graphics.*
import android.graphics.drawable.Drawable
import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.Shadow
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A custom Drawable that renders multiple shadows for neumorphism effects.
 *
 * Unlike Android's native elevation-based shadows which only support a single shadow,
 * this drawable can render multiple shadows with different colors, offsets, and blur radii.
 * This is essential for neumorphism which requires both a light highlight shadow and a dark shadow.
 *
 * This implementation pre-renders outer shadows to a cached bitmap, which allows shadows
 * to extend beyond the view's bounds without requiring a software layer on the view itself.
 * This avoids the clipping issues that occur with LAYER_TYPE_SOFTWARE.
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

    private val backgroundRect = RectF()
    private val backgroundPath = Path()

    // Cached shadow bitmap for outer shadows
    private var shadowBitmap: Bitmap? = null
    private var shadowBitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private var shadowExtent = 0f
    private var lastBoundsWidth = 0
    private var lastBoundsHeight = 0

    // For inset shadows (drawn directly, not cached)
    private val insetShadowPaints = mutableListOf<Paint>()

    init {
        updateInsetShadowPaints()
    }

    private fun updateInsetShadowPaints() {
        insetShadowPaints.clear()
        for (shadow in shadows) {
            if (!shadow.inset) continue
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = shadow.color.toInt()
                if (shadow.blurRadius.value > 0) {
                    maskFilter = BlurMaskFilter(
                        shadow.blurRadius.value.coerceAtLeast(1f),
                        BlurMaskFilter.Blur.NORMAL
                    )
                }
            }
            insetShadowPaints.add(paint)
        }
    }

    private fun calculateShadowExtent(): Float {
        var maxExtent = 0f
        for (shadow in shadows) {
            if (shadow.inset) continue
            val extent = shadow.blurRadius.value + shadow.spreadRadius.value +
                    max(abs(shadow.offsetX.value), abs(shadow.offsetY.value))
            maxExtent = max(maxExtent, extent)
        }
        return maxExtent
    }

    private fun createShadowBitmap(width: Int, height: Int) {
        shadowExtent = calculateShadowExtent()
        if (shadowExtent <= 0f || width <= 0 || height <= 0) {
            shadowBitmap?.recycle()
            shadowBitmap = null
            return
        }

        val bitmapWidth = (width + shadowExtent * 2).roundToInt()
        val bitmapHeight = (height + shadowExtent * 2).roundToInt()

        if (bitmapWidth <= 0 || bitmapHeight <= 0) {
            shadowBitmap?.recycle()
            shadowBitmap = null
            return
        }

        // Recycle old bitmap if size changed
        shadowBitmap?.let {
            if (it.width != bitmapWidth || it.height != bitmapHeight) {
                it.recycle()
                shadowBitmap = null
            }
        }

        // Create new bitmap if needed
        if (shadowBitmap == null) {
            shadowBitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        }

        val bitmap = shadowBitmap ?: return
        bitmap.eraseColor(android.graphics.Color.TRANSPARENT)

        val canvas = Canvas(bitmap)

        // The shape rect in bitmap coordinates (offset by shadowExtent)
        val shapeRect = RectF(shadowExtent, shadowExtent, shadowExtent + width, shadowExtent + height)

        // Draw each outer shadow
        for (shadow in shadows) {
            if (shadow.inset) continue

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = shadow.color.toInt()
                if (shadow.blurRadius.value > 0) {
                    maskFilter = BlurMaskFilter(
                        shadow.blurRadius.value.coerceAtLeast(1f),
                        BlurMaskFilter.Blur.NORMAL
                    )
                }
            }

            val shadowRect = RectF(shapeRect)
            shadowRect.offset(shadow.offsetX.value, shadow.offsetY.value)
            val spread = shadow.spreadRadius.value
            shadowRect.inset(-spread, -spread)

            val radii = cornerRadii
            if (radii != null) {
                val path = Path().apply {
                    addRoundRect(shadowRect, radii, Path.Direction.CW)
                }
                canvas.drawPath(path, paint)
            } else {
                canvas.drawRoundRect(shadowRect, cornerRadius, cornerRadius, paint)
            }
        }

        lastBoundsWidth = width
        lastBoundsHeight = height
    }

    fun setShadows(shadows: List<Shadow>) {
        this.shadows = shadows
        updateInsetShadowPaints()
        // Invalidate shadow bitmap
        shadowBitmap?.recycle()
        shadowBitmap = null
        invalidateSelf()
    }

    fun setCornerRadius(radius: Float) {
        this.cornerRadius = radius
        this.cornerRadii = null
        // Invalidate shadow bitmap
        shadowBitmap?.recycle()
        shadowBitmap = null
        invalidateSelf()
    }

    fun setCornerRadii(radii: FloatArray) {
        this.cornerRadii = radii
        // Invalidate shadow bitmap
        shadowBitmap?.recycle()
        shadowBitmap = null
        invalidateSelf()
    }

    fun setBackgroundColor(color: Int) {
        this.backgroundColor = color
        backgroundPaint.color = color
        invalidateSelf()
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)

        backgroundRect.set(bounds)

        // Update background path
        backgroundPath.reset()
        val radii = cornerRadii
        if (radii != null) {
            backgroundPath.addRoundRect(backgroundRect, radii, Path.Direction.CW)
        } else {
            backgroundPath.addRoundRect(backgroundRect, cornerRadius, cornerRadius, Path.Direction.CW)
        }

        // Recreate shadow bitmap if bounds changed
        if (bounds.width() != lastBoundsWidth || bounds.height() != lastBoundsHeight) {
            createShadowBitmap(bounds.width(), bounds.height())
        }
    }

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        if (bounds.isEmpty) return

        // Draw cached outer shadow bitmap
        shadowBitmap?.let { bitmap ->
            // Draw the shadow bitmap offset by -shadowExtent so it's centered on the view
            canvas.drawBitmap(
                bitmap,
                bounds.left - shadowExtent,
                bounds.top - shadowExtent,
                shadowBitmapPaint
            )
        }

        // Draw the background
        canvas.drawPath(backgroundPath, backgroundPaint)

        // Draw inset shadows on top of the background
        var insetPaintIndex = 0
        for (shadow in shadows) {
            if (!shadow.inset) continue

            val paint = insetShadowPaints.getOrNull(insetPaintIndex++) ?: continue

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
        shadowBitmapPaint.alpha = alpha
        for (paint in insetShadowPaints) {
            paint.alpha = alpha
        }
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        backgroundPaint.colorFilter = colorFilter
        shadowBitmapPaint.colorFilter = colorFilter
        for (paint in insetShadowPaints) {
            paint.colorFilter = colorFilter
        }
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    /**
     * Returns true if this drawable has inset shadows that need software rendering.
     * Outer shadows are pre-rendered to a bitmap, so they don't need a software layer.
     */
    fun needsSoftwareLayer(): Boolean {
        // Only inset shadows need software layer now, since outer shadows are pre-rendered
        return shadows.any { it.inset && it.blurRadius.value > 0 }
    }
}
