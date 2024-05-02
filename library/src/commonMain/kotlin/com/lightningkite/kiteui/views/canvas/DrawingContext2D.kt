package com.lightningkite.kiteui.views.canvas

import com.lightningkite.kiteui.models.Angle
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.FontAndStyle

enum class TextAlign{
    start, end, left, right, center
}

expect abstract class DrawingContext2D {
    abstract fun save()
    abstract fun restore()
    abstract fun scale(x: Double, y: Double)
    abstract fun rotate(angle: Double)
    abstract fun translate(x: Double, y: Double)
    abstract fun transform(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double)
//  abstract   fun getTransform(): DOMMatrix
//    abstract fun setTransform(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double)
//    abstract var globalAlpha: Double
    abstract var globalCompositeOperation: String
    abstract var imageSmoothingEnabled: Boolean
//  abstract   var imageSmoothingQuality: ImageSmoothingQuality
//  abstract   var strokeStyle: dynamic
//  abstract       get()
//  abstract       set(value)
//  abstract   var fillStyle: dynamic  // String | CanvasGradient | CanvasPattern
//  abstract       get()
//  abstract       set(value)
//  abstract   fun createLinearGradient(x0: Double, y0: Double, x1: Double, y1: Double): CanvasGradient
//  abstract   fun createRadialGradient(x0: Double, y0: Double, r0: Double, x1: Double, y1: Double, r1: Double): CanvasGradient
//  abstract   fun createPattern(image: CanvasImageSource, repetition: String): CanvasPattern?
//    abstract var shadowOffsetX: Double
//    abstract var shadowOffsetY: Double
//    abstract var shadowBlur: Double
//    abstract var shadowColor: String
//    abstract var filter: String
    abstract fun clearRect(x: Double, y: Double, w: Double, h: Double)
    abstract fun fillRect(x: Double, y: Double, w: Double, h: Double)
    abstract fun strokeRect(x: Double, y: Double, w: Double, h: Double)
    abstract fun beginPath()
//  abstract   fun fill(path: Path2D, fillRule: CanvasFillRule)
    abstract fun stroke()
//  abstract   fun stroke(path: Path2D)

//  abstract   fun clip(fillRule: CanvasFillRule)
//  abstract   fun clip(path: Path2D, fillRule: CanvasFillRule)
//    abstract fun resetClip()

//  abstract   fun isPointInPath(x: Double, y: Double, fillRule: CanvasFillRule): Boolean
//  abstract   fun isPointInPath(path: Path2D, x: Double, y: Double, fillRule: CanvasFillRule): Boolean
//    abstract fun isPointInStroke(x: Double, y: Double): Boolean
//  abstract   fun isPointInStroke(path: Path2D, x: Double, y: Double): Boolean

//  abstract   fun drawFocusIfNeeded(element: Element)
//  abstract   fun drawFocusIfNeeded(path: Path2D, element: Element)

//    abstract fun scrollPathIntoView()
//  abstract   fun scrollPathIntoView(path: Path2D)

//    abstract fun fillText(text: String, x: Double, y: Double, maxWidth: Double)
//    abstract fun strokeText(text: String, x: Double, y: Double, maxWidth: Double)

//  abstract   fun measureText(text: String): TextMetrics
//    abstract var font: String
//  abstract   var textAlign: TextAlign
//  abstract   var textBaseline: CanvasTextBaseline
//  abstract   var direction: CanvasDirection

//  abstract   fun drawImage(image: CanvasImageSource, dx: Double, dy: Double)
//  abstract   fun drawImage(image: CanvasImageSource, dx: Double, dy: Double, dw: Double, dh: Double)
//  abstract   fun drawImage(image: CanvasImageSource, sx: Double, sy: Double, sw: Double, sh: Double, dx: Double, dy: Double, dw: Double, dh: Double)
//  abstract   fun createImageData(sw: Double, sh: Double): ImageData
//  abstract   fun createImageData(imagedata: ImageData): ImageData
//  abstract   fun getImageData(sx: Double, sy: Double, sw: Double, sh: Double): ImageData
//  abstract   fun putImageData(imagedata: ImageData, dx: Double, dy: Double)
//  abstract   fun putImageData(imagedata: ImageData, dx: Double, dy: Double, dirtyX: Double, dirtyY: Double, dirtyWidth: Double, dirtyHeight: Double)

//  abstract   fun addHitRegion(options: HitRegionOptions)
//    abstract fun removeHitRegion(id: String)
//    abstract fun clearHitRegions()

    abstract var lineWidth: Double
//  abstract   var lineCap: CanvasLineCap
//  abstract   var lineJoin: CanvasLineJoin
    abstract var miterLimit: Double
    abstract var lineDashOffset: Double
//    abstract fun setLineDash(segments: Array<Double>)
//    abstract fun getLineDash(): Array<Double>
    abstract fun closePath()
    abstract fun moveTo(x: Double, y: Double)
    abstract fun lineTo(x: Double, y: Double)
    abstract fun quadraticCurveTo(cpx: Double, cpy: Double, x: Double, y: Double)
    abstract fun bezierCurveTo(cp1x: Double, cp1y: Double, cp2x: Double, cp2y: Double, x: Double, y: Double)
//    abstract fun arcTo(x1: Double, y1: Double, x2: Double, y2: Double, radius: Double)
//    abstract fun arcTo(x1: Double, y1: Double, x2: Double, y2: Double, radiusX: Double, radiusY: Double, rotation: Double)
    abstract fun rect(x: Double, y: Double, w: Double, h: Double)
//    abstract fun arc(x: Double, y: Double, radius: Double, startAngle: Double, endAngle: Double, anticlockwise: Boolean)
//    abstract fun ellipse(x: Double, y: Double, radiusX: Double, radiusY: Double, rotation: Double, startAngle: Double, endAngle: Double, anticlockwise: Boolean)
}

expect fun DrawingContext2D.appendArc(x: Double, y: Double, radius: Double, startAngle: Angle, endAngle: Angle, anticlockwise: Boolean)
expect fun DrawingContext2D.drawOutlinedText(text: String, x: Double, y: Double)
expect fun DrawingContext2D.drawText(text: String, x: Double, y: Double)
expect fun DrawingContext2D.font(size: Double, value: FontAndStyle)
expect fun DrawingContext2D.textAlign(alignment: TextAlign)
expect fun DrawingContext2D.clear()
expect fun DrawingContext2D.fill()
expect fun DrawingContext2D.fillEvenOdd()
expect var DrawingContext2D.strokePaint: com.lightningkite.kiteui.models.Paint
expect var DrawingContext2D.fillPaint: com.lightningkite.kiteui.models.Paint
expect val DrawingContext2D.width: Double
expect val DrawingContext2D.height: Double
