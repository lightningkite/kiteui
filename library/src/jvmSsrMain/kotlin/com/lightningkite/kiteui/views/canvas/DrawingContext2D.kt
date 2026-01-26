@file:JvmName("DrawingContext2DSsrKt")
package com.lightningkite.kiteui.views.canvas

import com.lightningkite.kiteui.models.*

actual abstract class DrawingContext2D {
    actual abstract fun save()
    actual abstract fun restore()
    actual abstract fun scale(x: Double, y: Double)
    actual abstract fun rotate(angle: Double)
    actual abstract fun translate(x: Double, y: Double)
    actual abstract fun transform(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double)
    actual abstract var globalAlpha: Double
    actual abstract var globalCompositeOperation: String
    actual abstract var imageSmoothingEnabled: Boolean
    actual abstract fun clearRect(x: Double, y: Double, w: Double, h: Double)
    actual abstract fun fillRect(x: Double, y: Double, w: Double, h: Double)
    actual abstract fun strokeRect(x: Double, y: Double, w: Double, h: Double)
    actual abstract fun beginPath()
    actual abstract fun stroke()
    actual abstract var lineWidth: Double
    actual abstract var miterLimit: Double
    actual abstract var lineDashOffset: Double
    abstract fun setLineDash(segments: Array<Double>)
    abstract fun getLineDash(): Array<Double>
    actual abstract fun closePath()
    actual abstract fun moveTo(x: Double, y: Double)
    actual abstract fun lineTo(x: Double, y: Double)
    actual abstract fun quadraticCurveTo(cpx: Double, cpy: Double, x: Double, y: Double)
    actual abstract fun bezierCurveTo(cp1x: Double, cp1y: Double, cp2x: Double, cp2y: Double, x: Double, y: Double)
    actual abstract fun rect(x: Double, y: Double, w: Double, h: Double)
}

/**
 * SSR stub implementation of DrawingContext2D.
 * Since SSR doesn't render graphics, this provides no-op implementations.
 */
class DrawingContext2DStub(
    private val _width: Double = 0.0,
    private val _height: Double = 0.0
) : DrawingContext2D() {
    override fun save() {}
    override fun restore() {}
    override fun scale(x: Double, y: Double) {}
    override fun rotate(angle: Double) {}
    override fun translate(x: Double, y: Double) {}
    override fun transform(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double) {}
    override var globalAlpha: Double = 1.0
    override var globalCompositeOperation: String = "source-over"
    override var imageSmoothingEnabled: Boolean = true
    override fun clearRect(x: Double, y: Double, w: Double, h: Double) {}
    override fun fillRect(x: Double, y: Double, w: Double, h: Double) {}
    override fun strokeRect(x: Double, y: Double, w: Double, h: Double) {}
    override fun beginPath() {}
    override fun stroke() {}
    override var lineWidth: Double = 1.0
    override var miterLimit: Double = 10.0
    override var lineDashOffset: Double = 0.0
    override fun setLineDash(segments: Array<Double>) {}
    override fun getLineDash(): Array<Double> = emptyArray()
    override fun closePath() {}
    override fun moveTo(x: Double, y: Double) {}
    override fun lineTo(x: Double, y: Double) {}
    override fun quadraticCurveTo(cpx: Double, cpy: Double, x: Double, y: Double) {}
    override fun bezierCurveTo(cp1x: Double, cp1y: Double, cp2x: Double, cp2y: Double, x: Double, y: Double) {}
    override fun rect(x: Double, y: Double, w: Double, h: Double) {}

    // SSR stub state
    var _fillPaint: Paint = Color.black
    var _strokePaint: Paint = Color.black
    var _lineCap: LineCap = LineCap.butt
    var _lineJoin: LineJoin = LineJoin.miter
    val widthValue: Double get() = _width
    val heightValue: Double get() = _height

    // Shadow properties
    var _shadowBlur: Double = 0.0
    var _shadowColor: Color = Color.transparent
    var _shadowOffsetX: Double = 0.0
    var _shadowOffsetY: Double = 0.0
}

actual fun DrawingContext2D.appendArc(x: Double, y: Double, radius: Double, startAngle: Angle, endAngle: Angle, anticlockwise: Boolean) {}
actual fun DrawingContext2D.drawText(text: String, x: Double, y: Double) {}
actual fun DrawingContext2D.drawOutlinedText(text: String, x: Double, y: Double) {}
actual fun DrawingContext2D.font(size: Double, value: FontAndStyle) {}
actual fun DrawingContext2D.textAlign(alignment: TextAlign) {}
actual fun DrawingContext2D.fill() {}
actual fun DrawingContext2D.fillEvenOdd() {}

actual var DrawingContext2D.strokePaint: Paint
    get() = (this as? DrawingContext2DStub)?._strokePaint ?: Color.black
    set(value) { (this as? DrawingContext2DStub)?._strokePaint = value }

actual var DrawingContext2D.fillPaint: Paint
    get() = (this as? DrawingContext2DStub)?._fillPaint ?: Color.black
    set(value) { (this as? DrawingContext2DStub)?._fillPaint = value }

actual var DrawingContext2D.lineCapStyle: LineCap
    get() = (this as? DrawingContext2DStub)?._lineCap ?: LineCap.butt
    set(value) { (this as? DrawingContext2DStub)?._lineCap = value }

actual var DrawingContext2D.lineJoinStyle: LineJoin
    get() = (this as? DrawingContext2DStub)?._lineJoin ?: LineJoin.miter
    set(value) { (this as? DrawingContext2DStub)?._lineJoin = value }

actual val DrawingContext2D.width: Double
    get() = (this as? DrawingContext2DStub)?.widthValue ?: 0.0

actual val DrawingContext2D.height: Double
    get() = (this as? DrawingContext2DStub)?.heightValue ?: 0.0

// ============================================================================
// Clipping - no-op implementations for SSR
// ============================================================================

actual fun DrawingContext2D.clip() {}

actual fun DrawingContext2D.clip(fillRule: FillRule) {}

actual fun DrawingContext2D.resetClip() {}

// ============================================================================
// Line Dash
// ============================================================================

actual fun DrawingContext2D.setLineDash(segments: List<Double>) {}

actual fun DrawingContext2D.getLineDash(): List<Double> = emptyList()

// ============================================================================
// Text Metrics
// ============================================================================

actual fun DrawingContext2D.measureText(text: String): TextMetrics = TextMetrics(width = 0.0)

// ============================================================================
// Shapes - no-op implementations for SSR
// ============================================================================

actual fun DrawingContext2D.roundRect(x: Double, y: Double, width: Double, height: Double, radius: Double) {}

actual fun DrawingContext2D.roundRect(
    x: Double, y: Double, width: Double, height: Double,
    topLeftRadius: Double, topRightRadius: Double,
    bottomRightRadius: Double, bottomLeftRadius: Double
) {}

actual fun DrawingContext2D.ellipse(
    x: Double, y: Double,
    radiusX: Double, radiusY: Double,
    rotation: Double,
    startAngle: Double, endAngle: Double,
    anticlockwise: Boolean
) {}

// ============================================================================
// Hit Testing - always return false for SSR
// ============================================================================

actual fun DrawingContext2D.isPointInPath(x: Double, y: Double): Boolean = false

actual fun DrawingContext2D.isPointInPath(x: Double, y: Double, fillRule: FillRule): Boolean = false

// ============================================================================
// Shadows
// ============================================================================

actual var DrawingContext2D.shadowBlur: Double
    get() = (this as? DrawingContext2DStub)?._shadowBlur ?: 0.0
    set(value) { (this as? DrawingContext2DStub)?._shadowBlur = value }

actual var DrawingContext2D.shadowColorValue: Color
    get() = (this as? DrawingContext2DStub)?._shadowColor ?: Color.transparent
    set(value) { (this as? DrawingContext2DStub)?._shadowColor = value }

actual var DrawingContext2D.shadowOffsetX: Double
    get() = (this as? DrawingContext2DStub)?._shadowOffsetX ?: 0.0
    set(value) { (this as? DrawingContext2DStub)?._shadowOffsetX = value }

actual var DrawingContext2D.shadowOffsetY: Double
    get() = (this as? DrawingContext2DStub)?._shadowOffsetY ?: 0.0
    set(value) { (this as? DrawingContext2DStub)?._shadowOffsetY = value }

// ============================================================================
// Transform
// ============================================================================

actual fun DrawingContext2D.getTransform(): TransformMatrix = TransformMatrix.identity

actual fun DrawingContext2D.setTransform(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double) {}
