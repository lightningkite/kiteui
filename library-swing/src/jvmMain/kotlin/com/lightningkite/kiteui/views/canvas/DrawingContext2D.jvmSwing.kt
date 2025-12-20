package com.lightningkite.kiteui.views.canvas

import com.lightningkite.kiteui.models.Angle
import com.lightningkite.kiteui.models.FontAndStyle
import com.lightningkite.kiteui.models.Paint
import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.toAwt
import com.lightningkite.kiteui.models.turns
import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import kotlin.math.PI

actual abstract class DrawingContext2D(val g2d: Graphics2D) {
    val currentPath = Path2D.Double()
    var fillPaintObj: java.awt.Paint = java.awt.Color.BLACK
    var strokePaintObj: java.awt.Paint = java.awt.Color.BLACK
    var currentFont: Font? = null
    var currentTextAlign: TextAlign = TextAlign.left

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

class DrawingContext2DImpl(g2d: Graphics2D): DrawingContext2D(g2d) {
    private val transformStack = mutableListOf<AffineTransform>()

    override fun save() {
        transformStack.add(g2d.transform)
    }

    override fun restore() {
        if (transformStack.isNotEmpty()) {
            g2d.transform = transformStack.removeLast()
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
        strokePaintObj = when (value) {
            is Color -> value.toAwt()
            else -> java.awt.Color.BLACK
        }
    }

actual var DrawingContext2D.fillPaint: Paint
    get() = Color.black
    set(value) {
        fillPaintObj = when (value) {
            is Color -> value.toAwt()
            else -> java.awt.Color.BLACK
        }
    }

actual val DrawingContext2D.width: Double
    get() = g2d.clipBounds?.width?.toDouble() ?: 0.0

actual val DrawingContext2D.height: Double
    get() = g2d.clipBounds?.height?.toDouble() ?: 0.0
