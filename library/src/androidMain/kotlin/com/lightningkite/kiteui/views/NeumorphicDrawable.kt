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
 *
 * Shadow bitmaps are rendered at half resolution since blur makes full
 * resolution unnecessary. Bitmaps are shared across drawables with the
 * same shadow config and dimensions via [ShadowBitmapCache].
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

    // Cached bitmap references (shared via ShadowBitmapCache)
    private var outerCacheEntry: ShadowBitmapCache.Entry? = null
    private var insetCacheEntry: ShadowBitmapCache.Entry? = null
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val drawMatrix = Matrix()
    private var lastBgWidth = 0
    private var lastBgHeight = 0

    /**
     * The calculated extent that outer shadows extend beyond the background shape.
     * This space is reserved within the drawable bounds.
     */
    var shadowExtent = 0f
        private set

    private var outerShadows = emptyList<Shadow>()
    private var insetShadows = emptyList<Shadow>()

    init {
        shadowExtent = calculateShadowExtent()
        categorizeShadows()
    }

    private fun categorizeShadows() {
        outerShadows = shadows.filter { !it.inset }
        insetShadows = shadows.filter { it.inset }
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

    private fun updateBitmaps(bgWidth: Int, bgHeight: Int) {
        if (bgWidth <= 0 || bgHeight <= 0) {
            releaseBitmaps()
            return
        }

        // Outer shadow bitmap
        if (outerShadows.isNotEmpty() && shadowExtent > 0f) {
            val key = ShadowBitmapCache.Key(
                bgWidth,
                bgHeight,
                cornerRadius,
                cornerRadii?.contentHashCode() ?: 0,
                outerShadows
            )
            outerCacheEntry = ShadowBitmapCache.getOrCreate(key, shadowExtent) { bitmapCanvas, scale ->
                renderOuterShadows(bitmapCanvas, bgWidth, bgHeight, scale)
            }
        } else {
            outerCacheEntry?.release()
            outerCacheEntry = null
        }

        // Inset shadow bitmap
        if (insetShadows.isNotEmpty()) {
            val key = ShadowBitmapCache.Key(
                bgWidth,
                bgHeight,
                cornerRadius,
                cornerRadii?.contentHashCode() ?: 0,
                insetShadows
            )
            insetCacheEntry = ShadowBitmapCache.getOrCreate(key, 0f) { bitmapCanvas, scale ->
                renderInsetShadows(bitmapCanvas, bgWidth, bgHeight, scale)
            }
        } else {
            insetCacheEntry?.release()
            insetCacheEntry = null
        }

        lastBgWidth = bgWidth
        lastBgHeight = bgHeight
    }

    private fun renderOuterShadows(canvas: Canvas, bgWidth: Int, bgHeight: Int, scale: Float) {
        val extent = shadowExtent * scale
        val shapeRect = RectF(extent, extent, extent + bgWidth * scale, extent + bgHeight * scale)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val path = Path()

        for (shadow in outerShadows) {
            paint.color = shadow.color.toInt()
            paint.maskFilter = if (shadow.blurRadius.value > 0)
                BlurMaskFilter((shadow.blurRadius.value * scale).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
            else null

            val r = RectF(shapeRect)
            r.offset(shadow.offsetX.value * scale, shadow.offsetY.value * scale)
            val spread = shadow.spreadRadius.value * scale
            r.inset(-spread, -spread)

            val cr = cornerRadius * scale
            val radii = cornerRadii
            if (radii != null) {
                path.reset()
                path.addRoundRect(r, radii.map { it * scale }.toFloatArray(), Path.Direction.CW)
                canvas.drawPath(path, paint)
            } else {
                canvas.drawRoundRect(r, cr, cr, paint)
            }
        }
    }

    private fun renderInsetShadows(canvas: Canvas, bgWidth: Int, bgHeight: Int, scale: Float) {
        val clipRect = RectF(0f, 0f, bgWidth * scale, bgHeight * scale)
        val cr = cornerRadius * scale
        val clipPath = Path()
        val radii = cornerRadii
        if (radii != null) {
            clipPath.addRoundRect(clipRect, radii.map { it * scale }.toFloatArray(), Path.Direction.CW)
        } else {
            clipPath.addRoundRect(clipRect, cr, cr, Path.Direction.CW)
        }
        canvas.clipPath(clipPath)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val outerPath = Path()
        val innerPath = Path()

        for (shadow in insetShadows) {
            paint.color = shadow.color.toInt()
            paint.maskFilter = if (shadow.blurRadius.value > 0)
                BlurMaskFilter((shadow.blurRadius.value * scale).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
            else null

            val spread = shadow.spreadRadius.value * scale
            val blur = shadow.blurRadius.value * scale
            val expansion = blur + spread

            val innerRect = RectF(clipRect)
            innerRect.offset(shadow.offsetX.value * scale, shadow.offsetY.value * scale)
            val outerRect = RectF(innerRect).apply { inset(-expansion * 2, -expansion * 2) }

            outerPath.reset()
            innerPath.reset()
            if (radii != null) {
                val scaledRadii = radii.map { it * scale }.toFloatArray()
                outerPath.addRoundRect(outerRect, scaledRadii, Path.Direction.CW)
                innerPath.addRoundRect(innerRect, scaledRadii, Path.Direction.CCW)
            } else {
                outerPath.addRoundRect(outerRect, cr, cr, Path.Direction.CW)
                innerPath.addRoundRect(innerRect, cr, cr, Path.Direction.CCW)
            }
            outerPath.addPath(innerPath)
            canvas.drawPath(outerPath, paint)
        }
    }

    private fun releaseBitmaps() {
        outerCacheEntry?.release()
        outerCacheEntry = null
        insetCacheEntry?.release()
        insetCacheEntry = null
        lastBgWidth = 0
        lastBgHeight = 0
    }

    fun setShadows(shadows: List<Shadow>) {
        this.shadows = shadows
        shadowExtent = calculateShadowExtent()
        categorizeShadows()
        releaseBitmaps()
        invalidateSelf()
    }

    fun setCornerRadius(radius: Float) {
        this.cornerRadius = radius
        this.cornerRadii = null
        releaseBitmaps()
        invalidateSelf()
    }

    fun setCornerRadii(radii: FloatArray) {
        this.cornerRadii = radii
        releaseBitmaps()
        invalidateSelf()
    }

    fun setBackgroundColor(color: Int) {
        this.backgroundColor = color
        backgroundPaint.color = color
        invalidateSelf()
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)

        backgroundRect.set(
            bounds.left + shadowExtent,
            bounds.top + shadowExtent,
            bounds.right - shadowExtent,
            bounds.bottom - shadowExtent
        )

        backgroundPath.reset()
        val radii = cornerRadii
        if (radii != null) {
            backgroundPath.addRoundRect(backgroundRect, radii, Path.Direction.CW)
        } else {
            backgroundPath.addRoundRect(backgroundRect, cornerRadius, cornerRadius, Path.Direction.CW)
        }

        val bgWidth = backgroundRect.width().roundToInt()
        val bgHeight = backgroundRect.height().roundToInt()
        if (bgWidth != lastBgWidth || bgHeight != lastBgHeight) {
            updateBitmaps(bgWidth, bgHeight)
        }
    }

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        if (bounds.isEmpty) return

        // Draw outer shadows
        outerCacheEntry?.let { entry ->
            drawMatrix.setScale(entry.inverseScale, entry.inverseScale)
            drawMatrix.postTranslate(bounds.left.toFloat(), bounds.top.toFloat())
            canvas.drawBitmap(entry.bitmap, drawMatrix, bitmapPaint)
        }

        // Draw background
        canvas.drawPath(backgroundPath, backgroundPaint)

        // Draw inset shadows
        insetCacheEntry?.let { entry ->
            drawMatrix.setScale(entry.inverseScale, entry.inverseScale)
            drawMatrix.postTranslate(backgroundRect.left, backgroundRect.top)
            canvas.drawBitmap(entry.bitmap, drawMatrix, bitmapPaint)
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

    /** All shadows are pre-rendered to bitmaps, so no software layer is needed. */
    fun needsSoftwareLayer(): Boolean = false
}

/**
 * LRU cache for shadow bitmaps. Elements with the same shadow configuration and dimensions
 * share a single bitmap, dramatically reducing memory on pages with many cards/buttons.
 */
internal object ShadowBitmapCache {
    data class Key(
        val bgWidth: Int,
        val bgHeight: Int,
        val cornerRadius: Float,
        val cornerRadiiHash: Int,
        val shadows: List<Shadow>
    )

    class Entry(
        val bitmap: Bitmap,
        val inverseScale: Float,
        private val key: Key
    ) {
        private var refCount = 1
        fun acquire(): Entry {
            refCount++; return this
        }

        fun release() {
            refCount--
            if (refCount <= 0) {
                synchronized(cache) {
                    // Only remove from cache if this is still the cached entry
                    if (cache[key] === this) cache.remove(key)
                }
                bitmap.recycle()
            }
        }
    }

    private const val MAX_CACHE_SIZE = 64
    private const val MAX_BITMAP_PIXELS = 4096 * 4096

    // Render at half resolution - shadows are blurry so quality loss is invisible
    private const val SCALE = 0.5f

    private val cache = LinkedHashMap<Key, Entry>(16, 0.75f, true)

    fun getOrCreate(key: Key, shadowExtent: Float, render: (Canvas, Float) -> Unit): Entry? {
        synchronized(cache) {
            cache[key]?.let { return it.acquire() }
        }

        val scaledBgW = (key.bgWidth * SCALE).roundToInt()
        val scaledBgH = (key.bgHeight * SCALE).roundToInt()
        val scaledExtent = (shadowExtent * SCALE).roundToInt()
        val bitmapW = scaledBgW + scaledExtent * 2
        val bitmapH = scaledBgH + scaledExtent * 2

        if (bitmapW <= 0 || bitmapH <= 0 || bitmapW.toLong() * bitmapH > MAX_BITMAP_PIXELS) {
            return null
        }

        val bitmap = Bitmap.createBitmap(bitmapW, bitmapH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        render(canvas, SCALE)

        val entry = Entry(bitmap, 1f / SCALE, key)

        synchronized(cache) {
            // Evict oldest entries if cache is full
            while (cache.size >= MAX_CACHE_SIZE) {
                val oldest = cache.entries.iterator().next()
                cache.remove(oldest.key)
                oldest.value.release()
            }
            cache[key] = entry.acquire() // cache holds a ref
        }

        return entry
    }
}
