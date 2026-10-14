package com.lightningkite.kiteui.views.canvas

import com.lightningkite.kiteui.models.Angle
import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.FontAndStyle
import com.lightningkite.kiteui.models.Paint

public actual abstract class DrawingContext2D {
    init { throw NotImplementedError("Compose backend: DrawingContext2D is not implemented yet") }

    public actual abstract fun save()
    public actual abstract fun restore()
    public actual abstract fun scale(x: Double, y: Double)
    public actual abstract fun rotate(angle: Double)
    public actual abstract fun translate(x: Double, y: Double)
    public actual abstract fun transform(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double)
    public actual abstract var globalAlpha: Double
    public actual abstract var globalCompositeOperation: String
    public actual abstract var imageSmoothingEnabled: Boolean
    public actual abstract fun clearRect(x: Double, y: Double, w: Double, h: Double)
    public actual abstract fun fillRect(x: Double, y: Double, w: Double, h: Double)
    public actual abstract fun strokeRect(x: Double, y: Double, w: Double, h: Double)
    public actual abstract fun beginPath()
    public actual abstract fun stroke()
    public actual abstract var lineWidth: Double
    public actual abstract var miterLimit: Double
    public actual abstract var lineDashOffset: Double
    public actual abstract fun closePath()
    public actual abstract fun moveTo(x: Double, y: Double)
    public actual abstract fun lineTo(x: Double, y: Double)
    public actual abstract fun quadraticCurveTo(cpx: Double, cpy: Double, x: Double, y: Double)
    public actual abstract fun bezierCurveTo(cp1x: Double, cp1y: Double, cp2x: Double, cp2y: Double, x: Double, y: Double)
    public actual abstract fun rect(x: Double, y: Double, w: Double, h: Double)
}

public actual fun DrawingContext2D.appendArc(x: Double, y: Double, radius: Double, startAngle: Angle, endAngle: Angle, anticlockwise: Boolean): Unit = TODO("Compose backend: appendArc")
public actual fun DrawingContext2D.drawOutlinedText(text: String, x: Double, y: Double): Unit = TODO("Compose backend: drawOutlinedText")
public actual fun DrawingContext2D.drawText(text: String, x: Double, y: Double): Unit = TODO("Compose backend: drawText")
public actual fun DrawingContext2D.font(size: Double, value: FontAndStyle): Unit = TODO("Compose backend: font")
public actual fun DrawingContext2D.textAlign(alignment: TextAlign): Unit = TODO("Compose backend: textAlign")
public actual fun DrawingContext2D.clear(): Unit = TODO("Compose backend: clear")
public actual fun DrawingContext2D.fill(): Unit = TODO("Compose backend: fill")
public actual fun DrawingContext2D.fillEvenOdd(): Unit = TODO("Compose backend: fillEvenOdd")
public actual var DrawingContext2D.strokePaint: Paint
    get() = TODO("Compose backend: strokePaint")
    set(value) = TODO("Compose backend: strokePaint")
public actual var DrawingContext2D.fillPaint: Paint
    get() = TODO("Compose backend: fillPaint")
    set(value) = TODO("Compose backend: fillPaint")
public actual var DrawingContext2D.lineCapStyle: LineCap
    get() = TODO("Compose backend: lineCapStyle")
    set(value) = TODO("Compose backend: lineCapStyle")
public actual var DrawingContext2D.lineJoinStyle: LineJoin
    get() = TODO("Compose backend: lineJoinStyle")
    set(value) = TODO("Compose backend: lineJoinStyle")
public actual val DrawingContext2D.width: Double get() = TODO("Compose backend: width")
public actual val DrawingContext2D.height: Double get() = TODO("Compose backend: height")

public actual fun DrawingContext2D.clip(): Unit = TODO("Compose backend: clip")
public actual fun DrawingContext2D.clip(fillRule: FillRule): Unit = TODO("Compose backend: clip")
public actual fun DrawingContext2D.resetClip(): Unit = TODO("Compose backend: resetClip")
public actual fun DrawingContext2D.setLineDash(segments: List<Double>): Unit = TODO("Compose backend: setLineDash")
public actual fun DrawingContext2D.getLineDash(): List<Double> = TODO("Compose backend: getLineDash")
public actual fun DrawingContext2D.measureText(text: String): TextMetrics = TODO("Compose backend: measureText")
public actual fun DrawingContext2D.roundRect(x: Double, y: Double, width: Double, height: Double, radius: Double): Unit = TODO("Compose backend: roundRect")
public actual fun DrawingContext2D.roundRect(
    x: Double, y: Double, width: Double, height: Double,
    topLeftRadius: Double, topRightRadius: Double,
    bottomRightRadius: Double, bottomLeftRadius: Double
): Unit = TODO("Compose backend: roundRect")
public actual fun DrawingContext2D.ellipse(
    x: Double, y: Double,
    radiusX: Double, radiusY: Double,
    rotation: Double,
    startAngle: Double, endAngle: Double,
    anticlockwise: Boolean
): Unit = TODO("Compose backend: ellipse")
public actual fun DrawingContext2D.isPointInPath(x: Double, y: Double): Boolean = TODO("Compose backend: isPointInPath")
public actual fun DrawingContext2D.isPointInPath(x: Double, y: Double, fillRule: FillRule): Boolean = TODO("Compose backend: isPointInPath")
public actual var DrawingContext2D.shadowBlur: Double
    get() = TODO("Compose backend: shadowBlur")
    set(value) = TODO("Compose backend: shadowBlur")
public actual var DrawingContext2D.shadowColorValue: Color
    get() = TODO("Compose backend: shadowColorValue")
    set(value) = TODO("Compose backend: shadowColorValue")
public actual var DrawingContext2D.shadowOffsetX: Double
    get() = TODO("Compose backend: shadowOffsetX")
    set(value) = TODO("Compose backend: shadowOffsetX")
public actual var DrawingContext2D.shadowOffsetY: Double
    get() = TODO("Compose backend: shadowOffsetY")
    set(value) = TODO("Compose backend: shadowOffsetY")
public actual fun DrawingContext2D.getTransform(): TransformMatrix = TODO("Compose backend: getTransform")
public actual fun DrawingContext2D.setTransform(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double): Unit = TODO("Compose backend: setTransform")
