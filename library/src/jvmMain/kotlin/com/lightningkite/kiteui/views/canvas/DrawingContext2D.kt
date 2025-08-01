package com.lightningkite.kiteui.views.canvas

import com.lightningkite.kiteui.models.*

public actual abstract class DrawingContext2D {
    public actual abstract fun save()
    public actual abstract fun restore()
    public actual abstract fun scale(x: Double, y: Double)
    public actual abstract fun rotate(angle: Double)
    public actual abstract fun translate(x: Double, y: Double)
    public actual abstract fun transform(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double)
    //  public actual abstract   fun getTransform(): DOMMatrix
    //    public actual abstract fun setTransform(transform: dynamic)
    public actual abstract var globalCompositeOperation: String
    public actual abstract var imageSmoothingEnabled: Boolean
    //  public actual abstract   var imageSmoothingQuality: ImageSmoothingQuality
//  public actual abstract   var strokeStyle: dynamic
//  public actual abstract       get()
//  public actual abstract       set(value)
//  public actual abstract   var fillStyle: dynamic  // String | CanvasGradient | CanvasPattern
//  public actual abstract       get()
//  public actual abstract       set(value)
//  public actual abstract   fun createLinearGradient(x0: Double, y0: Double, x1: Double, y1: Double): CanvasGradient
//  public actual abstract   fun createRadialGradient(x0: Double, y0: Double, r0: Double, x1: Double, y1: Double, r1: Double): CanvasGradient
//  public actual abstract   fun createPattern(image: CanvasImageSource, repetition: String): CanvasPattern?
    public actual abstract fun clearRect(x: Double, y: Double, w: Double, h: Double)
    public actual abstract fun fillRect(x: Double, y: Double, w: Double, h: Double)
    public actual abstract fun strokeRect(x: Double, y: Double, w: Double, h: Double)
    public actual abstract fun beginPath()
    //  public actual abstract   fun fill(path: Path2D, fillRule: CanvasFillRule)
    public actual abstract fun stroke()
//  public actual abstract   fun stroke(path: Path2D)

//  public actual abstract   fun clip(fillRule: CanvasFillRule)
//  public actual abstract   fun clip(path: Path2D, fillRule: CanvasFillRule)
//    public actual abstract fun resetClip()

//  public actual abstract   fun isPointInPath(x: Double, y: Double, fillRule: CanvasFillRule): Boolean
//  public actual abstract   fun isPointInPath(path: Path2D, x: Double, y: Double, fillRule: CanvasFillRule): Boolean
//    public actual abstract fun isPointInStroke(x: Double, y: Double): Boolean
//  public actual abstract   fun isPointInStroke(path: Path2D, x: Double, y: Double): Boolean

//  public actual abstract   fun drawFocusIfNeeded(element: Element)
//  public actual abstract   fun drawFocusIfNeeded(path: Path2D, element: Element)

//    public actual abstract fun scrollPathIntoView()
//  public actual abstract   fun scrollPathIntoView(path: Path2D)

//    public actual abstract fun fillText(text: String, x: Double, y: Double, maxWidth: Double)
//    public actual abstract fun strokeText(text: String, x: Double, y: Double, maxWidth: Double)

//  public actual abstract   fun measureText(text: String): TextMetrics
//    public actual abstract var font: String
//  public actual abstract   var textAlign: CanvasTextAlign
//  public actual abstract   var textBaseline: CanvasTextBaseline
//  public actual abstract   var direction: CanvasDirection

//  public actual abstract   fun drawImage(image: CanvasImageSource, dx: Double, dy: Double)
//  public actual abstract   fun drawImage(image: CanvasImageSource, dx: Double, dy: Double, dw: Double, dh: Double)
//  public actual abstract   fun drawImage(image: CanvasImageSource, sx: Double, sy: Double, sw: Double, sh: Double, dx: Double, dy: Double, dw: Double, dh: Double)
//  public actual abstract   fun createImageData(sw: Double, sh: Double): ImageData
//  public actual abstract   fun createImageData(imagedata: ImageData): ImageData
//  public actual abstract   fun getImageData(sx: Double, sy: Double, sw: Double, sh: Double): ImageData
//  public actual abstract   fun putImageData(imagedata: ImageData, dx: Double, dy: Double)
//  public actual abstract   fun putImageData(imagedata: ImageData, dx: Double, dy: Double, dirtyX: Double, dirtyY: Double, dirtyWidth: Double, dirtyHeight: Double)

//  public actual abstract   fun addHitRegion(options: HitRegionOptions)
//    public actual abstract fun removeHitRegion(id: String)
//    public actual abstract fun clearHitRegions()

    public actual abstract var lineWidth: Double
    //  public actual abstract   var lineCap: CanvasLineCap
//  public actual abstract   var lineJoin: CanvasLineJoin
    public actual abstract var miterLimit: Double
    public actual abstract var lineDashOffset: Double
    public abstract fun setLineDash(segments: Array<Double>)
    public abstract fun getLineDash(): Array<Double>
    public actual abstract fun closePath()
    public actual abstract fun moveTo(x: Double, y: Double)
    public actual abstract fun lineTo(x: Double, y: Double)
    public actual abstract fun quadraticCurveTo(cpx: Double, cpy: Double, x: Double, y: Double)
    public actual abstract fun bezierCurveTo(cp1x: Double, cp1y: Double, cp2x: Double, cp2y: Double, x: Double, y: Double)
//    public actual abstract fun arcTo(x1: Double, y1: Double, x2: Double, y2: Double, radius: Double)
//    public actual abstract fun arcTo(x1: Double, y1: Double, x2: Double, y2: Double, radiusX: Double, radiusY: Double, rotation: Double)
    public actual abstract fun rect(x: Double, y: Double, w: Double, h: Double)
//    public actual abstract fun arc(x: Double, y: Double, radius: Double, startAngle: Double, endAngle: Double, anticlockwise: Boolean)
//    public actual abstract fun ellipse(x: Double, y: Double, radiusX: Double, radiusY: Double, rotation: Double, startAngle: Double, endAngle: Double, anticlockwise: Boolean)
}

public actual fun DrawingContext2D.appendArc(x: Double, y: Double, radius: Double, startAngle: Angle, endAngle: Angle, anticlockwise: Boolean): Unit = TODO()
public actual fun DrawingContext2D.drawText(text: String, x: Double, y: Double):Unit = TODO()
public actual fun DrawingContext2D.drawOutlinedText(text: String, x: Double, y: Double):Unit = TODO()
public actual fun DrawingContext2D.font(size: Double, value: FontAndStyle):Unit = TODO()
public actual fun DrawingContext2D.textAlign(alignment: TextAlign):Unit = TODO()
public actual fun DrawingContext2D.fill(): Unit = TODO()
public actual fun DrawingContext2D.fillEvenOdd(): Unit = TODO()
public actual var DrawingContext2D.strokePaint: Paint
    get() = TODO()
    set(value) { TODO() }
public actual var DrawingContext2D.fillPaint: Paint
    get() = TODO()
    set(value) { TODO() }
public actual val DrawingContext2D.width: Double get() = TODO()
public actual val DrawingContext2D.height: Double get() = TODO()
