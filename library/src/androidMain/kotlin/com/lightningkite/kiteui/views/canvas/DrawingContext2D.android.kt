package com.lightningkite.kiteui.views.canvas

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.lightningkite.kiteui.models.Angle
import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.FontAndStyle
import com.lightningkite.kiteui.models.turns
import com.lightningkite.kiteui.views.Path.DrawingResources
import com.lightningkite.kiteui.views.direct.colorInt


abstract class DrawingView : View {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
}
@Suppress("ACTUAL_WITHOUT_EXPECT")
actual abstract class DrawingContext2D(val canvas: Canvas) {
    val currentPath = Path()
    val clearPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.TRANSPARENT
        setXfermode(PorterDuffXfermode(PorterDuff.Mode.DST_OUT))
    }
    var fillPaintObj = android.graphics.Paint().apply { style = android.graphics.Paint.Style.FILL }
    var strokePaintObj = android.graphics.Paint().apply { style = android.graphics.Paint.Style.STROKE }
    val drawingResource = DrawingResources()
    actual abstract fun save()
    actual abstract fun restore()
    actual abstract fun scale(x: Double, y: Double)
    actual abstract fun rotate(angle: Double)
    actual abstract fun translate(x: Double, y: Double)
    actual abstract fun transform(
        a: Double,
        b: Double,
        c: Double,
        d: Double,
        e: Double,
        f: Double
    )

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
    abstract fun setLineDash(segments: Array<Double>)
    abstract fun getLineDash(): Array<Double>
    actual abstract fun closePath()
    actual abstract fun moveTo(x: Double, y: Double)
    actual abstract fun lineTo(x: Double, y: Double)
    actual abstract fun quadraticCurveTo(cpx: Double, cpy: Double, x: Double, y: Double)
    actual abstract fun bezierCurveTo(
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

    actual abstract fun rect(x: Double, y: Double, w: Double, h: Double)

}

class DrawingContext2DImpl(canvas: Canvas): DrawingContext2D(canvas) {
    override fun save() { canvas.save() }
    override fun restore() { canvas.restore() }
    override fun scale(x: Double, y: Double) { canvas.scale(x.toFloat(), y.toFloat()) }
    override fun rotate(angle: Double) { canvas.rotate(angle.toFloat()) }
    override fun translate(x: Double, y: Double) { canvas.translate(x.toFloat(), y.toFloat()) }
    override fun transform(
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
    override var globalCompositeOperation: String
        get() = TODO()
        set(value) {}
    override var imageSmoothingEnabled: Boolean
        get() = TODO()
        set(value) {}
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
    override var lineDashOffset: Double
        get() = TODO()
        set(value) { }
    override fun setLineDash(segments: Array<Double>) {
        if(segments.isEmpty()) strokePaintObj.pathEffect = null
        else strokePaintObj.pathEffect = DashPathEffect(segments.map { it.toFloat() }.toFloatArray(), 0f)
    }
    override fun getLineDash(): Array<Double> {
        TODO()
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

actual fun DrawingContext2D.fill() {
    currentPath.fillType = Path.FillType.WINDING
    canvas.drawPath(currentPath, fillPaintObj)
}
actual fun DrawingContext2D.fillEvenOdd()  {
    currentPath.fillType = Path.FillType.EVEN_ODD
    canvas.drawPath(currentPath, fillPaintObj)
}
actual var DrawingContext2D.strokePaint: com.lightningkite.kiteui.models.Paint
    get() = Color.fromInt(fillPaintObj.color)
    set(value) {
        strokePaintObj.color = value.colorInt()
    }
actual var DrawingContext2D.fillPaint: com.lightningkite.kiteui.models.Paint
    get() = Color.fromInt(fillPaintObj.color)
    set(value) {
        fillPaintObj.color = value.colorInt()
    }
actual val DrawingContext2D.width: Double
    get() = canvas.width.toDouble()
actual val DrawingContext2D.height: Double
    get() = canvas.height.toDouble()

actual fun DrawingContext2D.appendArc(
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

actual fun DrawingContext2D.drawText(
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

actual fun DrawingContext2D.drawOutlinedText(
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

actual fun DrawingContext2D.font(
    size: Double,
    value: FontAndStyle
) {
    fillPaintObj.setTypeface(value.font)
    fillPaintObj.textSize = size.toFloat()
}

actual fun DrawingContext2D.textAlign(alignment: TextAlign) {
    fillPaintObj.textAlign = when(alignment) {
        TextAlign.start -> android.graphics.Paint.Align.LEFT  // TODO: locales
        TextAlign.end -> android.graphics.Paint.Align.RIGHT  // TODO: locales
        TextAlign.left -> android.graphics.Paint.Align.LEFT
        TextAlign.right -> android.graphics.Paint.Align.RIGHT
        TextAlign.center -> android.graphics.Paint.Align.CENTER
    }
}

actual fun DrawingContext2D.clear() {
    canvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.DST_OUT)
}