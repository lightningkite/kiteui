package com.lightningkite.kiteui.views.canvas

import com.lightningkite.kiteui.models.Angle
import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.FontAndStyle

public enum class TextAlign {
    start, end, left, right, center
}

public enum class LineCap {
    butt, round, square
}

public enum class LineJoin {
    miter, round, bevel
}

public enum class FillRule {
    nonzero, evenodd
}

/**
 * Result of measuring text dimensions.
 */
public data class TextMetrics(
    /** The width of the text in pixels. */
    val width: Double,
    /** The height of the text based on font metrics (ascent + descent). */
    val height: Double = 0.0,
    /** Distance from baseline to top of highest glyph. */
    val ascent: Double = 0.0,
    /** Distance from baseline to bottom of lowest glyph. */
    val descent: Double = 0.0,
)

/**
 * Represents a 2D affine transformation matrix.
 * The matrix is in the form:
 * | a  c  e |
 * | b  d  f |
 * | 0  0  1 |
 */
public data class TransformMatrix(
    val a: Double = 1.0,  // horizontal scaling
    val b: Double = 0.0,  // vertical skewing
    val c: Double = 0.0,  // horizontal skewing
    val d: Double = 1.0,  // vertical scaling
    val e: Double = 0.0,  // horizontal translation
    val f: Double = 0.0,  // vertical translation
) {
    public companion object {
        public val identity: TransformMatrix = TransformMatrix()
    }
}

public expect abstract class DrawingContext2D {
    public abstract fun save()
    public abstract fun restore()
    public abstract fun scale(x: Double, y: Double)
    public abstract fun rotate(angle: Double)
    public abstract fun translate(x: Double, y: Double)
    public abstract fun transform(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double)
//  abstract   fun getTransform(): DOMMatrix
//    abstract fun setTransform(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double)
    public abstract var globalAlpha: Double
    public abstract var globalCompositeOperation: String
    public abstract var imageSmoothingEnabled: Boolean
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
    public abstract fun clearRect(x: Double, y: Double, w: Double, h: Double)
    public abstract fun fillRect(x: Double, y: Double, w: Double, h: Double)
    public abstract fun strokeRect(x: Double, y: Double, w: Double, h: Double)
    public abstract fun beginPath()
//  abstract   fun fill(path: Path2D, fillRule: CanvasFillRule)
    public abstract fun stroke()
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

    public abstract var lineWidth: Double
    public abstract var miterLimit: Double
    public abstract var lineDashOffset: Double
//    abstract fun setLineDash(segments: Array<Double>)
//    abstract fun getLineDash(): Array<Double>
    public abstract fun closePath()
    public abstract fun moveTo(x: Double, y: Double)
    public abstract fun lineTo(x: Double, y: Double)
    public abstract fun quadraticCurveTo(cpx: Double, cpy: Double, x: Double, y: Double)
    public abstract fun bezierCurveTo(cp1x: Double, cp1y: Double, cp2x: Double, cp2y: Double, x: Double, y: Double)
//    abstract fun arcTo(x1: Double, y1: Double, x2: Double, y2: Double, radius: Double)
//    abstract fun arcTo(x1: Double, y1: Double, x2: Double, y2: Double, radiusX: Double, radiusY: Double, rotation: Double)
    public abstract fun rect(x: Double, y: Double, w: Double, h: Double)
//    abstract fun arc(x: Double, y: Double, radius: Double, startAngle: Double, endAngle: Double, anticlockwise: Boolean)
//    abstract fun ellipse(x: Double, y: Double, radiusX: Double, radiusY: Double, rotation: Double, startAngle: Double, endAngle: Double, anticlockwise: Boolean)
}

public expect fun DrawingContext2D.appendArc(x: Double, y: Double, radius: Double, startAngle: Angle, endAngle: Angle, anticlockwise: Boolean)
public expect fun DrawingContext2D.drawOutlinedText(text: String, x: Double, y: Double)
public expect fun DrawingContext2D.drawText(text: String, x: Double, y: Double)
public expect fun DrawingContext2D.font(size: Double, value: FontAndStyle)
public expect fun DrawingContext2D.textAlign(alignment: TextAlign)
public expect fun DrawingContext2D.clear()
public expect fun DrawingContext2D.fill()
public expect fun DrawingContext2D.fillEvenOdd()
public expect var DrawingContext2D.strokePaint: com.lightningkite.kiteui.models.Paint
public expect var DrawingContext2D.fillPaint: com.lightningkite.kiteui.models.Paint
public expect var DrawingContext2D.lineCapStyle: LineCap
public expect var DrawingContext2D.lineJoinStyle: LineJoin
public expect val DrawingContext2D.width: Double
public expect val DrawingContext2D.height: Double

// ============================================================================
// Clipping
// ============================================================================

/** Clips the drawing region to the current path using the non-zero winding rule. */
public expect fun DrawingContext2D.clip()

/** Clips the drawing region to the current path using the specified fill rule. */
public expect fun DrawingContext2D.clip(fillRule: FillRule)

/** Resets the clipping region to the entire canvas. */
public expect fun DrawingContext2D.resetClip()

// ============================================================================
// Line Dash
// ============================================================================

/** Sets the line dash pattern. Empty list means solid line. */
public expect fun DrawingContext2D.setLineDash(segments: List<Double>)

/** Gets the current line dash pattern. */
public expect fun DrawingContext2D.getLineDash(): List<Double>

// ============================================================================
// Text Metrics
// ============================================================================

/** Measures the width and height of the given text with the current font. */
public expect fun DrawingContext2D.measureText(text: String): TextMetrics

// ============================================================================
// Shapes
// ============================================================================

/**
 * Adds a rounded rectangle to the current path.
 * @param x The x-coordinate of the rectangle's starting point.
 * @param y The y-coordinate of the rectangle's starting point.
 * @param width The rectangle's width.
 * @param height The rectangle's height.
 * @param radius The corner radius (applied to all corners).
 */
public expect fun DrawingContext2D.roundRect(x: Double, y: Double, width: Double, height: Double, radius: Double)

/**
 * Adds a rounded rectangle to the current path with different radii for each corner.
 * @param x The x-coordinate of the rectangle's starting point.
 * @param y The y-coordinate of the rectangle's starting point.
 * @param width The rectangle's width.
 * @param height The rectangle's height.
 * @param topLeftRadius Top-left corner radius.
 * @param topRightRadius Top-right corner radius.
 * @param bottomRightRadius Bottom-right corner radius.
 * @param bottomLeftRadius Bottom-left corner radius.
 */
public expect fun DrawingContext2D.roundRect(
    x: Double, y: Double, width: Double, height: Double,
    topLeftRadius: Double, topRightRadius: Double,
    bottomRightRadius: Double, bottomLeftRadius: Double
)

/**
 * Adds an ellipse to the current path.
 * @param x The x-coordinate of the ellipse's center.
 * @param y The y-coordinate of the ellipse's center.
 * @param radiusX The ellipse's major-axis radius.
 * @param radiusY The ellipse's minor-axis radius.
 * @param rotation The rotation of the ellipse in radians.
 * @param startAngle The angle at which the ellipse starts, in radians.
 * @param endAngle The angle at which the ellipse ends, in radians.
 * @param anticlockwise If true, draws the ellipse anticlockwise.
 */
public expect fun DrawingContext2D.ellipse(
    x: Double, y: Double,
    radiusX: Double, radiusY: Double,
    rotation: Double,
    startAngle: Double, endAngle: Double,
    anticlockwise: Boolean = false
)

// ============================================================================
// Hit Testing
// ============================================================================

/** Returns true if the given point is inside the current path. */
public expect fun DrawingContext2D.isPointInPath(x: Double, y: Double): Boolean

/** Returns true if the given point is inside the current path using the specified fill rule. */
public expect fun DrawingContext2D.isPointInPath(x: Double, y: Double, fillRule: FillRule): Boolean

// ============================================================================
// Shadows
// ============================================================================

/** The blur level for shadows. 0 means no blur. */
public expect var DrawingContext2D.shadowBlur: Double

/** The color of the shadow. Use Color.transparent to disable shadows. */
public expect var DrawingContext2D.shadowColorValue: Color

/** The horizontal offset of the shadow. */
public expect var DrawingContext2D.shadowOffsetX: Double

/** The vertical offset of the shadow. */
public expect var DrawingContext2D.shadowOffsetY: Double

// ============================================================================
// Transform
// ============================================================================

/** Gets the current transformation matrix. */
public expect fun DrawingContext2D.getTransform(): TransformMatrix

/** Sets the transformation matrix directly, replacing the current transform. */
public expect fun DrawingContext2D.setTransform(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double)

/** Sets the transformation matrix directly from a TransformMatrix. */
public fun DrawingContext2D.setTransform(matrix: TransformMatrix) {
    setTransform(matrix.a, matrix.b, matrix.c, matrix.d, matrix.e, matrix.f)
}

/** Resets the transformation matrix to the identity matrix. */
// On JS, DrawingContext2D is a typealias for the browser's CanvasRenderingContext2D, which already
// has a native resetTransform() member with identical semantics - this extension is unreachable there.
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public fun DrawingContext2D.resetTransform() {
    setTransform(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
}
