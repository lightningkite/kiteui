package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RotateDrawable
import android.view.View
import androidx.annotation.FloatRange
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import kotlin.math.roundToInt

actual class CircularProgress actual constructor(context: RContext) : RView(context) {

    override val native = NCircularProgress(context.activity).apply {

    }

    override fun applyForeground(theme: Theme) {
        native.setProgressColor(theme.foreground.colorInt())
        native.setProgressBackgroundColor(theme.background.colorInt())
        native.setProgressWidth(15f)
        native.setRounded(true)
        native.setPaddingAll(0)
    }

    actual var ratio: Float
        get() = native.progress /100f
        set(value) { native.setProgress((value * 100)) }
}


class NCircularProgress(context: Context) : android.widget.ProgressBar (context) {

    private val progressPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val backgroundPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private var currentProgress = 0f
    private val rect = RectF()
    private val startAngle = -90f
    private val maxAngle = 360f
    private val maxProgress = 100

    private var diameter = 0f
    private var angle = 0f

    override fun onDraw(canvas: android.graphics.Canvas) {
        drawCircle(maxAngle, canvas, backgroundPaint)
        drawCircle(angle, canvas, progressPaint)
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        diameter = Math.min(width, height).toFloat()
        updateRect()
    }

    private fun updateRect() {
        val strokeWidth = backgroundPaint.strokeWidth
        rect.set(strokeWidth, strokeWidth, diameter - strokeWidth, diameter - strokeWidth)
    }

    private fun drawCircle(angle: Float, canvas: Canvas, paint: Paint) {
        canvas.drawArc(rect, startAngle, angle, false, paint)
    }

    private fun calculateAngle(progress: Float) = maxAngle / maxProgress * progress

    fun setProgress(@FloatRange(from = 0.0, to = 100.0) progress: Float) {
        this.progress = progress.toInt()
        angle = calculateAngle(progress)
        invalidate()
    }

    fun setProgressColor(color: Int) {
        progressPaint.color = color
        invalidate()
    }

    fun setProgressBackgroundColor(color: Int) {
        backgroundPaint.color = color
        invalidate()
    }

    fun setProgressWidth(width: Float) {
        progressPaint.strokeWidth = width
        backgroundPaint.strokeWidth = width
        updateRect()
        invalidate()
    }

    fun setRounded(rounded: Boolean) {
        progressPaint.strokeCap = if (rounded) Paint.Cap.ROUND else Paint.Cap.BUTT
        invalidate()
    }
}