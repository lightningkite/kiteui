package com.lightningkite.kiteui.views.canvas

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.direct.addLine
import com.lightningkite.kiteui.views.direct.arcTo
import com.lightningkite.kiteui.views.toUIFontWeight
import com.lightningkite.kiteui.views.toUiColor
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.*
import platform.Foundation.NSAttributedStringKey
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.UIKit.*
import kotlin.math.*

public actual abstract class DrawingContext2D {
    public actual abstract fun save()
    public actual abstract fun restore()
    public actual abstract fun scale(x: Double, y: Double)
    public actual abstract fun rotate(angle: Double)
    public actual abstract fun translate(x: Double, y: Double)
    public actual abstract fun transform(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double)

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
    abstract fun setLineDash(segments: Array<Double>)
    abstract fun getLineDash(): Array<Double>
    public actual abstract fun closePath()
    public actual abstract fun moveTo(x: Double, y: Double)
    public actual abstract fun lineTo(x: Double, y: Double)
    public actual abstract fun quadraticCurveTo(cpx: Double, cpy: Double, x: Double, y: Double)
    public actual abstract fun bezierCurveTo(cp1x: Double, cp1y: Double, cp2x: Double, cp2y: Double, x: Double, y: Double)
//    public actual abstract fun arcTo(x1: Double, y1: Double, x2: Double, y2: Double, radius: Double)
//    public actual abstract fun arcTo(
//        x1: Double,
//        y1: Double,
//        x2: Double,
//        y2: Double,
//        radiusX: Double,
//        radiusY: Double,
//        rotation: Double
//    )

    public actual abstract fun rect(x: Double, y: Double, w: Double, h: Double)
//    public actual abstract fun arc(x: Double, y: Double, radius: Double, startAngle: Double, endAngle: Double, anticlockwise: Boolean)
//    public actual abstract fun ellipse(x: Double, y: Double, radiusX: Double, radiusY: Double, rotation: Double, startAngle: Double, endAngle: Double, anticlockwise: Boolean)
}


class DrawingContext2DImpl(val wraps: CGContextRef, val width: Double, val height: Double) : DrawingContext2D() {
    override fun save() = CGContextSaveGState(wraps)
    override fun restore() = CGContextRestoreGState(wraps)
    override fun scale(x: Double, y: Double) = CGContextScaleCTM(wraps, x, y)
    override fun rotate(angle: Double) = CGContextRotateCTM(wraps, angle)
    override fun translate(x: Double, y: Double) = CGContextTranslateCTM(wraps, x, y)
    override fun transform(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double) = CGContextConcatCTM(
        wraps, CGAffineTransformMake(
            a, b, c, d, e, f
        )
    )

    override var globalCompositeOperation: String
        get() = TODO("Not yet implemented")
        set(value) {}
    override var imageSmoothingEnabled: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun clearRect(x: Double, y: Double, w: Double, h: Double) =
        CGContextClearRect(wraps, CGRectMake(x, y, w, h))

    override fun fillRect(x: Double, y: Double, w: Double, h: Double) = CGContextFillRect(wraps, CGRectMake(x, y, w, h))
    override fun strokeRect(x: Double, y: Double, w: Double, h: Double) =
        CGContextStrokeRect(wraps, CGRectMake(x, y, w, h))

    override fun beginPath() = CGContextBeginPath(wraps)
    override fun stroke() = CGContextStrokePath(wraps)
    private var lastLineWidth = 0.0
    override var lineWidth: Double
        get() = lastLineWidth
        set(value) {
            lastLineWidth = value
            CGContextSetLineWidth(wraps, value)
        }
    override var miterLimit: Double
        get() = TODO("Not yet implemented")
        set(value) {
            CGContextSetMiterLimit(wraps, value)
        }
    override var lineDashOffset: Double
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun setLineDash(segments: Array<Double>) = TODO()
    override fun getLineDash(): Array<Double> = TODO()
    override fun closePath() = CGContextClosePath(wraps)
    override fun moveTo(x: Double, y: Double) = CGContextMoveToPoint(wraps, x, y)
    override fun lineTo(x: Double, y: Double) = CGContextAddLineToPoint(wraps, x, y)
    override fun quadraticCurveTo(cpx: Double, cpy: Double, x: Double, y: Double) =
        CGContextAddQuadCurveToPoint(wraps, cpx, cpy, x, y)

    override fun bezierCurveTo(cp1x: Double, cp1y: Double, cp2x: Double, cp2y: Double, x: Double, y: Double) =
        CGContextAddCurveToPoint(wraps, cp1x, cp1y, cp2x, cp2y, x, y)

//    override fun arcTo(x1: Double, y1: Double, x2: Double, y2: Double, radius: Double) = arcTo(
//        x1, y1, x2, y2, radius, radius, 0.0
//    )
//
//    override fun arcTo(
//        x1: Double,
//        y1: Double,
//        x2: Double,
//        y2: Double,
//        radiusX: Double,
//        radiusY: Double,
//        rotation: Double
//    ) = TODO()

    override fun rect(x: Double, y: Double, w: Double, h: Double) = CGContextAddRect(wraps, CGRectMake(x, y, w, h))

    internal var textAlign: TextAlign = TextAlign.start
    internal var font: UIFont = UIFont.systemFontOfSize(12.0)
    internal var fill: Paint = Color.black
    internal var stroke: Paint = Color.black
}


public actual fun DrawingContext2D.appendArc(
    x: Double,
    y: Double,
    radius: Double,
    startAngle: Angle,
    endAngle: Angle,
    anticlockwise: Boolean
): Unit {
    CGContextAddArc(
        (this as DrawingContext2DImpl).wraps,
        x,
        y,
        radius,
        startAngle.radians.toDouble(),
        endAngle.radians.toDouble(),
        if (anticlockwise) 1 else 0
    )
}


public actual fun DrawingContext2D.drawText(text: String, x: Double, y: Double): Unit {
    val attrs = mapOf<Any?, Any?>(
        NSFontAttributeName to (this as DrawingContext2DImpl).font,
        NSForegroundColorAttributeName to this.fill.closestColor().toUiColor(),
        NSParagraphStyleAttributeName to NSMutableParagraphStyle().apply {
            setAlignment(
                when ((this@drawText as DrawingContext2DImpl).textAlign) {
                    TextAlign.start -> NSTextAlignmentLeft
                    TextAlign.end -> NSTextAlignmentRight
                    TextAlign.left -> NSTextAlignmentLeft
                    TextAlign.right -> NSTextAlignmentRight
                    TextAlign.center -> NSTextAlignmentCenter
                }
            )
        }
    )
    val ns = (text as NSString)
    val sizeTaken = ns.sizeWithAttributes(attrs).useContents { width }
    val height = font.lineHeight
    var dx = x
    var dy = y
    when((this as DrawingContext2DImpl).textAlign) {
        TextAlign.start, TextAlign.left -> {}
        TextAlign.end, TextAlign.right -> {
            dx -= sizeTaken
        }
        TextAlign.center -> {
            dx -= sizeTaken / 2
        }
    }
    dy -= height
    (text as NSString).drawAtPoint(
        CGPointMake(dx, dy),
        withAttributes = attrs
    )
}


public actual fun DrawingContext2D.drawOutlinedText(text: String, x: Double, y: Double): Unit {
    val attrs = mapOf<Any?, Any?>(
        NSFontAttributeName to (this as DrawingContext2DImpl).font,
        NSStrokeColorAttributeName to this.stroke.closestColor().toUiColor(),
        NSStrokeWidthAttributeName to lineWidth as NSNumber,
        NSParagraphStyleAttributeName to NSMutableParagraphStyle().apply {
            setAlignment(
                when ((this@drawOutlinedText as DrawingContext2DImpl).textAlign) {
                    TextAlign.start -> NSTextAlignmentLeft
                    TextAlign.end -> NSTextAlignmentRight
                    TextAlign.left -> NSTextAlignmentLeft
                    TextAlign.right -> NSTextAlignmentRight
                    TextAlign.center -> NSTextAlignmentCenter
                }
            )
        }
    )
    val ns = (text as NSString)
    val sizeTaken = ns.sizeWithAttributes(attrs).useContents { width }
    val height = font.lineHeight
    var dx = x
    var dy = y
    when((this as DrawingContext2DImpl).textAlign) {
        TextAlign.start, TextAlign.left -> {}
        TextAlign.end, TextAlign.right -> {
            dx -= sizeTaken
        }
        TextAlign.center -> {
            dx -= sizeTaken / 2
        }
    }
    dy -= height
    (text as NSString).drawAtPoint(
        CGPointMake(dx, dy),
        withAttributes = attrs
    )
}

public actual fun DrawingContext2D.font(size: Double, value: FontAndStyle): Unit {
    (this as DrawingContext2DImpl).font =
        value.font.get(size, value.weight.toUIFontWeight(), value.italic)
}

public actual fun DrawingContext2D.textAlign(alignment: TextAlign): Unit {
    (this as DrawingContext2DImpl).textAlign = alignment
}


public actual fun DrawingContext2D.fill(): Unit = CGContextFillPath((this as DrawingContext2DImpl).wraps)

public actual fun DrawingContext2D.fillEvenOdd(): Unit = CGContextEOFillPath((this as DrawingContext2DImpl).wraps)

public actual var DrawingContext2D.strokePaint: Paint
    get() = (this as DrawingContext2DImpl).stroke
    set(value) {
        (this as DrawingContext2DImpl).stroke = value
        val c = value.closestColor()
        CGContextSetRGBStrokeColor(
            (this as DrawingContext2DImpl).wraps,
            c.red.toDouble(),
            c.green.toDouble(),
            c.blue.toDouble(),
            c.alpha.toDouble()
        )
    }

public actual var DrawingContext2D.fillPaint: Paint
    get() = (this as DrawingContext2DImpl).fill
    set(value) {
        (this as DrawingContext2DImpl).fill = value
        val c = value.closestColor()
        CGContextSetRGBFillColor(
            (this as DrawingContext2DImpl).wraps,
            c.red.toDouble(),
            c.green.toDouble(),
            c.blue.toDouble(),
            c.alpha.toDouble()
        )
    }

public actual val DrawingContext2D.width: Double get() = (this as DrawingContext2DImpl).width

public actual val DrawingContext2D.height: Double get() = (this as DrawingContext2DImpl).height


public actual fun DrawingContext2D.clear() {
    (this as DrawingContext2DImpl).wraps.let {
        CGContextClearRect(it, CGRectMake(0.0, 0.0, width, height))
    }
}