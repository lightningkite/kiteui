package com.lightningkite.kiteui.views

import android.graphics.*
import android.graphics.drawable.Drawable
import com.lightningkite.kiteui.models.Shadow
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Self-contained drawable for neumorphic control indicators (checkbox, radio button, switch track).
 * Renders neumorphic shadows (both outer and inset) pre-rendered to cached bitmaps,
 * so no software layer is needed. Unlike [NeumorphicDrawable], this drawable includes
 * padding for outer shadows within its own bounds (no parent cooperation needed).
 *
 * Supports Android drawable state system to switch between checked/unchecked appearances.
 */
class NeumorphicControlDrawable(
    val controlWidthPx: Int,
    val controlHeightPx: Int,
    val isCircle: Boolean,
    val drawCheckmark: Boolean,
    val drawDot: Boolean,
) : Drawable() {

    constructor(
        controlSizePx: Int,
        isCircle: Boolean,
        drawCheckmark: Boolean,
        drawDot: Boolean,
    ) : this(controlSizePx, controlSizePx, isCircle, drawCheckmark, drawDot)

    var checkedShadows: List<Shadow> = emptyList()
        private set
    var uncheckedShadows: List<Shadow> = emptyList()
        private set
    var checkedBgColor: Int = Color.LTGRAY
        private set
    var uncheckedBgColor: Int = Color.LTGRAY
        private set
    var indicatorColor: Int = Color.DKGRAY
        private set
    var cornerRadiusPx: Float = controlHeightPx * 0.2f
        private set

    private var isChecked = false

    // Cached bitmaps for checked/unchecked shadow rendering
    private var checkedBitmap: Bitmap? = null
    private var uncheckedBitmap: Bitmap? = null

    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)

    val maxExtent: Float
        get() {
            var maxVal = 0f
            for (shadow in checkedShadows + uncheckedShadows) {
                if (shadow.inset) continue
                val extent = shadow.blurRadius.value + shadow.spreadRadius.value +
                        max(abs(shadow.offsetX.value), abs(shadow.offsetY.value))
                maxVal = max(maxVal, extent)
            }
            return maxVal
        }

    override fun getIntrinsicWidth() = controlWidthPx + (maxExtent * 2).roundToInt()
    override fun getIntrinsicHeight() = controlHeightPx + (maxExtent * 2).roundToInt()

    override fun isStateful() = true

    override fun onStateChange(state: IntArray): Boolean {
        val was = isChecked
        isChecked = android.R.attr.state_checked in state
        if (was != isChecked) {
            invalidateSelf()
            return true
        }
        return false
    }

    fun update(
        checkedShadows: List<Shadow>,
        uncheckedShadows: List<Shadow>,
        checkedBgColor: Int,
        uncheckedBgColor: Int,
        indicatorColor: Int,
        cornerRadiusPx: Float = this.cornerRadiusPx,
    ) {
        this.checkedShadows = checkedShadows
        this.uncheckedShadows = uncheckedShadows
        this.checkedBgColor = checkedBgColor
        this.uncheckedBgColor = uncheckedBgColor
        this.indicatorColor = indicatorColor
        this.cornerRadiusPx = cornerRadiusPx
        // Invalidate cached bitmaps
        checkedBitmap?.recycle()
        checkedBitmap = null
        uncheckedBitmap?.recycle()
        uncheckedBitmap = null
        invalidateSelf()
    }

    private fun renderShadowBitmap(shadows: List<Shadow>, bgColor: Int): Bitmap? {
        val extent = maxExtent
        val totalWidth = controlWidthPx + (extent * 2).roundToInt()
        val totalHeight = controlHeightPx + (extent * 2).roundToInt()
        if (totalWidth <= 0 || totalHeight <= 0) return null

        val bitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        val shapeRect = RectF(extent, extent, extent + controlWidthPx, extent + controlHeightPx)

        // Draw outer shadows
        for (shadow in shadows) {
            if (shadow.inset) continue
            paint.color = shadow.color.toInt()
            paint.maskFilter = if (shadow.blurRadius.value > 0)
                BlurMaskFilter(shadow.blurRadius.value.coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
            else null

            val r = RectF(shapeRect)
            r.offset(shadow.offsetX.value, shadow.offsetY.value)
            r.inset(-shadow.spreadRadius.value, -shadow.spreadRadius.value)

            if (isCircle) canvas.drawOval(r, paint)
            else canvas.drawRoundRect(r, cornerRadiusPx, cornerRadiusPx, paint)
        }

        // Draw background shape
        paint.color = bgColor
        paint.maskFilter = null
        if (isCircle) canvas.drawOval(shapeRect, paint)
        else canvas.drawRoundRect(shapeRect, cornerRadiusPx, cornerRadiusPx, paint)

        // Draw inset shadows (clipped to shape)
        val insetShadows = shadows.filter { it.inset }
        if (insetShadows.isNotEmpty()) {
            canvas.save()
            val clipPath = Path()
            if (isCircle) clipPath.addOval(shapeRect, Path.Direction.CW)
            else clipPath.addRoundRect(shapeRect, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW)
            canvas.clipPath(clipPath)

            val outerPath = Path()
            val innerPath = Path()
            for (shadow in insetShadows) {
                paint.color = shadow.color.toInt()
                paint.maskFilter = if (shadow.blurRadius.value > 0)
                    BlurMaskFilter(shadow.blurRadius.value.coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
                else null

                val blur = shadow.blurRadius.value
                val spread = shadow.spreadRadius.value
                val expansion = blur + spread

                val innerRect = RectF(shapeRect)
                innerRect.offset(shadow.offsetX.value, shadow.offsetY.value)
                val outerRect = RectF(innerRect).apply { inset(-expansion * 2, -expansion * 2) }

                outerPath.reset()
                innerPath.reset()
                if (isCircle) {
                    outerPath.addOval(outerRect, Path.Direction.CW)
                    innerPath.addOval(innerRect, Path.Direction.CCW)
                } else {
                    outerPath.addRoundRect(outerRect, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW)
                    innerPath.addRoundRect(innerRect, cornerRadiusPx, cornerRadiusPx, Path.Direction.CCW)
                }
                outerPath.addPath(innerPath)
                canvas.drawPath(outerPath, paint)
            }
            canvas.restore()
        }

        return bitmap
    }

    private fun ensureBitmaps() {
        if (checkedBitmap == null && checkedShadows.isNotEmpty()) {
            checkedBitmap = renderShadowBitmap(checkedShadows, checkedBgColor)
        }
        if (uncheckedBitmap == null && uncheckedShadows.isNotEmpty()) {
            uncheckedBitmap = renderShadowBitmap(uncheckedShadows, uncheckedBgColor)
        }
    }

    override fun draw(canvas: Canvas) {
        ensureBitmaps()

        val cx = bounds.centerX().toFloat()
        val cy = bounds.centerY().toFloat()
        val halfW = controlWidthPx / 2f
        val halfH = controlHeightPx / 2f
        val shapeRect = RectF(cx - halfW, cy - halfH, cx + halfW, cy + halfH)

        // Draw the pre-rendered shadow + background bitmap
        val bitmap = if (isChecked) checkedBitmap else uncheckedBitmap
        if (bitmap != null && !bitmap.isRecycled) {
            canvas.drawBitmap(bitmap, cx - bitmap.width / 2f, cy - bitmap.height / 2f, bitmapPaint)
        } else {
            // Fallback: draw just the shape without shadows
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = if (isChecked) checkedBgColor else uncheckedBgColor
            }
            if (isCircle) canvas.drawOval(shapeRect, bgPaint)
            else canvas.drawRoundRect(shapeRect, cornerRadiusPx, cornerRadiusPx, bgPaint)
        }

        // Draw indicator
        if (isChecked) {
            if (drawCheckmark) drawCheckMark(canvas, shapeRect)
            if (drawDot) drawRadioDot(canvas, shapeRect)
        }
    }

    private fun drawCheckMark(canvas: Canvas, rect: RectF) {
        val size = controlHeightPx.toFloat()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = indicatorColor
            strokeWidth = size * 0.12f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val path = Path()
        path.moveTo(rect.left + size * 0.22f, rect.centerY())
        path.lineTo(rect.left + size * 0.42f, rect.bottom - size * 0.25f)
        path.lineTo(rect.right - size * 0.18f, rect.top + size * 0.28f)
        canvas.drawPath(path, paint)
    }

    private fun drawRadioDot(canvas: Canvas, rect: RectF) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = indicatorColor
        }
        canvas.drawCircle(rect.centerX(), rect.centerY(), controlHeightPx * 0.25f, paint)
    }

    override fun setAlpha(alpha: Int) {
        bitmapPaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        bitmapPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
