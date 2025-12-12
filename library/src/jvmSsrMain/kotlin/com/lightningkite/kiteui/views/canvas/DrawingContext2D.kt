package com.lightningkite.kiteui.views.canvas

import com.lightningkite.kiteui.models.*

actual abstract class DrawingContext2D {
    actual abstract fun save()
    actual abstract fun restore()
    actual abstract fun scale(x: Double, y: Double)
    actual abstract fun rotate(angle: Double)
    actual abstract fun translate(x: Double, y: Double)
    actual abstract fun transform(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double)
    //  actual abstract   fun getTransform(): DOMMatrix
    //    actual abstract fun setTransform(transform: dynamic)
    actual abstract var globalCompositeOperation: String
    actual abstract var imageSmoothingEnabled: Boolean
    //  actual abstract   var imageSmoothingQuality: ImageSmoothingQuality
//  actual abstract   var strokeStyle: dynamic
//  actual abstract       get()
//  actual abstract       set(value)
//  actual abstract   var fillStyle: dynamic  // String | CanvasGradient | CanvasPattern
//  actual abstract       get()
//  actual abstract       set(value)
//  actual abstract   fun createLinearGradient(x0: Double, y0: Double, x1: Double, y1: Double): CanvasGradient
//  actual abstract   fun createRadialGradient(x0: Double, y0: Double, r0: Double, x1: Double, y1: Double, r1: Double): CanvasGradient
//  actual abstract   fun createPattern(image: CanvasImageSource, repetition: String): CanvasPattern?
    actual abstract fun clearRect(x: Double, y: Double, w: Double, h: Double)
    actual abstract fun fillRect(x: Double, y: Double, w: Double, h: Double)
    actual abstract fun strokeRect(x: Double, y: Double, w: Double, h: Double)
    actual abstract fun beginPath()
    //  actual abstract   fun fill(path: Path2D, fillRule: CanvasFillRule)
    actual abstract fun stroke()
//  actual abstract   fun stroke(path: Path2D)

//  actual abstract   fun clip(fillRule: CanvasFillRule)
//  actual abstract   fun clip(path: Path2D, fillRule: CanvasFillRule)
//    actual abstract fun resetClip()

//  actual abstract   fun isPointInPath(x: Double, y: Double, fillRule: CanvasFillRule): Boolean
//  actual abstract   fun isPointInPath(path: Path2D, x: Double, y: Double, fillRule: CanvasFillRule): Boolean
//    actual abstract fun isPointInStroke(x: Double, y: Double): Boolean
//  actual abstract   fun isPointInStroke(path: Path2D, x: Double, y: Double): Boolean

//  actual abstract   fun drawFocusIfNeeded(element: Element)
//  actual abstract   fun drawFocusIfNeeded(path: Path2D, element: Element)

//    actual abstract fun scrollPathIntoView()
//  actual abstract   fun scrollPathIntoView(path: Path2D)

//    actual abstract fun fillText(text: String, x: Double, y: Double, maxWidth: Double)
//    actual abstract fun strokeText(text: String, x: Double, y: Double, maxWidth: Double)

//  actual abstract   fun measureText(text: String): TextMetrics
//    actual abstract var font: String
//  actual abstract   var textAlign: CanvasTextAlign
//  actual abstract   var textBaseline: CanvasTextBaseline
//  actual abstract   var direction: CanvasDirection

//  actual abstract   fun drawImage(image: CanvasImageSource, dx: Double, dy: Double)
//  actual abstract   fun drawImage(image: CanvasImageSource, dx: Double, dy: Double, dw: Double, dh: Double)
//  actual abstract   fun drawImage(image: CanvasImageSource, sx: Double, sy: Double, sw: Double, sh: Double, dx: Double, dy: Double, dw: Double, dh: Double)
//  actual abstract   fun createImageData(sw: Double, sh: Double): ImageData
//  actual abstract   fun createImageData(imagedata: ImageData): ImageData
//  actual abstract   fun getImageData(sx: Double, sy: Double, sw: Double, sh: Double): ImageData
//  actual abstract   fun putImageData(imagedata: ImageData, dx: Double, dy: Double)
//  actual abstract   fun putImageData(imagedata: ImageData, dx: Double, dy: Double, dirtyX: Double, dirtyY: Double, dirtyWidth: Double, dirtyHeight: Double)

//  actual abstract   fun addHitRegion(options: HitRegionOptions)
//    actual abstract fun removeHitRegion(id: String)
//    actual abstract fun clearHitRegions()

    actual abstract var lineWidth: Double
    //  actual abstract   var lineCap: CanvasLineCap
//  actual abstract   var lineJoin: CanvasLineJoin
    actual abstract var miterLimit: Double
    actual abstract var lineDashOffset: Double
    abstract fun setLineDash(segments: Array<Double>)
    abstract fun getLineDash(): Array<Double>
    actual abstract fun closePath()
    actual abstract fun moveTo(x: Double, y: Double)
    actual abstract fun lineTo(x: Double, y: Double)
    actual abstract fun quadraticCurveTo(cpx: Double, cpy: Double, x: Double, y: Double)
    actual abstract fun bezierCurveTo(cp1x: Double, cp1y: Double, cp2x: Double, cp2y: Double, x: Double, y: Double)
//    actual abstract fun arcTo(x1: Double, y1: Double, x2: Double, y2: Double, radius: Double)
//    actual abstract fun arcTo(x1: Double, y1: Double, x2: Double, y2: Double, radiusX: Double, radiusY: Double, rotation: Double)
    actual abstract fun rect(x: Double, y: Double, w: Double, h: Double)
//    actual abstract fun arc(x: Double, y: Double, radius: Double, startAngle: Double, endAngle: Double, anticlockwise: Boolean)
//    actual abstract fun ellipse(x: Double, y: Double, radiusX: Double, radiusY: Double, rotation: Double, startAngle: Double, endAngle: Double, anticlockwise: Boolean)
}

actual fun DrawingContext2D.appendArc(x: Double, y: Double, radius: Double, startAngle: Angle, endAngle: Angle, anticlockwise: Boolean): Unit = TODO()
actual fun DrawingContext2D.drawText(text: String, x: Double, y: Double):Unit = TODO()
actual fun DrawingContext2D.drawOutlinedText(text: String, x: Double, y: Double):Unit = TODO()
actual fun DrawingContext2D.font(size: Double, value: FontAndStyle):Unit = TODO()
actual fun DrawingContext2D.textAlign(alignment: TextAlign):Unit = TODO()
actual fun DrawingContext2D.fill(): Unit = TODO()
actual fun DrawingContext2D.fillEvenOdd(): Unit = TODO()
actual var DrawingContext2D.strokePaint: Paint
    get() = TODO()
    set(value) { TODO() }
actual var DrawingContext2D.fillPaint: Paint
    get() = TODO()
    set(value) { TODO() }
actual val DrawingContext2D.width: Double get() = TODO()
actual val DrawingContext2D.height: Double get() = TODO()
