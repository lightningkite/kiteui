package com.lightningkite.kiteui.views.canvas

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.View
import com.lightningkite.kiteui.models.Angle
import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.FontAndStyle
import com.lightningkite.kiteui.models.LinearGradient
import com.lightningkite.kiteui.models.RadialGradient
import com.lightningkite.kiteui.models.turns
import com.lightningkite.kiteui.views.Path.DrawingResources
import com.lightningkite.kiteui.views.direct.colorInt
import kotlin.math.cos
import kotlin.math.sin


public abstract class DrawingView : View {
    public constructor(context: Context?) : super(context)
    public constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    public constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
}
@Suppress("ACTUAL_WITHOUT_EXPECT")
public actual abstract class DrawingContext2D(public val canvas: Canvas) {
    public val currentPath: Path = Path()
    public val clearPaint: android.graphics.Paint = android.graphics.Paint().apply {
        color = android.graphics.Color.TRANSPARENT
        setXfermode(PorterDuffXfermode(PorterDuff.Mode.DST_OUT))
    }
    public var fillPaintObj: android.graphics.Paint = android.graphics.Paint().apply { style = android.graphics.Paint.Style.FILL }
    public var strokePaintObj: android.graphics.Paint = android.graphics.Paint().apply { style = android.graphics.Paint.Style.STROKE }
    public val drawingResource: DrawingResources = DrawingResources()
    public actual abstract fun save()
    public actual abstract fun restore()
    public actual abstract fun scale(x: Double, y: Double)
    public actual abstract fun rotate(angle: Double)
    public actual abstract fun translate(x: Double, y: Double)
    public actual abstract fun transform(
        a: Double,
        b: Double,
        c: Double,
        d: Double,
        e: Double,
        f: Double
    )

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
    public abstract fun setLineDash(segments: Array<Double>)
    public abstract fun getLineDash(): Array<Double>
    public actual abstract fun closePath()
    public actual abstract fun moveTo(x: Double, y: Double)
    public actual abstract fun lineTo(x: Double, y: Double)
    public actual abstract fun quadraticCurveTo(cpx: Double, cpy: Double, x: Double, y: Double)
    public actual abstract fun bezierCurveTo(
        cp1x: Double,
        cp1y: Double,
        cp2x: Double,
        cp2y: Double,
        x: Double,
        y: Double
    )

//    actual abstract fun arcTo(
//        x1: Double,
//        y1: Double,
//        x2: Double,
//        y2: Double,
//        radius: Double
//    )
//
//    actual abstract fun arcTo(
//        x1: Double,
//        y1: Double,
//        x2: Double,
//        y2: Double,
//        radiusX: Double,
//        radiusY: Double,
//        rotation: Double
//    )

    public actual abstract fun rect(x: Double, y: Double, w: Double, h: Double)

}

public class DrawingContext2DImpl(canvas: Canvas): DrawingContext2D(canvas) {
    // Internal state tracking
    internal var lineDashSegments: List<Double> = emptyList()
    internal var _shadowBlur: Double = 0.0
    internal var _shadowColor: Color = Color.transparent
    internal var _shadowOffsetX: Double = 0.0
    internal var _shadowOffsetY: Double = 0.0
    internal val transformMatrix: Matrix = Matrix()

    override fun save() {
        canvas.save()
    }
    override fun restore() {
        canvas.restore()
    }
    override fun scale(x: Double, y: Double) {
        canvas.scale(x.toFloat(), y.toFloat())
        transformMatrix.preScale(x.toFloat(), y.toFloat())
    }
    override fun rotate(angle: Double) {
        canvas.rotate(Math.toDegrees(angle).toFloat())
        transformMatrix.preRotate(Math.toDegrees(angle).toFloat())
    }
    override fun translate(x: Double, y: Double) {
        canvas.translate(x.toFloat(), y.toFloat())
        transformMatrix.preTranslate(x.toFloat(), y.toFloat())
    }
    override fun transform(
        a: Double,
        b: Double,
        c: Double,
        d: Double,
        e: Double,
        f: Double,
    ) {
        // Canvas transform matrix format:
        // | a  c  e |
        // | b  d  f |
        // | 0  0  1 |
        // Android Matrix format (row-major):
        // | MSCALE_X  MSKEW_X   MTRANS_X |   indices: 0, 1, 2
        // | MSKEW_Y   MSCALE_Y  MTRANS_Y |   indices: 3, 4, 5
        // | MPERSP_0  MPERSP_1  MPERSP_2 |   indices: 6, 7, 8
        val m = Matrix().apply {
            setValues(floatArrayOf(
                a.toFloat(), c.toFloat(), e.toFloat(),
                b.toFloat(), d.toFloat(), f.toFloat(),
                0f, 0f, 1f
            ))
        }
        canvas.concat(m)
        transformMatrix.preConcat(m)
    }
    private var _globalAlpha: Double = 1.0
    override var globalAlpha: Double
        get() = _globalAlpha
        set(value) {
            _globalAlpha = value.coerceIn(0.0, 1.0)
            val alpha = (_globalAlpha * 255).toInt()
            fillPaintObj.alpha = alpha
            strokePaintObj.alpha = alpha
        }
    override var globalCompositeOperation: String
        get() = "source-over"
        set(value) {
            // Map composite operation strings to PorterDuff modes
            val mode = when(value) {
                "source-over" -> PorterDuff.Mode.SRC_OVER
                "source-in" -> PorterDuff.Mode.SRC_IN
                "source-out" -> PorterDuff.Mode.SRC_OUT
                "source-atop" -> PorterDuff.Mode.SRC_ATOP
                "destination-over" -> PorterDuff.Mode.DST_OVER
                "destination-in" -> PorterDuff.Mode.DST_IN
                "destination-out" -> PorterDuff.Mode.DST_OUT
                "destination-atop" -> PorterDuff.Mode.DST_ATOP
                "xor" -> PorterDuff.Mode.XOR
                "lighter" -> PorterDuff.Mode.ADD
                "copy" -> PorterDuff.Mode.SRC
                "clear" -> PorterDuff.Mode.CLEAR
                else -> PorterDuff.Mode.SRC_OVER
            }
            fillPaintObj.setXfermode(PorterDuffXfermode(mode))
            strokePaintObj.setXfermode(PorterDuffXfermode(mode))
        }
    override var imageSmoothingEnabled: Boolean
        get() = fillPaintObj.isAntiAlias
        set(value) {
            fillPaintObj.isAntiAlias = value
            strokePaintObj.isAntiAlias = value
            fillPaintObj.isFilterBitmap = value
            strokePaintObj.isFilterBitmap = value
        }
    override fun clearRect(x: Double, y: Double, w: Double, h: Double) {
        canvas.drawRect(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat(), clearPaint)
    }
    override fun fillRect(x: Double, y: Double, w: Double, h: Double) {
        canvas.drawRect(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat(), fillPaintObj)
    }
    override fun strokeRect(x: Double, y: Double, w: Double, h: Double){
        canvas.drawRect(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat(), strokePaintObj)
    }
    override fun beginPath() {
        currentPath.reset()
    }
    override fun stroke() {
        canvas.drawPath(currentPath, strokePaintObj)
    }
    override var lineWidth: Double
        get() = strokePaintObj.strokeWidth.toDouble()
        set(value) { strokePaintObj.strokeWidth = value.toFloat() }
    override var miterLimit: Double
        get() = strokePaintObj.strokeMiter.toDouble()
        set(value) { strokePaintObj.strokeMiter = value.toFloat() }
    private var _lineDashOffset: Double = 0.0
    override var lineDashOffset: Double
        get() = _lineDashOffset
        set(value) {
            _lineDashOffset = value
            applyLineDash()
        }
    override fun setLineDash(segments: Array<Double>) {
        lineDashSegments = segments.toList()
        applyLineDash()
    }
    override fun getLineDash(): Array<Double> {
        return lineDashSegments.toTypedArray()
    }
    private fun applyLineDash() {
        if (lineDashSegments.isEmpty()) {
            strokePaintObj.pathEffect = null
        } else {
            strokePaintObj.pathEffect = DashPathEffect(
                lineDashSegments.map { it.toFloat() }.toFloatArray(),
                _lineDashOffset.toFloat()
            )
        }
    }
    override fun closePath() {
        currentPath.close()
    }
    override fun moveTo(x: Double, y: Double) {
        currentPath.moveTo(x.toFloat(), y.toFloat())
    }
    override fun lineTo(x: Double, y: Double) {
        currentPath.lineTo(x.toFloat(), y.toFloat())
    }
    override fun quadraticCurveTo(cpx: Double, cpy: Double, x: Double, y: Double) {
        currentPath.quadTo(cpx.toFloat(), cpy.toFloat(), x.toFloat(), y.toFloat())
    }
    override fun bezierCurveTo(
        cp1x: Double,
        cp1y: Double,
        cp2x: Double,
        cp2y: Double,
        x: Double,
        y: Double,
    ) {
        currentPath.cubicTo(cp1x.toFloat(), cp1y.toFloat(),cp2x.toFloat(), cp2y.toFloat(), x.toFloat(), y.toFloat())
    }

    override fun rect(x: Double, y: Double, w: Double, h: Double) {
        currentPath.addRect(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat(), Path.Direction.CCW)
    }

//    override fun arcTo(
//        x1: Double,
//        y1: Double,
//        x2: Double,
//        y2: Double,
//        radius: Double
//    ){
//        currentPath.addArc()
//    }
//
//    override fun arcTo(
//        x1: Double,
//        y1: Double,
//        x2: Double,
//        y2: Double,
//        radiusX: Double,
//        radiusY: Double,
//        rotation: Double
//    ){
//        currentPath.addArc()
//    }
}

public actual fun DrawingContext2D.fill() {
    currentPath.fillType = Path.FillType.WINDING
    canvas.drawPath(currentPath, fillPaintObj)
}
public actual fun DrawingContext2D.fillEvenOdd()  {
    currentPath.fillType = Path.FillType.EVEN_ODD
    canvas.drawPath(currentPath, fillPaintObj)
}
public actual var DrawingContext2D.strokePaint: com.lightningkite.kiteui.models.Paint
    get() = Color.fromInt(strokePaintObj.color)
    set(value) {
        applyPaintToAndroidPaint(value, strokePaintObj)
    }
public actual var DrawingContext2D.fillPaint: com.lightningkite.kiteui.models.Paint
    get() = Color.fromInt(fillPaintObj.color)
    set(value) {
        applyPaintToAndroidPaint(value, fillPaintObj)
    }

private fun DrawingContext2D.applyPaintToAndroidPaint(
    paint: com.lightningkite.kiteui.models.Paint,
    target: android.graphics.Paint
) {
    when (paint) {
        is Color -> {
            target.shader = null
            target.color = paint.toInt()
        }
        is LinearGradient -> {
            val x0 = paint.x0?.toFloat() ?: 0f
            val y0 = paint.y0?.toFloat() ?: 0f
            val x1 = paint.x1?.toFloat() ?: width.toFloat()
            val y1 = paint.y1?.toFloat() ?: 0f

            if (paint.stops.isNotEmpty()) {
                val colors = paint.stops.map { it.color.toInt() }.toIntArray()
                val positions = paint.stops.map { it.ratio }.toFloatArray()
                target.shader = android.graphics.LinearGradient(
                    x0, y0, x1, y1,
                    colors, positions,
                    Shader.TileMode.CLAMP
                )
            } else {
                target.shader = null
                target.color = android.graphics.Color.TRANSPARENT
            }
        }
        is RadialGradient -> {
            val cx = paint.cx?.toFloat() ?: (width / 2).toFloat()
            val cy = paint.cy?.toFloat() ?: (height / 2).toFloat()
            val radius = paint.radius?.toFloat() ?: (minOf(width, height) / 2).toFloat()

            if (paint.stops.isNotEmpty() && radius > 0) {
                val colors = paint.stops.map { it.color.toInt() }.toIntArray()
                val positions = paint.stops.map { it.ratio }.toFloatArray()
                target.shader = android.graphics.RadialGradient(
                    cx, cy, radius,
                    colors, positions,
                    Shader.TileMode.CLAMP
                )
            } else {
                target.shader = null
                target.color = android.graphics.Color.TRANSPARENT
            }
        }
        else -> {
            // FadingColor or other types - use closest color
            target.shader = null
            target.color = paint.colorInt()
        }
    }
}
public actual val DrawingContext2D.width: Double
    get() = canvas.width.toDouble()
public actual val DrawingContext2D.height: Double
    get() = canvas.height.toDouble()

public actual fun DrawingContext2D.appendArc(
    x: Double,
    y: Double,
    radius: Double,
    startAngle: Angle,
    endAngle: Angle,
    anticlockwise: Boolean
) {
    // Calculate sweep angle directly without using angleTo (which normalizes to shortest path)
    // This ensures full circles work correctly
    val startDegrees = startAngle.degrees
    val endDegrees = endAngle.degrees

    // Calculate raw difference
    var sweepDegrees = endDegrees - startDegrees

    // Normalize based on direction
    if (anticlockwise) {
        // For anticlockwise, sweep should be negative
        if (sweepDegrees > 0) {
            sweepDegrees -= 360f
        }
        // Handle full circle case
        if (sweepDegrees == 0f && endAngle.turns != startAngle.turns) {
            sweepDegrees = -360f
        }
    } else {
        // For clockwise, sweep should be positive
        if (sweepDegrees < 0) {
            sweepDegrees += 360f
        }
        // Handle full circle case
        if (sweepDegrees == 0f && endAngle.turns != startAngle.turns) {
            sweepDegrees = 360f
        }
    }

    val oval = RectF(
        (x - radius).toFloat(),
        (y - radius).toFloat(),
        (x + radius).toFloat(),
        (y + radius).toFloat()
    )

    // Use addArc instead of arcTo for standalone arc drawing
    // arcTo connects to current point which doesn't work correctly after beginPath()
    currentPath.addArc(oval, startDegrees, sweepDegrees)
}

public actual fun DrawingContext2D.drawText(
    text: String,
    x: Double,
    y: Double
) {
    canvas.drawText(
        text,
        x.toFloat(),
        y.toFloat(),
        fillPaintObj
    )
}

public actual fun DrawingContext2D.drawOutlinedText(
    text: String,
    x: Double,
    y: Double
) {
    canvas.drawText(
        text,
        x.toFloat(),
        y.toFloat(),
        strokePaintObj
    )
}

public actual fun DrawingContext2D.font(
    size: Double,
    value: FontAndStyle
) {
    fillPaintObj.setTypeface(value.font.toTypeface())
    fillPaintObj.textSize = size.toFloat()
}

public actual fun DrawingContext2D.textAlign(alignment: TextAlign) {
    fillPaintObj.textAlign = when(alignment) {
        TextAlign.start -> android.graphics.Paint.Align.LEFT  // TODO: locales
        TextAlign.end -> android.graphics.Paint.Align.RIGHT  // TODO: locales
        TextAlign.left -> android.graphics.Paint.Align.LEFT
        TextAlign.right -> android.graphics.Paint.Align.RIGHT
        TextAlign.center -> android.graphics.Paint.Align.CENTER
    }
}

public actual fun DrawingContext2D.clear() {
    canvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.DST_OUT)
}

public actual var DrawingContext2D.lineCapStyle: LineCap
    get() = when (strokePaintObj.strokeCap) {
        android.graphics.Paint.Cap.BUTT -> LineCap.butt
        android.graphics.Paint.Cap.ROUND -> LineCap.round
        android.graphics.Paint.Cap.SQUARE -> LineCap.square
        else -> LineCap.butt
    }
    set(value) {
        strokePaintObj.strokeCap = when (value) {
            LineCap.butt -> android.graphics.Paint.Cap.BUTT
            LineCap.round -> android.graphics.Paint.Cap.ROUND
            LineCap.square -> android.graphics.Paint.Cap.SQUARE
        }
    }

public actual var DrawingContext2D.lineJoinStyle: LineJoin
    get() = when (strokePaintObj.strokeJoin) {
        android.graphics.Paint.Join.MITER -> LineJoin.miter
        android.graphics.Paint.Join.ROUND -> LineJoin.round
        android.graphics.Paint.Join.BEVEL -> LineJoin.bevel
        else -> LineJoin.miter
    }
    set(value) {
        strokePaintObj.strokeJoin = when (value) {
            LineJoin.miter -> android.graphics.Paint.Join.MITER
            LineJoin.round -> android.graphics.Paint.Join.ROUND
            LineJoin.bevel -> android.graphics.Paint.Join.BEVEL
        }
    }

// ============================================================================
// Clipping
// ============================================================================

public actual fun DrawingContext2D.clip() {
    currentPath.fillType = Path.FillType.WINDING
    canvas.clipPath(currentPath)
}

public actual fun DrawingContext2D.clip(fillRule: FillRule) {
    currentPath.fillType = when (fillRule) {
        FillRule.nonzero -> Path.FillType.WINDING
        FillRule.evenodd -> Path.FillType.EVEN_ODD
    }
    canvas.clipPath(currentPath)
}

@Suppress("DEPRECATION")
public actual fun DrawingContext2D.resetClip() {
    // Note: Region.Op.REPLACE is deprecated but there's no direct replacement for resetting clip
    // The recommended approach is to use save/restore, but this function is for cases where
    // that's not possible
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // On API 26+, we need to use clipOutPath with an inverted path or just restore to a saved state
        // Since resetClip needs to reset without save/restore, we use the deprecated API
        canvas.clipRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), Region.Op.REPLACE)
    } else {
        canvas.clipRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), Region.Op.REPLACE)
    }
}

// ============================================================================
// Line Dash
// ============================================================================

public actual fun DrawingContext2D.setLineDash(segments: List<Double>) {
    if (this is DrawingContext2DImpl) {
        lineDashSegments = segments
        if (segments.isEmpty()) {
            strokePaintObj.pathEffect = null
        } else {
            strokePaintObj.pathEffect = DashPathEffect(
                segments.map { it.toFloat() }.toFloatArray(),
                lineDashOffset.toFloat()
            )
        }
    }
}

public actual fun DrawingContext2D.getLineDash(): List<Double> {
    return if (this is DrawingContext2DImpl) {
        lineDashSegments
    } else {
        emptyList()
    }
}

// ============================================================================
// Text Metrics
// ============================================================================

public actual fun DrawingContext2D.measureText(text: String): TextMetrics {
    val width = fillPaintObj.measureText(text).toDouble()
    val fontMetrics = fillPaintObj.fontMetrics
    val ascent = -fontMetrics.ascent.toDouble()  // ascent is negative in Android
    val descent = fontMetrics.descent.toDouble()
    val height = ascent + descent
    return TextMetrics(
        width = width,
        height = height,
        ascent = ascent,
        descent = descent
    )
}

// ============================================================================
// Shapes
// ============================================================================

public actual fun DrawingContext2D.roundRect(x: Double, y: Double, width: Double, height: Double, radius: Double) {
    val r = radius.toFloat()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        currentPath.addRoundRect(
            x.toFloat(),
            y.toFloat(),
            (x + width).toFloat(),
            (y + height).toFloat(),
            r, r,
            Path.Direction.CW
        )
    } else {
        currentPath.addRoundRect(
            RectF(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat()),
            r, r,
            Path.Direction.CW
        )
    }
}

public actual fun DrawingContext2D.roundRect(
    x: Double, y: Double, width: Double, height: Double,
    topLeftRadius: Double, topRightRadius: Double,
    bottomRightRadius: Double, bottomLeftRadius: Double
) {
    // Android's addRoundRect with float array expects radii in order:
    // [topLeftX, topLeftY, topRightX, topRightY, bottomRightX, bottomRightY, bottomLeftX, bottomLeftY]
    val radii = floatArrayOf(
        topLeftRadius.toFloat(), topLeftRadius.toFloat(),
        topRightRadius.toFloat(), topRightRadius.toFloat(),
        bottomRightRadius.toFloat(), bottomRightRadius.toFloat(),
        bottomLeftRadius.toFloat(), bottomLeftRadius.toFloat()
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        currentPath.addRoundRect(
            x.toFloat(),
            y.toFloat(),
            (x + width).toFloat(),
            (y + height).toFloat(),
            radii,
            Path.Direction.CW
        )
    } else {
        currentPath.addRoundRect(
            RectF(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat()),
            radii,
            Path.Direction.CW
        )
    }
}

public actual fun DrawingContext2D.ellipse(
    x: Double, y: Double,
    radiusX: Double, radiusY: Double,
    rotation: Double,
    startAngle: Double, endAngle: Double,
    anticlockwise: Boolean
) {
    // Android doesn't have a direct ellipse method, so we use arc with transformation
    val left = (x - radiusX).toFloat()
    val top = (y - radiusY).toFloat()
    val right = (x + radiusX).toFloat()
    val bottom = (y + radiusY).toFloat()
    val oval = RectF(left, top, right, bottom)

    // Convert angles from radians to degrees
    val startDegrees = Math.toDegrees(startAngle).toFloat()
    val endDegrees = Math.toDegrees(endAngle).toFloat()

    // Calculate sweep angle
    var sweepDegrees = if (anticlockwise) {
        startDegrees - endDegrees
    } else {
        endDegrees - startDegrees
    }

    // Normalize sweep angle
    if (anticlockwise && sweepDegrees > 0) {
        sweepDegrees -= 360f
    } else if (!anticlockwise && sweepDegrees < 0) {
        sweepDegrees += 360f
    }

    if (rotation != 0.0) {
        // Apply rotation around the center
        val rotationDegrees = Math.toDegrees(rotation).toFloat()
        val matrix = Matrix()
        matrix.setRotate(rotationDegrees, x.toFloat(), y.toFloat())

        // Create a temporary path with the arc
        val tempPath = Path()
        tempPath.addArc(oval, startDegrees, sweepDegrees)

        // Transform and add to current path
        tempPath.transform(matrix)
        currentPath.addPath(tempPath)
    } else {
        currentPath.addArc(oval, startDegrees, sweepDegrees)
    }
}

// ============================================================================
// Hit Testing
// ============================================================================

public actual fun DrawingContext2D.isPointInPath(x: Double, y: Double): Boolean {
    return isPointInPath(x, y, FillRule.nonzero)
}

public actual fun DrawingContext2D.isPointInPath(x: Double, y: Double, fillRule: FillRule): Boolean {
    // Save the original fill type
    val originalFillType = currentPath.fillType

    // Set the fill type based on the fill rule
    currentPath.fillType = when (fillRule) {
        FillRule.nonzero -> Path.FillType.WINDING
        FillRule.evenodd -> Path.FillType.EVEN_ODD
    }

    // Compute the bounds of the path
    val bounds = RectF()
    currentPath.computeBounds(bounds, true)

    // Create a region from the path
    val region = Region()
    val clipBounds = Region(
        bounds.left.toInt(),
        bounds.top.toInt(),
        bounds.right.toInt() + 1,
        bounds.bottom.toInt() + 1
    )
    region.setPath(currentPath, clipBounds)

    // Check if the point is in the region
    val result = region.contains(x.toInt(), y.toInt())

    // Restore the original fill type
    currentPath.fillType = originalFillType

    return result
}

// ============================================================================
// Shadows
// ============================================================================

private fun DrawingContext2D.applyShadowToPaints() {
    if (this is DrawingContext2DImpl) {
        if (_shadowBlur > 0 && _shadowColor.alpha > 0) {
            val shadowColorInt = _shadowColor.toInt()
            fillPaintObj.setShadowLayer(
                _shadowBlur.toFloat(),
                _shadowOffsetX.toFloat(),
                _shadowOffsetY.toFloat(),
                shadowColorInt
            )
            strokePaintObj.setShadowLayer(
                _shadowBlur.toFloat(),
                _shadowOffsetX.toFloat(),
                _shadowOffsetY.toFloat(),
                shadowColorInt
            )
        } else {
            fillPaintObj.clearShadowLayer()
            strokePaintObj.clearShadowLayer()
        }
    }
}

public actual var DrawingContext2D.shadowBlur: Double
    get() = if (this is DrawingContext2DImpl) _shadowBlur else 0.0
    set(value) {
        if (this is DrawingContext2DImpl) {
            _shadowBlur = value.coerceAtLeast(0.0)
            applyShadowToPaints()
        }
    }

public actual var DrawingContext2D.shadowColorValue: Color
    get() = if (this is DrawingContext2DImpl) _shadowColor else Color.transparent
    set(value) {
        if (this is DrawingContext2DImpl) {
            _shadowColor = value
            applyShadowToPaints()
        }
    }

public actual var DrawingContext2D.shadowOffsetX: Double
    get() = if (this is DrawingContext2DImpl) _shadowOffsetX else 0.0
    set(value) {
        if (this is DrawingContext2DImpl) {
            _shadowOffsetX = value
            applyShadowToPaints()
        }
    }

public actual var DrawingContext2D.shadowOffsetY: Double
    get() = if (this is DrawingContext2DImpl) _shadowOffsetY else 0.0
    set(value) {
        if (this is DrawingContext2DImpl) {
            _shadowOffsetY = value
            applyShadowToPaints()
        }
    }

// ============================================================================
// Transform
// ============================================================================

public actual fun DrawingContext2D.getTransform(): TransformMatrix {
    if (this is DrawingContext2DImpl) {
        val values = FloatArray(9)
        transformMatrix.getValues(values)
        // Android Matrix format (row-major):
        // | MSCALE_X  MSKEW_X   MTRANS_X |   indices: 0, 1, 2
        // | MSKEW_Y   MSCALE_Y  MTRANS_Y |   indices: 3, 4, 5
        // | MPERSP_0  MPERSP_1  MPERSP_2 |   indices: 6, 7, 8
        // TransformMatrix format:
        // | a  c  e |
        // | b  d  f |
        // | 0  0  1 |
        return TransformMatrix(
            a = values[0].toDouble(),
            b = values[3].toDouble(),
            c = values[1].toDouble(),
            d = values[4].toDouble(),
            e = values[2].toDouble(),
            f = values[5].toDouble()
        )
    }
    return TransformMatrix.identity
}

public actual fun DrawingContext2D.setTransform(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double) {
    if (this is DrawingContext2DImpl) {
        // Reset the canvas matrix to identity first
        // We need to get the current matrix and invert it to reset
        val currentMatrix = Matrix()
        @Suppress("DEPRECATION")
        canvas.getMatrix(currentMatrix)
        val inverse = Matrix()
        currentMatrix.invert(inverse)
        canvas.concat(inverse)

        // Now set the new transform
        val newMatrix = Matrix().apply {
            setValues(floatArrayOf(
                a.toFloat(), c.toFloat(), e.toFloat(),
                b.toFloat(), d.toFloat(), f.toFloat(),
                0f, 0f, 1f
            ))
        }
        canvas.concat(newMatrix)

        // Update tracked matrix
        transformMatrix.set(newMatrix)
    }
}