package com.lightningkite.kiteui.views

import android.graphics.*
import android.graphics.drawable.Drawable
import com.lightningkite.kiteui.models.Shadow
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A custom Drawable that renders multiple shadows for neumorphism effects.
 *
 * All shadows (both outer and inset) are pre-rendered to cached bitmaps,
 * so no software layer is ever needed. Shadows are rendered WITHIN the
 * drawable's bounds by insetting the background shape, avoiding clipping
 * issues with ScrollView and other clipping containers.
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

    // Cached bitmaps
    private var outerShadowBitmap: Bitmap? = null
    private var insetShadowBitmap: Bitmap? = null
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private var lastBgWidth = 0
    private var lastBgHeight = 0

    // Reusable objects to avoid allocations in draw/create
    private val tmpPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tmpRect = RectF()
    private val tmpPath = Path()
    private val tmpPath2 = Path()

    /**
     * The calculated extent that outer shadows extend beyond the background shape.
     * This space is reserved within the drawable bounds.
     */
    var shadowExtent = 0f
        private set

    private var hasOuterShadows = false
    private var hasInsetShadows = false

    init {
        shadowExtent = calculateShadowExtent()
        categoriseShadows()
    }

    private fun categoriseShadows() {
        hasOuterShadows = shadows.any { !it.inset }
        hasInsetShadows = shadows.any { it.inset }
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

    companion object {
        // Cap bitmap size to avoid OOM / "trying to draw too large bitmap" crashes
        private const val MAX_BITMAP_PIXELS = 4096 * 4096
    }

    private fun createOuterShadowBitmap(bgWidth: Int, bgHeight: Int) {
        if (!hasOuterShadows || shadowExtent <= 0f || bgWidth <= 0 || bgHeight <= 0) {
            outerShadowBitmap?.recycle()
            outerShadowBitmap = null
            return
        }

        val bitmapWidth = (bgWidth + shadowExtent * 2).roundToInt()
        val bitmapHeight = (bgHeight + shadowExtent * 2).roundToInt()

        if (bitmapWidth <= 0 || bitmapHeight <= 0 || bitmapWidth.toLong() * bitmapHeight > MAX_BITMAP_PIXELS) {
            outerShadowBitmap?.recycle()
            outerShadowBitmap = null
            return
        }

        // Reuse existing bitmap if size matches
        outerShadowBitmap?.let {
            if (it.width == bitmapWidth && it.height == bitmapHeight) {
                it.eraseColor(android.graphics.Color.TRANSPARENT)
            } else {
                it.recycle()
                outerShadowBitmap = null
            }
        }

        if (outerShadowBitmap == null) {
            outerShadowBitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        }

        val bitmap = outerShadowBitmap ?: return
        val canvas = Canvas(bitmap)
        val shapeRect = RectF(shadowExtent, shadowExtent, shadowExtent + bgWidth, shadowExtent + bgHeight)

        for (shadow in shadows) {
            if (shadow.inset) continue

            tmpPaint.reset()
            tmpPaint.isAntiAlias = true
            tmpPaint.style = Paint.Style.FILL
            tmpPaint.color = shadow.color.toInt()
            if (shadow.blurRadius.value > 0) {
                tmpPaint.maskFilter = BlurMaskFilter(
                    shadow.blurRadius.value.coerceAtLeast(1f),
                    BlurMaskFilter.Blur.NORMAL
                )
            }

            tmpRect.set(shapeRect)
            tmpRect.offset(shadow.offsetX.value, shadow.offsetY.value)
            val spread = shadow.spreadRadius.value
            tmpRect.inset(-spread, -spread)

            val radii = cornerRadii
            if (radii != null) {
                tmpPath.reset()
                tmpPath.addRoundRect(tmpRect, radii, Path.Direction.CW)
                canvas.drawPath(tmpPath, tmpPaint)
            } else {
                canvas.drawRoundRect(tmpRect, cornerRadius, cornerRadius, tmpPaint)
            }
        }
    }

    private fun createInsetShadowBitmap(bgWidth: Int, bgHeight: Int) {
        if (!hasInsetShadows || bgWidth <= 0 || bgHeight <= 0) {
            insetShadowBitmap?.recycle()
            insetShadowBitmap = null
            return
        }

        if (bgWidth.toLong() * bgHeight > MAX_BITMAP_PIXELS) {
            insetShadowBitmap?.recycle()
            insetShadowBitmap = null
            return
        }

        // Reuse existing bitmap if size matches
        insetShadowBitmap?.let {
            if (it.width == bgWidth && it.height == bgHeight) {
                it.eraseColor(android.graphics.Color.TRANSPARENT)
            } else {
                it.recycle()
                insetShadowBitmap = null
            }
        }

        if (insetShadowBitmap == null) {
            insetShadowBitmap = Bitmap.createBitmap(bgWidth, bgHeight, Bitmap.Config.ARGB_8888)
        }

        val bitmap = insetShadowBitmap ?: return
        val canvas = Canvas(bitmap)

        // Clip to the background shape (in bitmap-local coordinates: 0,0 to bgWidth,bgHeight)
        val clipRect = RectF(0f, 0f, bgWidth.toFloat(), bgHeight.toFloat())
        val clipPath = Path()
        val radii = cornerRadii
        if (radii != null) {
            clipPath.addRoundRect(clipRect, radii, Path.Direction.CW)
        } else {
            clipPath.addRoundRect(clipRect, cornerRadius, cornerRadius, Path.Direction.CW)
        }
        canvas.clipPath(clipPath)

        for (shadow in shadows) {
            if (!shadow.inset) continue

            tmpPaint.reset()
            tmpPaint.isAntiAlias = true
            tmpPaint.style = Paint.Style.FILL
            tmpPaint.color = shadow.color.toInt()
            if (shadow.blurRadius.value > 0) {
                tmpPaint.maskFilter = BlurMaskFilter(
                    shadow.blurRadius.value.coerceAtLeast(1f),
                    BlurMaskFilter.Blur.NORMAL
                )
            }

            // Inset shadow: draw a ring (outer minus inner) to create edge shadow effect
            val spread = shadow.spreadRadius.value
            val blur = shadow.blurRadius.value
            val expansion = blur + spread

            val innerRect = RectF(clipRect)
            innerRect.offset(shadow.offsetX.value, shadow.offsetY.value)

            val outerRect = RectF(innerRect)
            outerRect.inset(-expansion * 2, -expansion * 2)

            tmpPath.reset()
            tmpPath2.reset()
            if (radii != null) {
                tmpPath.addRoundRect(outerRect, radii, Path.Direction.CW)
                tmpPath2.addRoundRect(innerRect, radii, Path.Direction.CCW)
            } else {
                tmpPath.addRoundRect(outerRect, cornerRadius, cornerRadius, Path.Direction.CW)
                tmpPath2.addRoundRect(innerRect, cornerRadius, cornerRadius, Path.Direction.CCW)
            }
            tmpPath.addPath(tmpPath2)
            canvas.drawPath(tmpPath, tmpPaint)
        }
    }

    private fun invalidateBitmaps() {
        outerShadowBitmap?.recycle()
        outerShadowBitmap = null
        insetShadowBitmap?.recycle()
        insetShadowBitmap = null
        lastBgWidth = 0
        lastBgHeight = 0
    }

    fun setShadows(shadows: List<Shadow>) {
        this.shadows = shadows
        shadowExtent = calculateShadowExtent()
        categoriseShadows()
        invalidateBitmaps()
        invalidateSelf()
    }

    fun setCornerRadius(radius: Float) {
        this.cornerRadius = radius
        this.cornerRadii = null
        invalidateBitmaps()
        invalidateSelf()
    }

    fun setCornerRadii(radii: FloatArray) {
        this.cornerRadii = radii
        invalidateBitmaps()
        invalidateSelf()
    }

    fun setBackgroundColor(color: Int) {
        this.backgroundColor = color
        backgroundPaint.color = color
        invalidateSelf()
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)

        // Background is inset from bounds by shadowExtent
        backgroundRect.set(
            bounds.left + shadowExtent,
            bounds.top + shadowExtent,
            bounds.right - shadowExtent,
            bounds.bottom - shadowExtent
        )

        // Update background path
        backgroundPath.reset()
        val radii = cornerRadii
        if (radii != null) {
            backgroundPath.addRoundRect(backgroundRect, radii, Path.Direction.CW)
        } else {
            backgroundPath.addRoundRect(backgroundRect, cornerRadius, cornerRadius, Path.Direction.CW)
        }

        // Recreate bitmaps if background size changed
        val bgWidth = backgroundRect.width().roundToInt()
        val bgHeight = backgroundRect.height().roundToInt()
        if (bgWidth != lastBgWidth || bgHeight != lastBgHeight) {
            lastBgWidth = bgWidth
            lastBgHeight = bgHeight
            createOuterShadowBitmap(bgWidth, bgHeight)
            createInsetShadowBitmap(bgWidth, bgHeight)
        }
    }

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        if (bounds.isEmpty) return

        // Draw cached outer shadow bitmap
        outerShadowBitmap?.let {
            canvas.drawBitmap(it, bounds.left.toFloat(), bounds.top.toFloat(), bitmapPaint)
        }

        // Draw the background
        canvas.drawPath(backgroundPath, backgroundPaint)

        // Draw cached inset shadow bitmap (positioned at backgroundRect origin)
        insetShadowBitmap?.let {
            canvas.drawBitmap(it, backgroundRect.left, backgroundRect.top, bitmapPaint)
        }
    }

    override fun setAlpha(alpha: Int) {
        backgroundPaint.alpha = alpha
        bitmapPaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        backgroundPaint.colorFilter = colorFilter
        bitmapPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    /**
     * All shadows are pre-rendered to bitmaps, so no software layer is needed.
     */
    fun needsSoftwareLayer(): Boolean = false
}
