package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import androidx.annotation.FloatRange
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement

public actual class CircularProgress actual constructor(context: ElementContext) : NativeElement(context) {

    override val native: NCircularProgress = NCircularProgress(context.activity).apply {
        contentDescription = "Progress"
        accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
    }

    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.nativeApplyTheme(theme)
        val theme = theme.theme
        native.setProgressColor(theme.foreground.colorInt())
        native.setProgressBackgroundColor(theme.background.colorInt())
        native.setProgressWidth(15f)
        native.setRounded(true)
        native.setPaddingAll(0)
    }

    public actual var ratio: Float
        get() = native.progress /100f
        set(value) { native.setProgress((value * 100)) }
}


public class NCircularProgress(context: Context) : android.widget.ProgressBar (context) {

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

    internal fun setProgress(@FloatRange(from = 0.0, to = 100.0) progress: Float) {
        this.progress = progress.toInt()
        angle = calculateAngle(progress)
        invalidate()
    }

    internal fun setProgressColor(color: Int) {
        progressPaint.color = color
        invalidate()
    }

    internal fun setProgressBackgroundColor(color: Int) {
        backgroundPaint.color = color
        invalidate()
    }

    internal fun setProgressWidth(width: Float) {
        progressPaint.strokeWidth = width
        backgroundPaint.strokeWidth = width
        updateRect()
        invalidate()
    }

    internal fun setRounded(rounded: Boolean) {
        progressPaint.strokeCap = if (rounded) Paint.Cap.ROUND else Paint.Cap.BUTT
        invalidate()
    }
}