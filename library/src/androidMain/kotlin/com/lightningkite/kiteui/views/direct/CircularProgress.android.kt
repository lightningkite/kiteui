package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.graphics.*
import android.view.View
import androidx.annotation.FloatRange
import com.lightningkite.kiteui.models.FieldSemantic
import com.lightningkite.kiteui.models.Shadow
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import kotlin.math.min

actual class CircularProgress actual constructor(context: RContext) : RView(context) {

    override val native = NCircularProgress(context.activity)

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)
        val t = theme.theme
        val fieldTheme = theme[FieldSemantic].theme
        native.progressColor = t.foreground.colorInt()
        native.trackColor = fieldTheme.background.colorInt()
        native.shadows = fieldTheme.shadows
        native.invalidate()
    }

    actual var ratio: Float
        get() = native.progress / 100f
        set(value) { native.setProgress((value * 100)) }
}


class NCircularProgress(context: Context) : View(context) {

    private val progressPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val trackPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    var progressColor: Int = Color.DKGRAY
        set(value) {
            field = value; progressPaint.color = value
        }
    var trackColor: Int = Color.LTGRAY
        set(value) {
            field = value; trackPaint.color = value
        }
    var shadows: List<Shadow>? = null
        set(value) {
            field = value; cachedTrackBitmap = null
        }

    private val rect = RectF()
    private val startAngle = -90f
    private val maxAngle = 360f
    private val maxProgress = 100
    var progress: Int = 0
        private set
    private var angle = 0f

    private var cachedTrackBitmap: Bitmap? = null

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        cachedTrackBitmap = null
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        if (size <= 0) return

        val strokeWidth = size * 0.1f
        progressPaint.strokeWidth = strokeWidth
        trackPaint.strokeWidth = strokeWidth

        val half = strokeWidth / 2f
        rect.set(half, half, size - half, size - half)

        val trackShadows = shadows
        if (trackShadows != null && trackShadows.isNotEmpty()) {
            drawNeumorphicTrack(canvas, size, strokeWidth)
        } else {
            canvas.drawArc(rect, 0f, 360f, false, trackPaint)
        }

        // Draw progress arc
        if (angle > 0f) {
            canvas.drawArc(rect, startAngle, angle, false, progressPaint)
        }
    }

    private fun drawNeumorphicTrack(canvas: android.graphics.Canvas, size: Float, strokeWidth: Float) {
        if (cachedTrackBitmap == null || cachedTrackBitmap?.isRecycled == true) {
            cachedTrackBitmap = renderTrackWithShadows(size, strokeWidth)
        }
        cachedTrackBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }
    }

    private fun renderTrackWithShadows(size: Float, strokeWidth: Float): Bitmap? {
        val w = size.toInt()
        val h = size.toInt()
        if (w <= 0 || h <= 0) return null

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bitmap)

        val cx = size / 2f
        val cy = size / 2f
        val half = strokeWidth / 2f
        val trackRect = RectF(half, half, size - half, size - half)
        val outerRadius = cx
        val innerRadius = cx - strokeWidth
        val midRadius = (outerRadius + innerRadius) / 2f

        // Extract shadow colors: find the dark and light shadow colors
        val trackShadows = shadows ?: return bitmap
        val insetShadows = trackShadows.filter { it.inset }
        if (insetShadows.isEmpty()) {
            // No inset shadows, just draw plain track
            val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth
                color = trackColor
            }
            c.drawArc(trackRect, 0f, 360f, false, basePaint)
            return bitmap
        }

        // Use radial gradient to simulate a uniform groove:
        // outer edge -> dark shadow, middle -> track color, inner edge -> light shadow
        val darkColor = insetShadows.maxByOrNull {
            val r = (it.color.toInt() shr 16) and 0xFF
            val g = (it.color.toInt() shr 8) and 0xFF
            val b = it.color.toInt() and 0xFF
            val a = (it.color.toInt() ushr 24) and 0xFF
            a - (r + g + b) / 3  // higher alpha + darker = more "dark shadow"
        }?.color?.toInt() ?: Color.BLACK

        val lightColor = insetShadows.minByOrNull {
            val r = (it.color.toInt() shr 16) and 0xFF
            val g = (it.color.toInt() shr 8) and 0xFF
            val b = it.color.toInt() and 0xFF
            val a = (it.color.toInt() ushr 24) and 0xFF
            a - (r + g + b) / 3
        }?.color?.toInt() ?: Color.WHITE

        // Draw the track with a radial gradient to create groove effect
        // RadialGradient goes from center outward:
        // center -> inner edge (light highlight) -> middle (track color) -> outer edge (dark shadow)
        val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = RadialGradient(
                cx, cy, outerRadius,
                intArrayOf(
                    Color.TRANSPARENT,     // center (inside ring)
                    lightColor,            // inner edge of track (highlight)
                    trackColor,            // middle of track
                    darkColor,             // outer edge of track (shadow)
                    Color.TRANSPARENT      // beyond outer edge
                ),
                floatArrayOf(
                    innerRadius / outerRadius - 0.01f,  // just inside inner edge
                    innerRadius / outerRadius + 0.02f,  // inner part of track
                    midRadius / outerRadius,             // middle
                    1f - 0.02f,                          // outer part of track
                    1f                                   // outer edge
                ),
                Shader.TileMode.CLAMP
            )
        }

        // Clip to the annulus shape
        val clipPath = Path()
        clipPath.addCircle(cx, cy, outerRadius, Path.Direction.CW)
        clipPath.addCircle(cx, cy, innerRadius, Path.Direction.CCW)

        c.save()
        c.clipPath(clipPath)
        c.drawCircle(cx, cy, outerRadius, gradientPaint)
        c.restore()

        return bitmap
    }

    private fun calculateAngle(progress: Float) = maxAngle / maxProgress * progress

    fun setProgress(@FloatRange(from = 0.0, to = 100.0) progress: Float) {
        this.progress = progress.toInt()
        angle = calculateAngle(progress)
        invalidate()
    }
}
