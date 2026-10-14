package com.lightningkite.kiteui.views.canvas

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathHitTester
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import com.lightningkite.kiteui.models.Angle
import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.FadingColor
import com.lightningkite.kiteui.models.FontAndStyle
import com.lightningkite.kiteui.models.LinearGradient
import com.lightningkite.kiteui.models.Paint
import com.lightningkite.kiteui.models.RadialGradient
import com.lightningkite.kiteui.views.toCompose
import com.lightningkite.kiteui.views.toFontFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.Paint as ComposePaint

public actual abstract class DrawingContext2D {
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

// Everything HTML canvas's save() and restore() cover.
internal data class CanvasState(
    val transform: TransformMatrix = TransformMatrix.identity,
    // In canvas pixels, unaffected by [transform]; null means no clip.
    val clip: Path? = null,
    val fill: Paint = Color.black,
    val stroke: Paint = Color.black,
    val lineWidth: Double = 1.0,
    val miterLimit: Double = 10.0,
    val lineDash: List<Double> = emptyList(),
    val lineDashOffset: Double = 0.0,
    // The web backend sets round caps and joins before every draw, overriding HTML's butt and miter.
    val lineCap: LineCap = LineCap.round,
    val lineJoin: LineJoin = LineJoin.round,
    val globalAlpha: Double = 1.0,
    val blendMode: BlendMode = BlendMode.SrcOver,
    val imageSmoothingEnabled: Boolean = true,
    val shadowBlur: Double = 0.0,
    val shadowColor: Color = Color.transparent,
    val shadowOffsetX: Double = 0.0,
    val shadowOffsetY: Double = 0.0,
    val font: TextStyle,
    val textAlign: TextAlign = TextAlign.start,
)

// HTML canvas 2D on a Compose [DrawScope], in its pixels.  As in HTML, the current [path] is kept in canvas
// pixels: each point is transformed as it's added, so changing the transform part-way through a path works.
internal class ComposeDrawingContext2D(
    internal val scope: DrawScope,
    internal val textMeasurer: TextMeasurer,
) : DrawingContext2D() {
    // HTML's default font is 10px sans-serif.
    internal var state = CanvasState(font = TextStyle(fontSize = with(scope) { 10f.toSp() }))
    private val saved = ArrayList<CanvasState>()
    internal val path = Path()

    override fun save() {
        saved.add(state)
    }

    override fun restore() {
        saved.removeLastOrNull()?.let { state = it }
    }

    override fun transform(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double) {
        state = state.copy(transform = state.transform * TransformMatrix(a, b, c, d, e, f))
    }

    override fun scale(x: Double, y: Double) = transform(x, 0.0, 0.0, y, 0.0, 0.0)
    override fun rotate(angle: Double) = transform(cos(angle), sin(angle), -sin(angle), cos(angle), 0.0, 0.0)
    override fun translate(x: Double, y: Double) = transform(1.0, 0.0, 0.0, 1.0, x, y)

    override var globalAlpha: Double
        get() = state.globalAlpha
        set(value) { state = state.copy(globalAlpha = value) }
    override var globalCompositeOperation: String
        get() = blendModes.entries.first { it.value == state.blendMode }.key
        set(value) {
            state = state.copy(blendMode = blendModes[value] ?: throw IllegalArgumentException("Unknown globalCompositeOperation '$value'"))
        }
    // Only affects drawn images, which this API can't draw yet.
    override var imageSmoothingEnabled: Boolean
        get() = state.imageSmoothingEnabled
        set(value) { state = state.copy(imageSmoothingEnabled = value) }
    override var lineWidth: Double
        get() = state.lineWidth
        set(value) { state = state.copy(lineWidth = value) }
    override var miterLimit: Double
        get() = state.miterLimit
        set(value) { state = state.copy(miterLimit = value) }
    override var lineDashOffset: Double
        get() = state.lineDashOffset
        set(value) { state = state.copy(lineDashOffset = value) }

    override fun clearRect(x: Double, y: Double, w: Double, h: Double) = withState {
        // Ignores alpha, shadow and compositing, as in HTML.
        drawRect(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat(), ComposePaint().apply { blendMode = BlendMode.Clear })
    }

    override fun fillRect(x: Double, y: Double, w: Double, h: Double) = withState {
        drawRect(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat(), paint(stroke = false))
    }

    override fun strokeRect(x: Double, y: Double, w: Double, h: Double) = withState {
        drawRect(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat(), paint(stroke = true))
    }

    override fun beginPath() = path.reset()
    override fun closePath() = path.close()

    override fun moveTo(x: Double, y: Double) {
        val p = state.transform.map(x, y)
        path.moveTo(p.x, p.y)
    }

    override fun lineTo(x: Double, y: Double) {
        val p = state.transform.map(x, y)
        path.lineTo(p.x, p.y)
    }

    override fun quadraticCurveTo(cpx: Double, cpy: Double, x: Double, y: Double) {
        val c = state.transform.map(cpx, cpy)
        val p = state.transform.map(x, y)
        path.quadraticTo(c.x, c.y, p.x, p.y)
    }

    override fun bezierCurveTo(cp1x: Double, cp1y: Double, cp2x: Double, cp2y: Double, x: Double, y: Double) {
        val c1 = state.transform.map(cp1x, cp1y)
        val c2 = state.transform.map(cp2x, cp2y)
        val p = state.transform.map(x, y)
        path.cubicTo(c1.x, c1.y, c2.x, c2.y, p.x, p.y)
    }

    // HTML defines rect() as these steps.
    override fun rect(x: Double, y: Double, w: Double, h: Double) {
        moveTo(x, y)
        lineTo(x + w, y)
        lineTo(x + w, y + h)
        lineTo(x, y + h)
        closePath()
        moveTo(x, y)
    }

    override fun stroke() = withState { drawPath(pathInUserSpace(), paint(stroke = true)) }

    internal fun fill(rule: PathFillType) = withState {
        drawPath(pathInUserSpace().apply { fillType = rule }, paint(stroke = false))
    }

    // Adds to [path] with [build] working in [space] rather than in canvas pixels.
    internal inline fun addInSpace(space: TransformMatrix, build: Path.() -> Unit) {
        path.transform(space.toCompose().apply { invert() })
        path.build()
        path.transform(space.toCompose())
    }

    // Drawn under the transform, so line widths, dashes and gradients are in user space, as in HTML.
    private fun pathInUserSpace() = Path().apply {
        addPath(path)
        transform(state.transform.toCompose().apply { invert() })
    }

    // Runs [draw] in user space, under the current clip.
    internal inline fun withState(draw: Canvas.() -> Unit) {
        val canvas = scope.drawContext.canvas
        canvas.save()
        state.clip?.let { canvas.clipPath(it) }
        canvas.concat(state.transform.toCompose())
        canvas.draw()
        canvas.restore()
    }

    private fun paint(stroke: Boolean) = ComposePaint().apply {
        val state = state
        (if (stroke) state.stroke else state.fill).toCanvasBrush().applyTo(scope.size, this, state.globalAlpha.toFloat())
        blendMode = state.blendMode
        if (stroke) {
            style = PaintingStyle.Stroke
            strokeWidth = state.lineWidth.toFloat()
            strokeMiterLimit = state.miterLimit.toFloat()
            strokeCap = state.lineCap.toCompose()
            strokeJoin = state.lineJoin.toCompose()
            if (state.lineDash.isNotEmpty()) {
                pathEffect = PathEffect.dashPathEffect(state.lineDash.map { it.toFloat() }.toFloatArray(), state.lineDashOffset.toFloat())
            }
        }
        if (state.shadowColor.alpha > 0f) setShadow(
            state.shadowColor.toCompose(),
            Offset(state.shadowOffsetX.toFloat(), state.shadowOffsetY.toFloat()),
            state.shadowBlur.toFloat(),
        )
    }

    internal fun drawText(text: String, x: Double, y: Double, stroke: Boolean) {
        val state = state
        val layout = textMeasurer.measure(text, state.font)
        val width = layout.size.width.toFloat()
        val rtl = scope.layoutDirection == LayoutDirection.Rtl
        val anchor = when (state.textAlign) {
            TextAlign.left -> 0f
            TextAlign.right -> width
            TextAlign.center -> width / 2
            TextAlign.start -> if (rtl) width else 0f
            TextAlign.end -> if (rtl) 0f else width
        }
        withState {
            scope.drawText(
                layout,
                brush = (if (stroke) state.stroke else state.fill).toCanvasBrush(),
                // HTML's default baseline is the alphabetic one.
                topLeft = Offset(x.toFloat() - anchor, y.toFloat() - layout.firstBaseline),
                alpha = state.globalAlpha.toFloat(),
                shadow = if (state.shadowColor.alpha > 0f) Shadow(
                    state.shadowColor.toCompose(),
                    Offset(state.shadowOffsetX.toFloat(), state.shadowOffsetY.toFloat()),
                    state.shadowBlur.toFloat(),
                ) else null,
                drawStyle = if (stroke) Stroke(
                    width = state.lineWidth.toFloat(),
                    miter = state.miterLimit.toFloat(),
                    cap = state.lineCap.toCompose(),
                    join = state.lineJoin.toCompose(),
                ) else Fill,
                blendMode = state.blendMode,
            )
        }
    }

    // Gradients without coordinates span the canvas, as on the web.
    private fun Paint.toCanvasBrush(): Brush = when (this) {
        is Color -> SolidColor(toCompose())
        // TODO: animate between base and alternate; animations are out of scope for now.
        is FadingColor -> SolidColor(base.toCompose())
        is LinearGradient -> {
            val stops = stops.map { it.ratio to it.color.toCompose() }.toTypedArray()
            if (x0 != null && y0 != null && x1 != null && y1 != null) {
                Brush.linearGradient(*stops, start = Offset(x0.toFloat(), y0.toFloat()), end = Offset(x1.toFloat(), y1.toFloat()))
            } else {
                val center = scope.center
                val halfLength = max(scope.size.width, scope.size.height) / 2
                val direction = Offset(cos(angle.radians), sin(angle.radians))
                Brush.linearGradient(*stops, start = center - direction * halfLength, end = center + direction * halfLength)
            }
        }
        is RadialGradient -> {
            val cx = cx ?: (scope.size.width / 2.0)
            val cy = cy ?: (scope.size.height / 2.0)
            if ((fx ?: cx) != cx || (fy ?: cy) != cy) TODO("Compose backend: radial gradients with a focal point")
            Brush.radialGradient(
                *stops.map { it.ratio to it.color.toCompose() }.toTypedArray(),
                center = Offset(cx.toFloat(), cy.toFloat()),
                radius = (radius ?: (min(scope.size.width, scope.size.height) / 2.0)).toFloat(),
            )
        }
    }
}

private val DrawingContext2D.compose: ComposeDrawingContext2D get() = this as ComposeDrawingContext2D

// Gives this paint a drop shadow; [blur] is HTML canvas's shadowBlur.
internal expect fun ComposePaint.setShadow(color: ComposeColor, offset: Offset, blur: Float)

private val blendModes = mapOf(
    "source-over" to BlendMode.SrcOver,
    "source-in" to BlendMode.SrcIn,
    "source-out" to BlendMode.SrcOut,
    "source-atop" to BlendMode.SrcAtop,
    "destination-over" to BlendMode.DstOver,
    "destination-in" to BlendMode.DstIn,
    "destination-out" to BlendMode.DstOut,
    "destination-atop" to BlendMode.DstAtop,
    "lighter" to BlendMode.Plus,
    "copy" to BlendMode.Src,
    "xor" to BlendMode.Xor,
    "multiply" to BlendMode.Multiply,
    "screen" to BlendMode.Screen,
    "overlay" to BlendMode.Overlay,
    "darken" to BlendMode.Darken,
    "lighten" to BlendMode.Lighten,
    "color-dodge" to BlendMode.ColorDodge,
    "color-burn" to BlendMode.ColorBurn,
    "hard-light" to BlendMode.Hardlight,
    "soft-light" to BlendMode.Softlight,
    "difference" to BlendMode.Difference,
    "exclusion" to BlendMode.Exclusion,
    "hue" to BlendMode.Hue,
    "saturation" to BlendMode.Saturation,
    "color" to BlendMode.Color,
    "luminosity" to BlendMode.Luminosity,
)

private operator fun TransformMatrix.times(o: TransformMatrix) = TransformMatrix(
    a = a * o.a + c * o.b,
    b = b * o.a + d * o.b,
    c = a * o.c + c * o.d,
    d = b * o.c + d * o.d,
    e = a * o.e + c * o.f + e,
    f = b * o.e + d * o.f + f,
)

private fun TransformMatrix.map(x: Double, y: Double) = Offset((a * x + c * y + e).toFloat(), (b * x + d * y + f).toFloat())

private fun TransformMatrix.toCompose() = Matrix().apply {
    values[Matrix.ScaleX] = a.toFloat()
    values[Matrix.SkewY] = b.toFloat()
    values[Matrix.SkewX] = c.toFloat()
    values[Matrix.ScaleY] = d.toFloat()
    values[Matrix.TranslateX] = e.toFloat()
    values[Matrix.TranslateY] = f.toFloat()
}

private fun LineCap.toCompose() = when (this) {
    LineCap.butt -> StrokeCap.Butt
    LineCap.round -> StrokeCap.Round
    LineCap.square -> StrokeCap.Square
}

private fun LineJoin.toCompose() = when (this) {
    LineJoin.miter -> StrokeJoin.Miter
    LineJoin.round -> StrokeJoin.Round
    LineJoin.bevel -> StrokeJoin.Bevel
}

private fun FillRule.toCompose() = when (this) {
    FillRule.nonzero -> PathFillType.NonZero
    FillRule.evenodd -> PathFillType.EvenOdd
}

// Adds an arc of the ellipse in [oval] as HTML's arc() does: a full turn or more is the whole ellipse, and
// otherwise it runs from [start] to [end] (radians) in the given direction, joined to the path by a line.
private fun Path.htmlArc(oval: Rect, start: Double, end: Double, anticlockwise: Boolean) {
    val turn = 2 * PI
    val sweep = when {
        !anticlockwise -> if (end - start >= turn) turn else (end - start).mod(turn)
        else -> if (start - end >= turn) -turn else -(start - end).mod(turn)
    }
    val startDegrees = (start * 180 / PI).toFloat()
    val halfSweepDegrees = (sweep * 90 / PI).toFloat()
    // Skia adds nothing for a 360 degree arcTo, so add it in two halves.
    arcTo(oval, startDegrees, halfSweepDegrees, forceMoveTo = false)
    arcTo(oval, startDegrees + halfSweepDegrees, halfSweepDegrees, forceMoveTo = false)
}

public actual fun DrawingContext2D.appendArc(x: Double, y: Double, radius: Double, startAngle: Angle, endAngle: Angle, anticlockwise: Boolean): Unit =
    compose.addInSpace(compose.state.transform) {
        htmlArc(Rect(Offset(x.toFloat(), y.toFloat()), radius.toFloat()), startAngle.radians.toDouble(), endAngle.radians.toDouble(), anticlockwise)
    }

public actual fun DrawingContext2D.drawOutlinedText(text: String, x: Double, y: Double): Unit = compose.drawText(text, x, y, stroke = true)
public actual fun DrawingContext2D.drawText(text: String, x: Double, y: Double): Unit = compose.drawText(text, x, y, stroke = false)

// Like the web's canvas font, which has only these parts.
public actual fun DrawingContext2D.font(size: Double, value: FontAndStyle) {
    val c = compose
    c.state = c.state.copy(
        font = TextStyle(
            fontFamily = value.font.toFontFamily(),
            fontWeight = FontWeight(value.weight),
            fontStyle = if (value.italic) FontStyle.Italic else FontStyle.Normal,
            fontSize = with(c.scope) { size.toFloat().toSp() },
        )
    )
}

public actual fun DrawingContext2D.textAlign(alignment: TextAlign) {
    compose.state = compose.state.copy(textAlign = alignment)
}

public actual fun DrawingContext2D.clear(): Unit = clearRect(0.0, 0.0, width, height)
public actual fun DrawingContext2D.fill(): Unit = compose.fill(PathFillType.NonZero)
public actual fun DrawingContext2D.fillEvenOdd(): Unit = compose.fill(PathFillType.EvenOdd)

public actual var DrawingContext2D.strokePaint: Paint
    get() = compose.state.stroke
    set(value) { compose.state = compose.state.copy(stroke = value) }
public actual var DrawingContext2D.fillPaint: Paint
    get() = compose.state.fill
    set(value) { compose.state = compose.state.copy(fill = value) }
public actual var DrawingContext2D.lineCapStyle: LineCap
    get() = compose.state.lineCap
    set(value) { compose.state = compose.state.copy(lineCap = value) }
public actual var DrawingContext2D.lineJoinStyle: LineJoin
    get() = compose.state.lineJoin
    set(value) { compose.state = compose.state.copy(lineJoin = value) }
public actual val DrawingContext2D.width: Double get() = compose.scope.size.width.toDouble()
public actual val DrawingContext2D.height: Double get() = compose.scope.size.height.toDouble()

public actual fun DrawingContext2D.clip(): Unit = clip(FillRule.nonzero)
public actual fun DrawingContext2D.clip(fillRule: FillRule) {
    val c = compose
    val shape = Path().apply {
        addPath(c.path)
        fillType = fillRule.toCompose()
    }
    c.state = c.state.copy(clip = c.state.clip?.let { Path.combine(PathOperation.Intersect, it, shape) } ?: shape)
}

public actual fun DrawingContext2D.resetClip() {
    compose.state = compose.state.copy(clip = null)
}

public actual fun DrawingContext2D.setLineDash(segments: List<Double>) {
    // As in HTML, an odd-length pattern is repeated to make it even.
    compose.state = compose.state.copy(lineDash = if (segments.size % 2 == 1) segments + segments else segments)
}

public actual fun DrawingContext2D.getLineDash(): List<Double> = compose.state.lineDash

public actual fun DrawingContext2D.measureText(text: String): TextMetrics {
    val layout = compose.textMeasurer.measure(text, compose.state.font)
    return TextMetrics(
        width = layout.size.width.toDouble(),
        height = layout.size.height.toDouble(),
        ascent = layout.firstBaseline.toDouble(),
        descent = (layout.size.height - layout.firstBaseline).toDouble(),
    )
}

public actual fun DrawingContext2D.roundRect(x: Double, y: Double, width: Double, height: Double, radius: Double): Unit =
    roundRect(x, y, width, height, radius, radius, radius, radius)

public actual fun DrawingContext2D.roundRect(
    x: Double, y: Double, width: Double, height: Double,
    topLeftRadius: Double, topRightRadius: Double,
    bottomRightRadius: Double, bottomLeftRadius: Double
): Unit = compose.addInSpace(compose.state.transform) {
    addRoundRect(
        RoundRect(
            left = x.toFloat(), top = y.toFloat(), right = (x + width).toFloat(), bottom = (y + height).toFloat(),
            topLeftCornerRadius = CornerRadius(topLeftRadius.toFloat()),
            topRightCornerRadius = CornerRadius(topRightRadius.toFloat()),
            bottomRightCornerRadius = CornerRadius(bottomRightRadius.toFloat()),
            bottomLeftCornerRadius = CornerRadius(bottomLeftRadius.toFloat()),
        )
    )
}

public actual fun DrawingContext2D.ellipse(
    x: Double, y: Double,
    radiusX: Double, radiusY: Double,
    rotation: Double,
    startAngle: Double, endAngle: Double,
    anticlockwise: Boolean
) {
    val c = compose
    val ellipseSpace = c.state.transform *
        TransformMatrix(e = x, f = y) *
        TransformMatrix(cos(rotation), sin(rotation), -sin(rotation), cos(rotation))
    c.addInSpace(ellipseSpace) {
        htmlArc(Rect(-radiusX.toFloat(), -radiusY.toFloat(), radiusX.toFloat(), radiusY.toFloat()), startAngle, endAngle, anticlockwise)
    }
}

// As in HTML, the point is in canvas pixels, unaffected by the transform.
public actual fun DrawingContext2D.isPointInPath(x: Double, y: Double): Boolean = isPointInPath(x, y, FillRule.nonzero)
public actual fun DrawingContext2D.isPointInPath(x: Double, y: Double, fillRule: FillRule): Boolean {
    val path = compose.path
    path.fillType = fillRule.toCompose()
    return PathHitTester(path).contains(Offset(x.toFloat(), y.toFloat()))
}

public actual var DrawingContext2D.shadowBlur: Double
    get() = compose.state.shadowBlur
    set(value) { compose.state = compose.state.copy(shadowBlur = value) }
public actual var DrawingContext2D.shadowColorValue: Color
    get() = compose.state.shadowColor
    set(value) { compose.state = compose.state.copy(shadowColor = value) }
public actual var DrawingContext2D.shadowOffsetX: Double
    get() = compose.state.shadowOffsetX
    set(value) { compose.state = compose.state.copy(shadowOffsetX = value) }
public actual var DrawingContext2D.shadowOffsetY: Double
    get() = compose.state.shadowOffsetY
    set(value) { compose.state = compose.state.copy(shadowOffsetY = value) }

public actual fun DrawingContext2D.getTransform(): TransformMatrix = compose.state.transform
public actual fun DrawingContext2D.setTransform(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double) {
    compose.state = compose.state.copy(transform = TransformMatrix(a, b, c, d, e, f))
}
