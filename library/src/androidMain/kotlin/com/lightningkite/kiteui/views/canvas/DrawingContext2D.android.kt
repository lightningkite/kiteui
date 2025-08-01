package com.lightningkite.kiteui.views.canvas

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.lightningkite.kiteui.models.Angle
import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.FontAndStyle
import com.lightningkite.kiteui.models.Paint
import com.lightningkite.kiteui.models.turns
import com.lightningkite.kiteui.views.Path.DrawingResources
import com.lightningkite.kiteui.views.direct.colorInt


public abstract class DrawingView : View {
    public constructor(context: Context?) : super(context)
    public constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    public constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
}
@Suppress("ACTUAL_WITHOUT_EXPECT")
public actual abstract class DrawingContext2D(public val canvas: Canvas) {
    public val currentPath: Path = Path()
    public val clearPaint: android.graphics.Paint = Paint().apply {
        color = android.graphics.Color.TRANSPARENT
        setXfermode(PorterDuffXfermode(PorterDuff.Mode.DST_OUT))
    }
    public var fillPaintObj: android.graphics.Paint = Paint().apply { style = android.graphics.Paint.Style.FILL }
    public var strokePaintObj: android.graphics.Paint = Paint().apply { style = android.graphics.Paint.Style.STROKE }
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

//    public actual abstract fun arcTo(
//        x1: Double,
//        y1: Double,
//        x2: Double,
//        y2: Double,
//        radius: Double
//    )
//
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

}

public class DrawingContext2DImpl(canvas: Canvas): DrawingContext2D(canvas) {
    public override fun save() { canvas.save() }
    public override fun restore() { canvas.restore() }
    public override fun scale(x: Double, y: Double) { canvas.scale(x.toFloat(), y.toFloat()) }
    public override fun rotate(angle: Double) { canvas.rotate(angle.toFloat()) }
    public override fun translate(x: Double, y: Double) { canvas.translate(x.toFloat(), y.toFloat()) }
    public override fun transform(
        a: Double,
        b: Double,
        c: Double,
        d: Double,
        e: Double,
        f: Double,
    ) {
        canvas.concat(Matrix().apply {
            setValues(floatArrayOf(
                a.toFloat(),
                b.toFloat(),
                c.toFloat(),
                d.toFloat(),
                e.toFloat(),
                f.toFloat(),
                0f,
                0f,
                1f
            ))
        })
    }
    public override var globalCompositeOperation: String
        get() = TODO()
        set(value) {}
    public override var imageSmoothingEnabled: Boolean
        get() = TODO()
        set(value) {}
    public override fun clearRect(x: Double, y: Double, w: Double, h: Double) {
        canvas.drawRect(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat(), clearPaint)
    }
    public override fun fillRect(x: Double, y: Double, w: Double, h: Double) {
        canvas.drawRect(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat(), fillPaintObj)
    }
    public override fun strokeRect(x: Double, y: Double, w: Double, h: Double){
        canvas.drawRect(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat(), strokePaintObj)
    }
    public override fun beginPath() {
        currentPath.reset()
    }
    public override fun stroke() {
        canvas.drawPath(currentPath, strokePaintObj)
    }
    public override var lineWidth: Double
        get() = strokePaintObj.strokeWidth.toDouble()
        set(value) { strokePaintObj.strokeWidth = value.toFloat() }
    public override var miterLimit: Double
        get() = strokePaintObj.strokeMiter.toDouble()
        set(value) { strokePaintObj.strokeMiter = value.toFloat() }
    public override var lineDashOffset: Double
        get() = TODO()
        set(value) { }
    public override fun setLineDash(segments: Array<Double>) {
        if(segments.isEmpty()) strokePaintObj.pathEffect = null
        else strokePaintObj.pathEffect = DashPathEffect(segments.map { it.toFloat() }.toFloatArray(), 0f)
    }
    public override fun getLineDash(): Array<Double> {
        TODO()
    }
    public override fun closePath() {
        currentPath.close()
    }
    public override fun moveTo(x: Double, y: Double) {
        currentPath.moveTo(x.toFloat(), y.toFloat())
    }
    public override fun lineTo(x: Double, y: Double) {
        currentPath.lineTo(x.toFloat(), y.toFloat())
    }
    public override fun quadraticCurveTo(cpx: Double, cpy: Double, x: Double, y: Double) {
        currentPath.quadTo(cpx.toFloat(), cpy.toFloat(), x.toFloat(), y.toFloat())
    }
    public override fun bezierCurveTo(
        cp1x: Double,
        cp1y: Double,
        cp2x: Double,
        cp2y: Double,
        x: Double,
        y: Double,
    ) {
        currentPath.cubicTo(cp1x.toFloat(), cp1y.toFloat(),cp2x.toFloat(), cp2y.toFloat(), x.toFloat(), y.toFloat())
    }

    public override fun rect(x: Double, y: Double, w: Double, h: Double) {
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
public actual var DrawingContext2D.strokePaint: Paint
    get() = Color.fromInt(fillPaintObj.color)
    set(value) {
        strokePaintObj.color = value.colorInt()
    }
public actual var DrawingContext2D.fillPaint: Paint
    get() = Color.fromInt(fillPaintObj.color)
    set(value) {
        fillPaintObj.color = value.colorInt()
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
    val rel = startAngle angleTo endAngle
    if (anticlockwise) {
        if (rel.degrees > 0) {
            currentPath.arcTo(
                (x - radius).toFloat(),
                (y - radius).toFloat(),
                (x + radius).toFloat(),
                (y + radius).toFloat(),
                startAngle.degrees,
                (rel - 1.turns).degrees,
                false
            )
        } else {
            currentPath.arcTo(
                (x - radius).toFloat(),
                (y - radius).toFloat(),
                (x + radius).toFloat(),
                (y + radius).toFloat(),
                startAngle.degrees,
                rel.degrees,
                false
            )
        }
    } else {
        if (rel.degrees > 0) {
            currentPath.arcTo(
                (x - radius).toFloat(),
                (y - radius).toFloat(),
                (x + radius).toFloat(),
                (y + radius).toFloat(),
                startAngle.degrees,
                rel.degrees,
                false
            )
        } else {
            currentPath.arcTo(
                (x - radius).toFloat(),
                (y - radius).toFloat(),
                (x + radius).toFloat(),
                (y + radius).toFloat(),
                startAngle.degrees,
                (rel + 1.turns).degrees,
                false
            )
        }
    }
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
    fillPaintObj.setTypeface(value.font)
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