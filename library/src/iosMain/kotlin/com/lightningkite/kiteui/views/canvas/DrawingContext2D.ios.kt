package com.lightningkite.kiteui.views.canvas

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.direct.addLine
import com.lightningkite.kiteui.views.direct.arcTo
import com.lightningkite.kiteui.views.toUIFontWeight
import com.lightningkite.kiteui.views.toUiColor
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.Foundation.NSAttributedStringKey
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.UIKit.*
import kotlin.math.*

@OptIn(ExperimentalForeignApi::class)
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
//    actual abstract fun arcTo(
//        x1: Double,
//        y1: Double,
//        x2: Double,
//        y2: Double,
//        radiusX: Double,
//        radiusY: Double,
//        rotation: Double
//    )

    actual abstract fun rect(x: Double, y: Double, w: Double, h: Double)
//    actual abstract fun arc(x: Double, y: Double, radius: Double, startAngle: Double, endAngle: Double, anticlockwise: Boolean)
//    actual abstract fun ellipse(x: Double, y: Double, radiusX: Double, radiusY: Double, rotation: Double, startAngle: Double, endAngle: Double, anticlockwise: Boolean)
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

    private var _globalAlpha: Double = 1.0
    override var globalAlpha: Double
        get() = _globalAlpha
        set(value) {
            _globalAlpha = value.coerceIn(0.0, 1.0)
            CGContextSetAlpha(wraps, _globalAlpha)
        }
    override var globalCompositeOperation: String
        get() = "source-over"
        set(value) {
            val mode = when(value) {
                "source-over" -> CGBlendMode.kCGBlendModeNormal
                "source-in" -> CGBlendMode.kCGBlendModeSourceIn
                "source-out" -> CGBlendMode.kCGBlendModeSourceOut
                "source-atop" -> CGBlendMode.kCGBlendModeSourceAtop
                "destination-over" -> CGBlendMode.kCGBlendModeDestinationOver
                "destination-in" -> CGBlendMode.kCGBlendModeDestinationIn
                "destination-out" -> CGBlendMode.kCGBlendModeDestinationOut
                "destination-atop" -> CGBlendMode.kCGBlendModeDestinationAtop
                "xor" -> CGBlendMode.kCGBlendModeXOR
                "lighter" -> CGBlendMode.kCGBlendModePlusLighter
                "copy" -> CGBlendMode.kCGBlendModeCopy
                "clear" -> CGBlendMode.kCGBlendModeClear
                else -> CGBlendMode.kCGBlendModeNormal
            }
            CGContextSetBlendMode(wraps, mode)
        }
    override var imageSmoothingEnabled: Boolean
        get() = true
        set(value) {
            CGContextSetShouldAntialias(wraps, value)
            CGContextSetAllowsAntialiasing(wraps, value)
        }

    override fun clearRect(x: Double, y: Double, w: Double, h: Double) =
        CGContextClearRect(wraps, CGRectMake(x, y, w, h))

    override fun fillRect(x: Double, y: Double, w: Double, h: Double) {
        when (val paint = fill) {
            is LinearGradient -> {
                CGContextSaveGState(wraps)
                CGContextClipToRect(wraps, CGRectMake(x, y, w, h))
                fillWithGradient(paint)
                CGContextRestoreGState(wraps)
            }
            is RadialGradient -> {
                CGContextSaveGState(wraps)
                CGContextClipToRect(wraps, CGRectMake(x, y, w, h))
                fillWithGradient(paint)
                CGContextRestoreGState(wraps)
            }
            else -> CGContextFillRect(wraps, CGRectMake(x, y, w, h))
        }
    }
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
    private var _miterLimit: Double = 10.0
    override var miterLimit: Double
        get() = _miterLimit
        set(value) {
            _miterLimit = value
            CGContextSetMiterLimit(wraps, value)
        }
    private var _lineDashOffset: Double = 0.0
    internal var _lineDashSegments: DoubleArray = doubleArrayOf()
    override var lineDashOffset: Double
        get() = _lineDashOffset
        set(value) {
            _lineDashOffset = value
            applyLineDash()
        }

    override fun setLineDash(segments: Array<Double>) {
        _lineDashSegments = segments.toDoubleArray()
        applyLineDash()
    }

    override fun getLineDash(): Array<Double> = _lineDashSegments.toTypedArray()

    @OptIn(ExperimentalForeignApi::class)
    internal fun applyLineDash() {
        if (_lineDashSegments.isEmpty()) {
            CGContextSetLineDash(wraps, 0.0, null, 0u)
        } else {
            // CoreGraphics expects CGFloat array
            val cgFloats = _lineDashSegments.map { it }.toDoubleArray()
            cgFloats.usePinned { pinned ->
                CGContextSetLineDash(wraps, _lineDashOffset, pinned.addressOf(0), _lineDashSegments.size.toULong())
            }
        }
    }
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
    internal var _lineCap: CGLineCap = CGLineCap.kCGLineCapButt
    internal var _lineJoin: CGLineJoin = CGLineJoin.kCGLineJoinMiter

    // Shadow properties
    internal var _shadowBlur: Double = 0.0
    internal var _shadowColor: Color = Color.transparent
    internal var _shadowOffsetX: Double = 0.0
    internal var _shadowOffsetY: Double = 0.0

    internal fun applyShadow() {
        if (_shadowColor.alpha > 0f && (_shadowBlur > 0 || _shadowOffsetX != 0.0 || _shadowOffsetY != 0.0)) {
            CGContextSetShadowWithColor(
                wraps,
                CGSizeMake(_shadowOffsetX, _shadowOffsetY),
                _shadowBlur,
                _shadowColor.toUiColor().CGColor
            )
        } else {
            // Disable shadow by setting transparent color
            CGContextSetShadowWithColor(wraps, CGSizeMake(0.0, 0.0), 0.0, null)
        }
    }

    // Gradient helpers - CoreGraphics requires gradients to be drawn differently
    internal fun createCGGradient(stops: List<GradientStop>): CGGradientRef? {
        if (stops.isEmpty()) return null

        val colorSpace = CGColorSpaceCreateDeviceRGB()

        // Create components array: [r0, g0, b0, a0, r1, g1, b1, a1, ...]
        val components = DoubleArray(stops.size * 4)
        val locations = DoubleArray(stops.size)

        stops.forEachIndexed { index, stop ->
            val c = stop.color
            components[index * 4 + 0] = c.red.toDouble()
            components[index * 4 + 1] = c.green.toDouble()
            components[index * 4 + 2] = c.blue.toDouble()
            components[index * 4 + 3] = c.alpha.toDouble()
            locations[index] = stop.ratio.toDouble()
        }

        return components.usePinned { pinnedComponents ->
            locations.usePinned { pinnedLocations ->
                CGGradientCreateWithColorComponents(
                    colorSpace,
                    pinnedComponents.addressOf(0),
                    pinnedLocations.addressOf(0),
                    stops.size.toULong()
                )
            }
        }
    }

    internal fun fillWithGradient(gradient: LinearGradient) {
        val cgGradient = createCGGradient(gradient.stops) ?: return

        val x0 = gradient.x0 ?: 0.0
        val y0 = gradient.y0 ?: 0.0
        val x1 = gradient.x1 ?: width
        val y1 = gradient.y1 ?: 0.0

        CGContextDrawLinearGradient(
            wraps,
            cgGradient,
            CGPointMake(x0, y0),
            CGPointMake(x1, y1),
            kCGGradientDrawsBeforeStartLocation or kCGGradientDrawsAfterEndLocation
        )
    }

    internal fun fillWithGradient(gradient: RadialGradient) {
        val cgGradient = createCGGradient(gradient.stops) ?: return

        val cx = gradient.cx ?: (width / 2)
        val cy = gradient.cy ?: (height / 2)
        val r = gradient.radius ?: (minOf(width, height) / 2)
        val fx = gradient.fx ?: cx
        val fy = gradient.fy ?: cy

        CGContextDrawRadialGradient(
            wraps,
            cgGradient,
            CGPointMake(fx, fy),
            0.0,
            CGPointMake(cx, cy),
            r,
            kCGGradientDrawsBeforeStartLocation or kCGGradientDrawsAfterEndLocation
        )
    }
}


actual fun DrawingContext2D.appendArc(
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


actual fun DrawingContext2D.drawText(text: String, x: Double, y: Double): Unit {
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


actual fun DrawingContext2D.drawOutlinedText(text: String, x: Double, y: Double): Unit {
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

actual fun DrawingContext2D.font(size: Double, value: FontAndStyle): Unit {
    (this as DrawingContext2DImpl).font =
        value.font.get(size, value.weight.toUIFontWeight(), value.italic)
}

actual fun DrawingContext2D.textAlign(alignment: TextAlign): Unit {
    (this as DrawingContext2DImpl).textAlign = alignment
}


actual fun DrawingContext2D.fill(): Unit {
    val impl = this as DrawingContext2DImpl
    when (val paint = impl.fill) {
        is LinearGradient -> {
            CGContextSaveGState(impl.wraps)
            CGContextClip(impl.wraps)
            impl.fillWithGradient(paint)
            CGContextRestoreGState(impl.wraps)
        }
        is RadialGradient -> {
            CGContextSaveGState(impl.wraps)
            CGContextClip(impl.wraps)
            impl.fillWithGradient(paint)
            CGContextRestoreGState(impl.wraps)
        }
        else -> CGContextFillPath(impl.wraps)
    }
}

actual fun DrawingContext2D.fillEvenOdd(): Unit {
    val impl = this as DrawingContext2DImpl
    when (val paint = impl.fill) {
        is LinearGradient -> {
            CGContextSaveGState(impl.wraps)
            CGContextEOClip(impl.wraps)
            impl.fillWithGradient(paint)
            CGContextRestoreGState(impl.wraps)
        }
        is RadialGradient -> {
            CGContextSaveGState(impl.wraps)
            CGContextEOClip(impl.wraps)
            impl.fillWithGradient(paint)
            CGContextRestoreGState(impl.wraps)
        }
        else -> CGContextEOFillPath(impl.wraps)
    }
}

actual var DrawingContext2D.strokePaint: Paint
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

actual var DrawingContext2D.fillPaint: Paint
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

actual val DrawingContext2D.width: Double get() = (this as DrawingContext2DImpl).width

actual val DrawingContext2D.height: Double get() = (this as DrawingContext2DImpl).height


actual fun DrawingContext2D.clear() {
    (this as DrawingContext2DImpl).wraps.let {
        CGContextClearRect(it, CGRectMake(0.0, 0.0, width, height))
    }
}

actual var DrawingContext2D.lineCapStyle: LineCap
    get() = (this as DrawingContext2DImpl).let { impl ->
        when (impl._lineCap) {
            CGLineCap.kCGLineCapButt -> LineCap.butt
            CGLineCap.kCGLineCapRound -> LineCap.round
            CGLineCap.kCGLineCapSquare -> LineCap.square
            else -> LineCap.butt
        }
    }
    set(value) {
        (this as DrawingContext2DImpl).let { impl ->
            impl._lineCap = when (value) {
                LineCap.butt -> CGLineCap.kCGLineCapButt
                LineCap.round -> CGLineCap.kCGLineCapRound
                LineCap.square -> CGLineCap.kCGLineCapSquare
            }
            CGContextSetLineCap(impl.wraps, impl._lineCap)
        }
    }

actual var DrawingContext2D.lineJoinStyle: LineJoin
    get() = (this as DrawingContext2DImpl).let { impl ->
        when (impl._lineJoin) {
            CGLineJoin.kCGLineJoinMiter -> LineJoin.miter
            CGLineJoin.kCGLineJoinRound -> LineJoin.round
            CGLineJoin.kCGLineJoinBevel -> LineJoin.bevel
            else -> LineJoin.miter
        }
    }
    set(value) {
        (this as DrawingContext2DImpl).let { impl ->
            impl._lineJoin = when (value) {
                LineJoin.miter -> CGLineJoin.kCGLineJoinMiter
                LineJoin.round -> CGLineJoin.kCGLineJoinRound
                LineJoin.bevel -> CGLineJoin.kCGLineJoinBevel
            }
            CGContextSetLineJoin(impl.wraps, impl._lineJoin)
        }
    }

// ============================================================================
// Clipping
// ============================================================================

actual fun DrawingContext2D.clip() {
    val impl = this as DrawingContext2DImpl
    CGContextClip(impl.wraps)
}

actual fun DrawingContext2D.clip(fillRule: FillRule) {
    val impl = this as DrawingContext2DImpl
    when (fillRule) {
        FillRule.nonzero -> CGContextClip(impl.wraps)
        FillRule.evenodd -> CGContextEOClip(impl.wraps)
    }
}

actual fun DrawingContext2D.resetClip() {
    val impl = this as DrawingContext2DImpl
    // CoreGraphics doesn't have a direct resetClip, so we restore to full canvas
    // This requires that we've saved state before clipping. As a fallback,
    // we clip to the full canvas bounds
    CGContextClipToRect(impl.wraps, CGRectMake(0.0, 0.0, impl.width, impl.height))
}

// ============================================================================
// Line Dash
// ============================================================================

actual fun DrawingContext2D.setLineDash(segments: List<Double>) {
    val impl = this as DrawingContext2DImpl
    impl._lineDashSegments = segments.toDoubleArray()
    impl.applyLineDash()
}

actual fun DrawingContext2D.getLineDash(): List<Double> {
    val impl = this as DrawingContext2DImpl
    return impl._lineDashSegments.toList()
}

// ============================================================================
// Text Metrics
// ============================================================================

actual fun DrawingContext2D.measureText(text: String): TextMetrics {
    val impl = this as DrawingContext2DImpl
    val attrs = mapOf<Any?, Any?>(
        NSFontAttributeName to impl.font
    )
    val ns = text as NSString
    val size = ns.sizeWithAttributes(attrs)
    val width = size.useContents { this.width }
    val height = size.useContents { this.height }
    val ascent = impl.font.ascender
    val descent = impl.font.descender

    return TextMetrics(
        width = width,
        height = height,
        ascent = ascent,
        descent = -descent // descender is negative in iOS
    )
}

// ============================================================================
// Shapes
// ============================================================================

actual fun DrawingContext2D.roundRect(x: Double, y: Double, width: Double, height: Double, radius: Double) {
    val impl = this as DrawingContext2DImpl
    val path = CGPathCreateWithRoundedRect(
        CGRectMake(x, y, width, height),
        radius,
        radius,
        null
    )
    CGContextAddPath(impl.wraps, path)
}

actual fun DrawingContext2D.roundRect(
    x: Double, y: Double, width: Double, height: Double,
    topLeftRadius: Double, topRightRadius: Double,
    bottomRightRadius: Double, bottomLeftRadius: Double
) {
    val impl = this as DrawingContext2DImpl

    // Build path manually with arcs for each corner
    val right = x + width
    val bottom = y + height

    // Start at top-left after the corner
    CGContextMoveToPoint(impl.wraps, x + topLeftRadius, y)

    // Top edge and top-right corner
    CGContextAddLineToPoint(impl.wraps, right - topRightRadius, y)
    if (topRightRadius > 0) {
        CGContextAddArcToPoint(impl.wraps, right, y, right, y + topRightRadius, topRightRadius)
    }

    // Right edge and bottom-right corner
    CGContextAddLineToPoint(impl.wraps, right, bottom - bottomRightRadius)
    if (bottomRightRadius > 0) {
        CGContextAddArcToPoint(impl.wraps, right, bottom, right - bottomRightRadius, bottom, bottomRightRadius)
    }

    // Bottom edge and bottom-left corner
    CGContextAddLineToPoint(impl.wraps, x + bottomLeftRadius, bottom)
    if (bottomLeftRadius > 0) {
        CGContextAddArcToPoint(impl.wraps, x, bottom, x, bottom - bottomLeftRadius, bottomLeftRadius)
    }

    // Left edge and top-left corner
    CGContextAddLineToPoint(impl.wraps, x, y + topLeftRadius)
    if (topLeftRadius > 0) {
        CGContextAddArcToPoint(impl.wraps, x, y, x + topLeftRadius, y, topLeftRadius)
    }

    CGContextClosePath(impl.wraps)
}

actual fun DrawingContext2D.ellipse(
    x: Double, y: Double,
    radiusX: Double, radiusY: Double,
    rotation: Double,
    startAngle: Double, endAngle: Double,
    anticlockwise: Boolean
) {
    val impl = this as DrawingContext2DImpl

    // Save current transform
    CGContextSaveGState(impl.wraps)

    // Translate to center, rotate, then scale for ellipse
    CGContextTranslateCTM(impl.wraps, x, y)
    CGContextRotateCTM(impl.wraps, rotation)
    CGContextScaleCTM(impl.wraps, radiusX, radiusY)

    // Draw arc as unit circle (will be transformed to ellipse)
    CGContextAddArc(
        impl.wraps,
        0.0, 0.0,  // center at origin
        1.0,       // unit radius
        startAngle,
        endAngle,
        if (anticlockwise) 1 else 0
    )

    // Restore transform
    CGContextRestoreGState(impl.wraps)
}

// ============================================================================
// Hit Testing
// ============================================================================

actual fun DrawingContext2D.isPointInPath(x: Double, y: Double): Boolean {
    return isPointInPath(x, y, FillRule.nonzero)
}

actual fun DrawingContext2D.isPointInPath(x: Double, y: Double, fillRule: FillRule): Boolean {
    val impl = this as DrawingContext2DImpl
    val path = CGContextCopyPath(impl.wraps) ?: return false
    // CGPathContainsPoint's last parameter is eoFill: true for even-odd, false for non-zero winding
    val useEvenOdd = fillRule == FillRule.evenodd
    return CGPathContainsPoint(path, null, CGPointMake(x, y), useEvenOdd)
}

// ============================================================================
// Shadows
// ============================================================================

actual var DrawingContext2D.shadowBlur: Double
    get() = (this as DrawingContext2DImpl)._shadowBlur
    set(value) {
        val impl = this as DrawingContext2DImpl
        impl._shadowBlur = value
        impl.applyShadow()
    }

actual var DrawingContext2D.shadowColorValue: Color
    get() = (this as DrawingContext2DImpl)._shadowColor
    set(value) {
        val impl = this as DrawingContext2DImpl
        impl._shadowColor = value
        impl.applyShadow()
    }

actual var DrawingContext2D.shadowOffsetX: Double
    get() = (this as DrawingContext2DImpl)._shadowOffsetX
    set(value) {
        val impl = this as DrawingContext2DImpl
        impl._shadowOffsetX = value
        impl.applyShadow()
    }

actual var DrawingContext2D.shadowOffsetY: Double
    get() = (this as DrawingContext2DImpl)._shadowOffsetY
    set(value) {
        val impl = this as DrawingContext2DImpl
        impl._shadowOffsetY = value
        impl.applyShadow()
    }

// ============================================================================
// Transform
// ============================================================================

actual fun DrawingContext2D.getTransform(): TransformMatrix {
    val impl = this as DrawingContext2DImpl
    val ctm = CGContextGetCTM(impl.wraps)
    return ctm.useContents {
        TransformMatrix(
            a = a,
            b = b,
            c = c,
            d = d,
            e = tx,
            f = ty
        )
    }
}

actual fun DrawingContext2D.setTransform(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double) {
    val impl = this as DrawingContext2DImpl
    // Get current transform and compute its inverse to reset
    val current = CGContextGetCTM(impl.wraps)

    // Compute inverse transform components
    val det = current.useContents { this.a * this.d - this.b * this.c }
    if (det == 0.0) return // Cannot invert singular matrix

    val inverse = current.useContents {
        CGAffineTransformMake(
            this.d / det,
            -this.b / det,
            -this.c / det,
            this.a / det,
            (this.c * this.ty - this.d * this.tx) / det,
            (this.b * this.tx - this.a * this.ty) / det
        )
    }

    // Apply inverse to reset to identity
    CGContextConcatCTM(impl.wraps, inverse)
    // Now apply the new transform
    CGContextConcatCTM(impl.wraps, CGAffineTransformMake(a, b, c, d, e, f))
}