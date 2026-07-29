package com.lightningkite.kiteui.views.canvas

import com.lightningkite.kiteui.models.*
import org.w3c.dom.CanvasFillRule
import org.w3c.dom.CanvasLineCap
import org.w3c.dom.CanvasLineJoin
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.CanvasTextAlign

public actual typealias DrawingContext2D = CanvasRenderingContext2D
//actual typealias TextAlign = CanvasTextAlign

public actual fun DrawingContext2D.appendArc(x: Double, y: Double, radius: Double, startAngle: Angle, endAngle: Angle, anticlockwise: Boolean) = arc(x, y, radius, startAngle.radians.toDouble(), endAngle.radians.toDouble(), anticlockwise)
public actual fun DrawingContext2D.drawOutlinedText(text: String, x: Double, y: Double):Unit = strokeText(text, x, y)
public actual fun DrawingContext2D.drawText(text: String, x: Double, y: Double):Unit = fillText(text, x, y)
public actual fun DrawingContext2D.font(size: Double, value: FontAndStyle) {
    font = "${value.weight} ${if(value.italic) "italic " else ""}${size}px ${value.font.cssFontFamilyName}"
}
public actual fun DrawingContext2D.textAlign(alignment: TextAlign){
    textAlign = alignment.toString().asDynamic().unsafeCast<CanvasTextAlign>()
}
public actual fun DrawingContext2D.fill() = fill("nonzero".asDynamic().unsafeCast<CanvasFillRule>())
public actual fun DrawingContext2D.fillEvenOdd() = fill("evenodd".asDynamic().unsafeCast<CanvasFillRule>())

public actual var DrawingContext2D.strokePaint: Paint
    get() = when(val it = strokeStyle) {
        is String -> Color.fromHexString(it)
        else -> Color.black
    }
    set(value) {
        strokeStyle = value.toCanvasStyle(this)
    }
public actual var DrawingContext2D.fillPaint: Paint
    get() = when(val it = fillStyle) {
        is String -> Color.fromHexString(it)
        else -> Color.black
    }
    set(value) {
        fillStyle = value.toCanvasStyle(this)
    }

private fun Paint.toCanvasStyle(ctx: DrawingContext2D): dynamic = when(this) {
    is Color -> toWeb()
    is LinearGradient -> {
        // Calculate gradient line based on provided coordinates or angle
        val gradX0: Double
        val gradY0: Double
        val gradX1: Double
        val gradY1: Double
        if (x0 != null && y0 != null && x1 != null && y1 != null) {
            // Use explicit coordinates
            gradX0 = x0
            gradY0 = y0
            gradX1 = x1
            gradY1 = y1
        } else {
            // Use angle relative to canvas bounds
            val w = ctx.width
            val h = ctx.height
            val radians = angle.radians.toDouble()
            val cos = kotlin.math.cos(radians)
            val sin = kotlin.math.sin(radians)
            // Calculate gradient line through center of canvas
            val centerX = w / 2.0
            val centerY = h / 2.0
            val length = kotlin.math.max(w, h)
            gradX0 = centerX - cos * length / 2.0
            gradY0 = centerY - sin * length / 2.0
            gradX1 = centerX + cos * length / 2.0
            gradY1 = centerY + sin * length / 2.0
        }
        val gradient = ctx.createLinearGradient(gradX0, gradY0, gradX1, gradY1)
        for (stop in stops) {
            gradient.addColorStop(stop.ratio.toDouble(), stop.color.toWeb())
        }
        gradient
    }
    is RadialGradient -> {
        // Calculate gradient circle based on provided coordinates or default to canvas center
        val w = ctx.width
        val h = ctx.height
        val gradCx = cx ?: (w / 2.0)
        val gradCy = cy ?: (h / 2.0)
        val gradRadius = radius ?: (kotlin.math.min(w, h) / 2.0)
        val gradFx = fx ?: gradCx
        val gradFy = fy ?: gradCy
        // createRadialGradient(x0, y0, r0, x1, y1, r1)
        // Inner circle (focal point) to outer circle (gradient boundary)
        val gradient = ctx.createRadialGradient(gradFx, gradFy, 0.0, gradCx, gradCy, gradRadius)
        for (stop in stops) {
            gradient.addColorStop(stop.ratio.toDouble(), stop.color.toWeb())
        }
        gradient
    }
    is FadingColor -> base.toWeb()
    else -> closestColor().toWeb()
}
public actual val DrawingContext2D.width: Double get() = canvas.width.toDouble()
public actual val DrawingContext2D.height: Double get() = canvas.height.toDouble()

public actual var DrawingContext2D.lineCapStyle: LineCap
    get() = when (asDynamic().lineCap.unsafeCast<String>()) {
        "butt" -> LineCap.butt
        "round" -> LineCap.round
        "square" -> LineCap.square
        else -> LineCap.butt
    }
    set(value) {
        asDynamic().lineCap = when (value) {
            LineCap.butt -> "butt"
            LineCap.round -> "round"
            LineCap.square -> "square"
        }
    }

public actual var DrawingContext2D.lineJoinStyle: LineJoin
    get() = when (asDynamic().lineJoin.unsafeCast<String>()) {
        "miter" -> LineJoin.miter
        "round" -> LineJoin.round
        "bevel" -> LineJoin.bevel
        else -> LineJoin.miter
    }
    set(value) {
        asDynamic().lineJoin = when (value) {
            LineJoin.miter -> "miter"
            LineJoin.round -> "round"
            LineJoin.bevel -> "bevel"
        }
    }

public actual fun DrawingContext2D.clear() {
    clearRect(0.0, 0.0, width, height)
}

// ============================================================================
// Clipping
// ============================================================================

public actual fun DrawingContext2D.clip() {
    asDynamic().clip()
}

public actual fun DrawingContext2D.clip(fillRule: FillRule) {
    asDynamic().clip(fillRule.toCanvasFillRule())
}

public actual fun DrawingContext2D.resetClip() {
    // Canvas API doesn't have a direct resetClip. The typical approach is to use save/restore.
    // Since we can't reset clip without affecting other state, we clip to a huge rect as a workaround.
    // Note: This implementation has limitations - ideally callers should use save/restore around clip operations.
    save()
    asDynamic().resetTransform()
    beginPath()
    rect(-1e9, -1e9, 2e9, 2e9)
    asDynamic().clip()
    restore()
}

// ============================================================================
// Line Dash
// ============================================================================

public actual fun DrawingContext2D.setLineDash(segments: List<Double>) {
    asDynamic().setLineDash(segments.toTypedArray())
}

public actual fun DrawingContext2D.getLineDash(): List<Double> {
    return (asDynamic().getLineDash() as Array<Double>).toList()
}

// ============================================================================
// Text Metrics
// ============================================================================

public actual fun DrawingContext2D.measureText(text: String): TextMetrics {
    val jsMetrics = asDynamic().measureText(text)
    return TextMetrics(
        width = jsMetrics.width as Double,
        height = ((jsMetrics.actualBoundingBoxAscent as? Double ?: 0.0) + (jsMetrics.actualBoundingBoxDescent as? Double ?: 0.0)),
        ascent = jsMetrics.actualBoundingBoxAscent as? Double ?: 0.0,
        descent = jsMetrics.actualBoundingBoxDescent as? Double ?: 0.0
    )
}

// ============================================================================
// Shapes
// ============================================================================

public actual fun DrawingContext2D.roundRect(x: Double, y: Double, width: Double, height: Double, radius: Double) {
    asDynamic().roundRect(x, y, width, height, radius)
}

public actual fun DrawingContext2D.roundRect(
    x: Double, y: Double, width: Double, height: Double,
    topLeftRadius: Double, topRightRadius: Double,
    bottomRightRadius: Double, bottomLeftRadius: Double
) {
    // roundRect accepts an array of radii: [top-left, top-right, bottom-right, bottom-left]
    asDynamic().roundRect(x, y, width, height, arrayOf(topLeftRadius, topRightRadius, bottomRightRadius, bottomLeftRadius))
}

public actual fun DrawingContext2D.ellipse(
    x: Double, y: Double,
    radiusX: Double, radiusY: Double,
    rotation: Double,
    startAngle: Double, endAngle: Double,
    anticlockwise: Boolean
) {
    asDynamic().ellipse(x, y, radiusX, radiusY, rotation, startAngle, endAngle, anticlockwise)
}

// ============================================================================
// Hit Testing
// ============================================================================

public actual fun DrawingContext2D.isPointInPath(x: Double, y: Double): Boolean {
    return asDynamic().isPointInPath(x, y) as Boolean
}

public actual fun DrawingContext2D.isPointInPath(x: Double, y: Double, fillRule: FillRule): Boolean {
    return asDynamic().isPointInPath(x, y, fillRule.toCanvasFillRule()) as Boolean
}

// ============================================================================
// Shadows
// ============================================================================

public actual var DrawingContext2D.shadowBlur: Double
    get() = asDynamic().shadowBlur as Double
    set(value) { asDynamic().shadowBlur = value }

public actual var DrawingContext2D.shadowColorValue: Color
    get() = Color.fromHexString(asDynamic().shadowColor as String)
    set(value) { asDynamic().shadowColor = value.toWeb() }

public actual var DrawingContext2D.shadowOffsetX: Double
    get() = asDynamic().shadowOffsetX as Double
    set(value) { asDynamic().shadowOffsetX = value }

public actual var DrawingContext2D.shadowOffsetY: Double
    get() = asDynamic().shadowOffsetY as Double
    set(value) { asDynamic().shadowOffsetY = value }

// ============================================================================
// Transform
// ============================================================================

public actual fun DrawingContext2D.getTransform(): TransformMatrix {
    val matrix = asDynamic().getTransform()
    return TransformMatrix(
        a = matrix.a as Double,
        b = matrix.b as Double,
        c = matrix.c as Double,
        d = matrix.d as Double,
        e = matrix.e as Double,
        f = matrix.f as Double
    )
}

public actual fun DrawingContext2D.setTransform(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double) {
    asDynamic().setTransform(a, b, c, d, e, f)
}

// ============================================================================
// Helpers
// ============================================================================

private fun FillRule.toCanvasFillRule(): CanvasFillRule = when (this) {
    FillRule.nonzero -> "nonzero".asDynamic().unsafeCast<CanvasFillRule>()
    FillRule.evenodd -> "evenodd".asDynamic().unsafeCast<CanvasFillRule>()
}