package com.lightningkite.kiteui.views.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import com.lightningkite.kiteui.models.Angle
import com.lightningkite.kiteui.models.FontAndStyle
import com.lightningkite.kiteui.models.Paint
import com.lightningkite.kiteui.models.toBrush
import com.lightningkite.kiteui.models.toCompose

@OptIn(ExperimentalTextApi::class)
class DrawingContext2DImpl(val drawScope: DrawScope) : DrawingContext2D() {
    private val path = Path()
    private var currentStrokePaint: Paint = com.lightningkite.kiteui.models.Color.black
    private var currentFillPaint: Paint = com.lightningkite.kiteui.models.Color.black
    private var currentFont: FontAndStyle? = null
    private var currentTextAlign: TextAlign = TextAlign.start
    internal lateinit var textMeasurer: TextMeasurer

    override fun save() {}
    override fun restore() {}
    override fun scale(x: Double, y: Double) {}
    override fun rotate(angle: Double) {}
    override fun translate(x: Double, y: Double) {}
    override fun transform(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double) {}

    override var globalCompositeOperation: String = "source-over"
    override var imageSmoothingEnabled: Boolean = true

    override fun clearRect(x: Double, y: Double, w: Double, h: Double) {
        // Attempt to clear using transparent draw with Clear blend mode
        drawScope.drawRect(
            color = androidx.compose.ui.graphics.Color.Transparent,
            topLeft = Offset(x.toFloat(), y.toFloat()),
            size = Size(w.toFloat(), h.toFloat())
        )
    }
    override fun fillRect(x: Double, y: Double, w: Double, h: Double) {
        currentFillPaint.toBrush()?.let { brush ->
            drawScope.drawRect(
                brush = brush,
                topLeft = Offset(x.toFloat(), y.toFloat()),
                size = androidx.compose.ui.geometry.Size(w.toFloat(), h.toFloat())
            )
        }
    }
    override fun strokeRect(x: Double, y: Double, w: Double, h: Double) {
        currentStrokePaint.toBrush()?.let { brush ->
            drawScope.drawRect(
                brush = brush,
                topLeft = Offset(x.toFloat(), y.toFloat()),
                size = androidx.compose.ui.geometry.Size(w.toFloat(), h.toFloat()),
                style = Stroke(width = lineWidth.toFloat())
            )
        }
    }
    override fun beginPath() {
        path.reset()
    }
    override fun stroke() {
        currentStrokePaint.toBrush()?.let { brush ->
            drawScope.drawPath(path = path, brush = brush, style = Stroke(width = lineWidth.toFloat()))
        }
    }

    override var lineWidth: Double = 1.0
    override var miterLimit: Double = 4.0
    override var lineDashOffset: Double = 0.0
    private var lineDashSegments: Array<Double> = emptyArray()
    override fun setLineDash(segments: Array<Double>) { lineDashSegments = segments }
    override fun getLineDash(): Array<Double> = lineDashSegments

    override fun closePath() { path.close() }
    override fun moveTo(x: Double, y: Double) { path.moveTo(x.toFloat(), y.toFloat()) }
    override fun lineTo(x: Double, y: Double) { path.lineTo(x.toFloat(), y.toFloat()) }
    override fun quadraticCurveTo(cpx: Double, cpy: Double, x: Double, y: Double) { path.quadraticBezierTo(cpx.toFloat(), cpy.toFloat(), x.toFloat(), y.toFloat()) }
    override fun bezierCurveTo(cp1x: Double, cp1y: Double, cp2x: Double, cp2y: Double, x: Double, y: Double) { path.cubicTo(cp1x.toFloat(), cp1y.toFloat(), cp2x.toFloat(), cp2y.toFloat(), x.toFloat(), y.toFloat()) }
    override fun rect(x: Double, y: Double, w: Double, h: Double) { path.addRect(androidx.compose.ui.geometry.Rect(x.toFloat(), y.toFloat(), (x+w).toFloat(), (y+h).toFloat())) }

    fun appendArc(x: Double, y: Double, radius: Double, startAngle: Angle, endAngle: Angle, anticlockwise: Boolean) {
        path.arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                left = (x - radius).toFloat(),
                top = (y - radius).toFloat(),
                right = (x + radius).toFloat(),
                bottom = (y + radius).toFloat()
            ),
            startAngleDegrees = startAngle.degrees.toFloat(),
            sweepAngleDegrees = (endAngle - startAngle).degrees.toFloat(),
            forceMoveTo = false
        )
    }

    fun drawText(text: String, x: Double, y: Double) {
        currentFillPaint.toBrush()?.let { brush ->
            val layout = textMeasurer.measure(text = text, style = TextStyle())
            val offsetX = when (currentTextAlign) {
                TextAlign.center -> x.toFloat() - layout.size.width / 2f
                TextAlign.end, TextAlign.right -> x.toFloat() - layout.size.width.toFloat()
                else -> x.toFloat()
            }
        
            drawScope.drawText(
                textMeasurer = textMeasurer,
                text = text,
                topLeft = Offset(offsetX, y.toFloat()),
                style = TextStyle(brush = brush)
            )
        }
    }

    fun drawOutlinedText(text: String, x: Double, y: Double) {
        currentStrokePaint.toBrush()?.let { brush ->
            val layout = textMeasurer.measure(text = text, style = TextStyle())
            val offsetX = when (currentTextAlign) {
                TextAlign.center -> x.toFloat() - layout.size.width / 2f
                TextAlign.end, TextAlign.right -> x.toFloat() - layout.size.width.toFloat()
                else -> x.toFloat()
            }
            drawScope.drawText(
                textMeasurer = textMeasurer,
                text = text,
                topLeft = Offset(offsetX, y.toFloat()),
                style = TextStyle(brush = brush)
            )
        }
    }

    fun font(size: Double, value: FontAndStyle) {
        currentFont = value
        // Not fully supported without TextStyle font family mapping
    }

    fun textAlign(alignment: TextAlign) { currentTextAlign = alignment }

    fun fill() { currentFillPaint.toBrush()?.let { brush -> drawScope.drawPath(path = path, brush = brush, style = Fill, alpha = 1f) } }
    fun fillEvenOdd() { currentFillPaint.toBrush()?.let { brush -> drawScope.drawPath(path = path, brush = brush, style = Fill, alpha = 1f) } }  //TODO: We can't distinguish between evenOdd and non-zero.  This is a problem

    var strokePaint: Paint
        get() = currentStrokePaint
        set(value) { currentStrokePaint = value }
    var fillPaint: Paint
        get() = currentFillPaint
        set(value) { currentFillPaint = value }

    val width: Double get() = drawScope.size.width.toDouble()
    val height: Double get() = drawScope.size.height.toDouble()
}
























