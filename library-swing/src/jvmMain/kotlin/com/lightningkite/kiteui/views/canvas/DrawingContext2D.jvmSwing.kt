package com.lightningkite.kiteui.views.canvas

import com.lightningkite.kiteui.models.Angle
import com.lightningkite.kiteui.models.FontAndStyle
import com.lightningkite.kiteui.models.Paint
import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.LinearGradient
import com.lightningkite.kiteui.models.RadialGradient
import com.lightningkite.kiteui.models.toAwt
import com.lightningkite.kiteui.models.turns
import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.geom.Arc2D
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

actual abstract class DrawingContext2D(val g2d: Graphics2D) {
    val currentPath = Path2D.Double()
    var fillPaintObj: java.awt.Paint = java.awt.Color.BLACK
    var strokePaintObj: java.awt.Paint = java.awt.Color.BLACK
    var currentFont: Font? = null
    var currentTextAlign: TextAlign = TextAlign.left
    var _lineCap: LineCap = LineCap.butt
    var _lineJoin: LineJoin = LineJoin.miter

    // Shadow properties
    var _shadowBlur: Double = 0.0
    var _shadowColor: Color = Color.transparent
    var _shadowOffsetX: Double = 0.0
    var _shadowOffsetY: Double = 0.0

    actual abstract var globalAlpha: Double
    actual abstract fun save()
    actual abstract fun restore()
    actual abstract fun scale(x: Double, y: Double)
    actual abstract fun rotate(angle: Double)
    actual abstract fun translate(x: Double, y: Double)
    actual abstract fun transform(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double)
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
    actual abstract fun closePath()
    actual abstract fun moveTo(x: Double, y: Double)
    actual abstract fun lineTo(x: Double, y: Double)
    actual abstract fun quadraticCurveTo(cpx: Double, cpy: Double, x: Double, y: Double)
    actual abstract fun bezierCurveTo(cp1x: Double, cp1y: Double, cp2x: Double, cp2y: Double, x: Double, y: Double)
    actual abstract fun rect(x: Double, y: Double, w: Double, h: Double)
}

/**
 * Saved state for save/restore operations.
 */
private data class SavedState(
    val transform: AffineTransform,
    val clip: Shape?,
    val globalAlpha: Double,
    val composite: Composite,
    val stroke: BasicStroke,
    val fillPaint: java.awt.Paint,
    val strokePaint: java.awt.Paint,
    val font: Font?,
    val textAlign: TextAlign,
    val lineCap: LineCap,
    val lineJoin: LineJoin,
    val shadowBlur: Double,
    val shadowColor: Color,
    val shadowOffsetX: Double,
    val shadowOffsetY: Double,
)

class DrawingContext2DImpl(g2d: Graphics2D): DrawingContext2D(g2d) {
    private val stateStack = mutableListOf<SavedState>()
    private var _globalAlpha: Double = 1.0
    internal var originalClip: Shape? = g2d.clip

    override var globalAlpha: Double
        get() = _globalAlpha
        set(value) {
            _globalAlpha = value.coerceIn(0.0, 1.0)
            val currentComposite = g2d.composite
            if (currentComposite is AlphaComposite) {
                g2d.composite = currentComposite.derive(_globalAlpha.toFloat())
            } else {
                g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, _globalAlpha.toFloat())
            }
        }

    override fun save() {
        stateStack.add(SavedState(
            transform = g2d.transform.clone() as AffineTransform,
            clip = g2d.clip,
            globalAlpha = _globalAlpha,
            composite = g2d.composite,
            stroke = (g2d.stroke as? BasicStroke) ?: BasicStroke(),
            fillPaint = fillPaintObj,
            strokePaint = strokePaintObj,
            font = currentFont,
            textAlign = currentTextAlign,
            lineCap = _lineCap,
            lineJoin = _lineJoin,
            shadowBlur = _shadowBlur,
            shadowColor = _shadowColor,
            shadowOffsetX = _shadowOffsetX,
            shadowOffsetY = _shadowOffsetY,
        ))
    }

    override fun restore() {
        if (stateStack.isNotEmpty()) {
            val state = stateStack.removeLast()
            g2d.transform = state.transform
            g2d.clip = state.clip
            _globalAlpha = state.globalAlpha
            g2d.composite = state.composite
            g2d.stroke = state.stroke
            fillPaintObj = state.fillPaint
            strokePaintObj = state.strokePaint
            currentFont = state.font
            currentTextAlign = state.textAlign
            _lineCap = state.lineCap
            _lineJoin = state.lineJoin
            _shadowBlur = state.shadowBlur
            _shadowColor = state.shadowColor
            _shadowOffsetX = state.shadowOffsetX
            _shadowOffsetY = state.shadowOffsetY
        }
    }

    override fun scale(x: Double, y: Double) {
        g2d.scale(x, y)
    }

    override fun rotate(angle: Double) {
        g2d.rotate(angle)
    }

    override fun translate(x: Double, y: Double) {
        g2d.translate(x, y)
    }

    override fun transform(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double) {
        g2d.transform(AffineTransform(a, b, c, d, e, f))
    }

    override var globalCompositeOperation: String
        get() = "source-over"
        set(value) {
            // Map composite operation strings to AlphaComposite modes
            val composite = when(value) {
                "source-over" -> AlphaComposite.SrcOver
                "source-in" -> AlphaComposite.SrcIn
                "source-out" -> AlphaComposite.SrcOut
                "source-atop" -> AlphaComposite.SrcAtop
                "destination-over" -> AlphaComposite.DstOver
                "destination-in" -> AlphaComposite.DstIn
                "destination-out" -> AlphaComposite.DstOut
                "destination-atop" -> AlphaComposite.DstAtop
                "lighter" -> AlphaComposite.SrcOver // Approximation
                "copy" -> AlphaComposite.Src
                "xor" -> AlphaComposite.Xor
                else -> AlphaComposite.SrcOver
            }
            g2d.composite = composite
        }

    override var imageSmoothingEnabled: Boolean
        get() = g2d.renderingHints[RenderingHints.KEY_INTERPOLATION] == RenderingHints.VALUE_INTERPOLATION_BILINEAR
        set(value) {
            g2d.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                if (value) RenderingHints.VALUE_INTERPOLATION_BILINEAR else RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
            )
        }

    override fun clearRect(x: Double, y: Double, w: Double, h: Double) {
        val oldComposite = g2d.composite
        g2d.composite = AlphaComposite.Clear
        g2d.fillRect(x.toInt(), y.toInt(), w.toInt(), h.toInt())
        g2d.composite = oldComposite
    }

    override fun fillRect(x: Double, y: Double, w: Double, h: Double) {
        g2d.paint = fillPaintObj
        g2d.fill(Rectangle2D.Double(x, y, w, h))
    }

    override fun strokeRect(x: Double, y: Double, w: Double, h: Double) {
        g2d.paint = strokePaintObj
        g2d.draw(Rectangle2D.Double(x, y, w, h))
    }

    override fun beginPath() {
        currentPath.reset()
    }

    override fun stroke() {
        g2d.paint = strokePaintObj
        g2d.draw(currentPath)
    }

    override var lineWidth: Double
        get() = (g2d.stroke as? BasicStroke)?.lineWidth?.toDouble() ?: 1.0
        set(value) {
            val currentStroke = g2d.stroke as? BasicStroke
            g2d.stroke = BasicStroke(
                value.toFloat(),
                currentStroke?.endCap ?: BasicStroke.CAP_BUTT,
                currentStroke?.lineJoin ?: BasicStroke.JOIN_MITER,
                currentStroke?.miterLimit ?: 10f,
                currentStroke?.dashArray,
                currentStroke?.dashPhase ?: 0f
            )
        }

    override var miterLimit: Double
        get() = (g2d.stroke as? BasicStroke)?.miterLimit?.toDouble() ?: 10.0
        set(value) {
            val currentStroke = g2d.stroke as? BasicStroke
            g2d.stroke = BasicStroke(
                currentStroke?.lineWidth ?: 1f,
                currentStroke?.endCap ?: BasicStroke.CAP_BUTT,
                currentStroke?.lineJoin ?: BasicStroke.JOIN_MITER,
                value.toFloat(),
                currentStroke?.dashArray,
                currentStroke?.dashPhase ?: 0f
            )
        }

    override var lineDashOffset: Double
        get() = (g2d.stroke as? BasicStroke)?.dashPhase?.toDouble() ?: 0.0
        set(value) {
            val currentStroke = g2d.stroke as? BasicStroke
            g2d.stroke = BasicStroke(
                currentStroke?.lineWidth ?: 1f,
                currentStroke?.endCap ?: BasicStroke.CAP_BUTT,
                currentStroke?.lineJoin ?: BasicStroke.JOIN_MITER,
                currentStroke?.miterLimit ?: 10f,
                currentStroke?.dashArray,
                value.toFloat()
            )
        }

    override fun closePath() {
        currentPath.closePath()
    }

    override fun moveTo(x: Double, y: Double) {
        currentPath.moveTo(x, y)
    }

    override fun lineTo(x: Double, y: Double) {
        currentPath.lineTo(x, y)
    }

    override fun quadraticCurveTo(cpx: Double, cpy: Double, x: Double, y: Double) {
        currentPath.quadTo(cpx, cpy, x, y)
    }

    override fun bezierCurveTo(cp1x: Double, cp1y: Double, cp2x: Double, cp2y: Double, x: Double, y: Double) {
        currentPath.curveTo(cp1x, cp1y, cp2x, cp2y, x, y)
    }

    override fun rect(x: Double, y: Double, w: Double, h: Double) {
        currentPath.append(Rectangle2D.Double(x, y, w, h), false)
    }

    fun setLineDash(segments: DoubleArray) {
        if (segments.isEmpty()) {
            val currentStroke = g2d.stroke as? BasicStroke
            g2d.stroke = BasicStroke(
                currentStroke?.lineWidth ?: 1f,
                currentStroke?.endCap ?: BasicStroke.CAP_BUTT,
                currentStroke?.lineJoin ?: BasicStroke.JOIN_MITER,
                currentStroke?.miterLimit ?: 10f,
                null,
                0f
            )
        } else {
            val currentStroke = g2d.stroke as? BasicStroke
            g2d.stroke = BasicStroke(
                currentStroke?.lineWidth ?: 1f,
                currentStroke?.endCap ?: BasicStroke.CAP_BUTT,
                currentStroke?.lineJoin ?: BasicStroke.JOIN_MITER,
                currentStroke?.miterLimit ?: 10f,
                segments.map { it.toFloat() }.toFloatArray(),
                currentStroke?.dashPhase ?: 0f
            )
        }
    }
}

actual fun DrawingContext2D.appendArc(x: Double, y: Double, radius: Double, startAngle: Angle, endAngle: Angle, anticlockwise: Boolean) {
    val rel = startAngle angleTo endAngle
    val startDegrees = startAngle.degrees.toDouble()
    val extentDegrees = if (anticlockwise) {
        if (rel.degrees > 0) {
            (rel - 1.turns).degrees.toDouble()
        } else {
            rel.degrees.toDouble()
        }
    } else {
        if (rel.degrees > 0) {
            rel.degrees.toDouble()
        } else {
            (rel + 1.turns).degrees.toDouble()
        }
    }

    currentPath.append(
        java.awt.geom.Arc2D.Double(
            x - radius,
            y - radius,
            radius * 2,
            radius * 2,
            startDegrees,
            extentDegrees,
            java.awt.geom.Arc2D.OPEN
        ),
        true
    )
}

actual fun DrawingContext2D.drawOutlinedText(text: String, x: Double, y: Double) {
    val font = currentFont ?: g2d.font
    g2d.font = font
    g2d.paint = strokePaintObj

    val adjustedX = when (currentTextAlign) {
        TextAlign.left, TextAlign.start -> x
        TextAlign.center -> {
            val metrics = g2d.fontMetrics
            x - metrics.stringWidth(text) / 2.0
        }
        TextAlign.right, TextAlign.end -> {
            val metrics = g2d.fontMetrics
            x - metrics.stringWidth(text)
        }
    }

    g2d.drawString(text, adjustedX.toFloat(), y.toFloat())
}

actual fun DrawingContext2D.drawText(text: String, x: Double, y: Double) {
    val font = currentFont ?: g2d.font
    g2d.font = font
    g2d.paint = fillPaintObj

    val adjustedX = when (currentTextAlign) {
        TextAlign.left, TextAlign.start -> x
        TextAlign.center -> {
            val metrics = g2d.fontMetrics
            x - metrics.stringWidth(text) / 2.0
        }
        TextAlign.right, TextAlign.end -> {
            val metrics = g2d.fontMetrics
            x - metrics.stringWidth(text)
        }
    }

    g2d.drawString(text, adjustedX.toFloat(), y.toFloat())
}

actual fun DrawingContext2D.font(size: Double, value: FontAndStyle) {
    // Create or derive font from FontAndStyle
    val style = when {
        value.bold && value.italic -> Font.BOLD or Font.ITALIC
        value.bold -> Font.BOLD
        value.italic -> Font.ITALIC
        else -> Font.PLAIN
    }
    currentFont = Font(value.font.name, style, size.toInt())
}

actual fun DrawingContext2D.textAlign(alignment: TextAlign) {
    currentTextAlign = alignment
}

actual fun DrawingContext2D.clear() {
    val oldComposite = g2d.composite
    g2d.composite = AlphaComposite.Clear
    g2d.fillRect(0, 0, width.toInt(), height.toInt())
    g2d.composite = oldComposite
}

actual fun DrawingContext2D.fill() {
    g2d.paint = fillPaintObj
    currentPath.windingRule = Path2D.WIND_NON_ZERO
    g2d.fill(currentPath)
}

actual fun DrawingContext2D.fillEvenOdd() {
    g2d.paint = fillPaintObj
    currentPath.windingRule = Path2D.WIND_EVEN_ODD
    g2d.fill(currentPath)
}

actual var DrawingContext2D.strokePaint: Paint
    get() = Color.black
    set(value) {
        strokePaintObj = value.toAwtPaint(this)
    }

actual var DrawingContext2D.fillPaint: Paint
    get() = Color.black
    set(value) {
        fillPaintObj = value.toAwtPaint(this)
    }

/**
 * Converts a KiteUI Paint to a java.awt.Paint.
 * Supports Color, LinearGradient, and RadialGradient.
 */
private fun Paint.toAwtPaint(context: DrawingContext2D): java.awt.Paint {
    return when (this) {
        is Color -> this.toAwt()
        is LinearGradient -> this.toAwtGradient(context)
        is RadialGradient -> this.toAwtGradient(context)
        else -> this.closestColor().toAwt()
    }
}

/**
 * Converts a KiteUI LinearGradient to a java.awt.LinearGradientPaint.
 */
private fun LinearGradient.toAwtGradient(context: DrawingContext2D): java.awt.Paint {
    if (stops.isEmpty()) return java.awt.Color.BLACK
    if (stops.size == 1) return stops[0].color.toAwt()

    // Determine start and end points
    val startX: Float
    val startY: Float
    val endX: Float
    val endY: Float

    if (x0 != null && y0 != null && x1 != null && y1 != null) {
        // Use explicit coordinates
        startX = x0.toFloat()
        startY = y0.toFloat()
        endX = x1.toFloat()
        endY = y1.toFloat()
    } else {
        // Calculate from angle - use the context bounds
        val w = context.width.toFloat()
        val h = context.height.toFloat()
        val angleRad = angle.radians.toDouble()

        // For a linear gradient, we calculate the points along the gradient line
        // that would cover the entire rectangle
        val cos = cos(angleRad).toFloat()
        val sin = sin(angleRad).toFloat()

        // Find the gradient line through the center
        val cx = w / 2f
        val cy = h / 2f

        // Calculate the half-length of the gradient line that covers the rectangle
        val halfLength = (kotlin.math.abs(cos) * w + kotlin.math.abs(sin) * h) / 2f

        startX = cx - cos * halfLength
        startY = cy - sin * halfLength
        endX = cx + cos * halfLength
        endY = cy + sin * halfLength
    }

    // Avoid zero-length gradient
    if (startX == endX && startY == endY) {
        return stops.last().color.toAwt()
    }

    val fractions = stops.map { it.ratio }.toFloatArray()
    val colors = stops.map { it.color.toAwt() }.toTypedArray()

    return java.awt.LinearGradientPaint(
        startX, startY,
        endX, endY,
        fractions,
        colors
    )
}

/**
 * Converts a KiteUI RadialGradient to a java.awt.RadialGradientPaint.
 */
private fun RadialGradient.toAwtGradient(context: DrawingContext2D): java.awt.Paint {
    if (stops.isEmpty()) return java.awt.Color.BLACK
    if (stops.size == 1) return stops[0].color.toAwt()

    // Determine center and radius
    val centerX: Float
    val centerY: Float
    val radiusVal: Float

    if (cx != null && cy != null && radius != null) {
        centerX = cx.toFloat()
        centerY = cy.toFloat()
        radiusVal = radius.toFloat()
    } else {
        // Default to center of the context with radius as half the smaller dimension
        val w = context.width.toFloat()
        val h = context.height.toFloat()
        centerX = w / 2f
        centerY = h / 2f
        radiusVal = kotlin.math.min(w, h) / 2f
    }

    // Avoid zero radius
    if (radiusVal <= 0f) {
        return stops.last().color.toAwt()
    }

    // Focal point (defaults to center)
    val focalX = fx?.toFloat() ?: centerX
    val focalY = fy?.toFloat() ?: centerY

    val fractions = stops.map { it.ratio }.toFloatArray()
    val colors = stops.map { it.color.toAwt() }.toTypedArray()

    return java.awt.RadialGradientPaint(
        centerX, centerY,
        radiusVal,
        focalX, focalY,
        fractions,
        colors,
        MultipleGradientPaint.CycleMethod.NO_CYCLE
    )
}

actual val DrawingContext2D.width: Double
    get() = g2d.clipBounds?.width?.toDouble() ?: 0.0

actual val DrawingContext2D.height: Double
    get() = g2d.clipBounds?.height?.toDouble() ?: 0.0

actual var DrawingContext2D.lineCapStyle: LineCap
    get() = _lineCap
    set(value) {
        _lineCap = value
        updateStroke()
    }

actual var DrawingContext2D.lineJoinStyle: LineJoin
    get() = _lineJoin
    set(value) {
        _lineJoin = value
        updateStroke()
    }

private fun DrawingContext2D.updateStroke() {
    val currentStroke = g2d.stroke as? BasicStroke
    val cap = when (_lineCap) {
        LineCap.butt -> BasicStroke.CAP_BUTT
        LineCap.round -> BasicStroke.CAP_ROUND
        LineCap.square -> BasicStroke.CAP_SQUARE
    }
    val join = when (_lineJoin) {
        LineJoin.miter -> BasicStroke.JOIN_MITER
        LineJoin.round -> BasicStroke.JOIN_ROUND
        LineJoin.bevel -> BasicStroke.JOIN_BEVEL
    }
    g2d.stroke = BasicStroke(
        currentStroke?.lineWidth ?: 1f,
        cap,
        join,
        currentStroke?.miterLimit ?: 10f,
        currentStroke?.dashArray,
        currentStroke?.dashPhase ?: 0f
    )
}

// ============================================================================
// Clipping
// ============================================================================

/** Clips the drawing region to the current path using the non-zero winding rule. */
actual fun DrawingContext2D.clip() {
    currentPath.windingRule = Path2D.WIND_NON_ZERO
    g2d.clip(currentPath)
}

/** Clips the drawing region to the current path using the specified fill rule. */
actual fun DrawingContext2D.clip(fillRule: FillRule) {
    currentPath.windingRule = when (fillRule) {
        FillRule.nonzero -> Path2D.WIND_NON_ZERO
        FillRule.evenodd -> Path2D.WIND_EVEN_ODD
    }
    g2d.clip(currentPath)
}

/** Resets the clipping region to the entire canvas. */
actual fun DrawingContext2D.resetClip() {
    (this as? DrawingContext2DImpl)?.let { impl ->
        g2d.clip = impl.originalClip
    }
}

// ============================================================================
// Line Dash
// ============================================================================

/** Sets the line dash pattern. Empty list means solid line. */
actual fun DrawingContext2D.setLineDash(segments: List<Double>) {
    (this as? DrawingContext2DImpl)?.setLineDash(segments.toDoubleArray())
}

/** Gets the current line dash pattern. */
actual fun DrawingContext2D.getLineDash(): List<Double> {
    val stroke = g2d.stroke as? BasicStroke
    return stroke?.dashArray?.map { it.toDouble() } ?: emptyList()
}

// ============================================================================
// Text Metrics
// ============================================================================

/** Measures the width and height of the given text with the current font. */
actual fun DrawingContext2D.measureText(text: String): TextMetrics {
    val font = currentFont ?: g2d.font
    g2d.font = font
    val metrics = g2d.fontMetrics
    return TextMetrics(
        width = metrics.stringWidth(text).toDouble(),
        height = metrics.height.toDouble(),
        ascent = metrics.ascent.toDouble(),
        descent = metrics.descent.toDouble()
    )
}

// ============================================================================
// Shapes
// ============================================================================

/**
 * Adds a rounded rectangle to the current path.
 */
actual fun DrawingContext2D.roundRect(x: Double, y: Double, width: Double, height: Double, radius: Double) {
    val arcSize = radius * 2
    currentPath.append(RoundRectangle2D.Double(x, y, width, height, arcSize, arcSize), false)
}

/**
 * Adds a rounded rectangle to the current path with different radii for each corner.
 * Since Java doesn't support per-corner radii directly, we build the path manually.
 */
actual fun DrawingContext2D.roundRect(
    x: Double, y: Double, width: Double, height: Double,
    topLeftRadius: Double, topRightRadius: Double,
    bottomRightRadius: Double, bottomLeftRadius: Double
) {
    // Clamp radii to half of the smallest dimension
    val maxRadiusH = width / 2
    val maxRadiusV = height / 2
    val tl = topLeftRadius.coerceAtMost(maxRadiusH).coerceAtMost(maxRadiusV)
    val tr = topRightRadius.coerceAtMost(maxRadiusH).coerceAtMost(maxRadiusV)
    val br = bottomRightRadius.coerceAtMost(maxRadiusH).coerceAtMost(maxRadiusV)
    val bl = bottomLeftRadius.coerceAtMost(maxRadiusH).coerceAtMost(maxRadiusV)

    // Build path starting from top-left corner, going clockwise
    currentPath.moveTo(x + tl, y)

    // Top edge
    currentPath.lineTo(x + width - tr, y)

    // Top-right corner
    if (tr > 0) {
        currentPath.append(Arc2D.Double(
            x + width - tr * 2, y, tr * 2, tr * 2,
            90.0, -90.0, Arc2D.OPEN
        ), true)
    }

    // Right edge
    currentPath.lineTo(x + width, y + height - br)

    // Bottom-right corner
    if (br > 0) {
        currentPath.append(Arc2D.Double(
            x + width - br * 2, y + height - br * 2, br * 2, br * 2,
            0.0, -90.0, Arc2D.OPEN
        ), true)
    }

    // Bottom edge
    currentPath.lineTo(x + bl, y + height)

    // Bottom-left corner
    if (bl > 0) {
        currentPath.append(Arc2D.Double(
            x, y + height - bl * 2, bl * 2, bl * 2,
            270.0, -90.0, Arc2D.OPEN
        ), true)
    }

    // Left edge
    currentPath.lineTo(x, y + tl)

    // Top-left corner
    if (tl > 0) {
        currentPath.append(Arc2D.Double(
            x, y, tl * 2, tl * 2,
            180.0, -90.0, Arc2D.OPEN
        ), true)
    }

    currentPath.closePath()
}

/**
 * Adds an ellipse to the current path.
 */
actual fun DrawingContext2D.ellipse(
    x: Double, y: Double,
    radiusX: Double, radiusY: Double,
    rotation: Double,
    startAngle: Double, endAngle: Double,
    anticlockwise: Boolean
) {
    // Create the ellipse arc
    val startDegrees = Math.toDegrees(startAngle)
    val endDegrees = Math.toDegrees(endAngle)

    var extent = endDegrees - startDegrees
    if (anticlockwise) {
        if (extent > 0) extent -= 360.0
    } else {
        if (extent < 0) extent += 360.0
    }

    val arc = Arc2D.Double(
        -radiusX, -radiusY,
        radiusX * 2, radiusY * 2,
        -startDegrees,  // Java uses counter-clockwise, so negate
        -extent,
        Arc2D.OPEN
    )

    // Apply rotation and translation
    val transform = AffineTransform()
    transform.translate(x, y)
    transform.rotate(rotation)

    currentPath.append(transform.createTransformedShape(arc), true)
}

// ============================================================================
// Hit Testing
// ============================================================================

/** Returns true if the given point is inside the current path. */
actual fun DrawingContext2D.isPointInPath(x: Double, y: Double): Boolean {
    currentPath.windingRule = Path2D.WIND_NON_ZERO
    return currentPath.contains(x, y)
}

/** Returns true if the given point is inside the current path using the specified fill rule. */
actual fun DrawingContext2D.isPointInPath(x: Double, y: Double, fillRule: FillRule): Boolean {
    currentPath.windingRule = when (fillRule) {
        FillRule.nonzero -> Path2D.WIND_NON_ZERO
        FillRule.evenodd -> Path2D.WIND_EVEN_ODD
    }
    return currentPath.contains(x, y)
}

// ============================================================================
// Shadows
// Note: Java2D doesn't have built-in shadow support like HTML Canvas.
// These properties are stored but the actual shadow rendering would require
// custom implementation using blur filters or rendering to offscreen buffer.
// ============================================================================

/** The blur level for shadows. 0 means no blur. */
actual var DrawingContext2D.shadowBlur: Double
    get() = _shadowBlur
    set(value) {
        _shadowBlur = value.coerceAtLeast(0.0)
    }

/** The color of the shadow. Use Color.transparent to disable shadows. */
actual var DrawingContext2D.shadowColorValue: Color
    get() = _shadowColor
    set(value) {
        _shadowColor = value
    }

/** The horizontal offset of the shadow. */
actual var DrawingContext2D.shadowOffsetX: Double
    get() = _shadowOffsetX
    set(value) {
        _shadowOffsetX = value
    }

/** The vertical offset of the shadow. */
actual var DrawingContext2D.shadowOffsetY: Double
    get() = _shadowOffsetY
    set(value) {
        _shadowOffsetY = value
    }

// ============================================================================
// Transform
// ============================================================================

/** Gets the current transformation matrix. */
actual fun DrawingContext2D.getTransform(): TransformMatrix {
    val t = g2d.transform
    return TransformMatrix(
        a = t.scaleX,
        b = t.shearY,
        c = t.shearX,
        d = t.scaleY,
        e = t.translateX,
        f = t.translateY
    )
}

/** Sets the transformation matrix directly, replacing the current transform. */
actual fun DrawingContext2D.setTransform(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double) {
    g2d.transform = AffineTransform(a, b, c, d, e, f)
}
