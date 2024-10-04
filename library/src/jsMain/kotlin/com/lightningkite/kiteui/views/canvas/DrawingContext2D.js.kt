package com.lightningkite.kiteui.views.canvas

import com.lightningkite.kiteui.models.*
import org.w3c.dom.CanvasFillRule
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.CanvasTextAlign

actual typealias DrawingContext2D = CanvasRenderingContext2D
//actual typealias TextAlign = CanvasTextAlign

actual fun DrawingContext2D.appendArc(x: Double, y: Double, radius: Double, startAngle: Angle, endAngle: Angle, anticlockwise: Boolean) = arc(x, y, radius, startAngle.radians.toDouble(), endAngle.radians.toDouble(), anticlockwise)
actual fun DrawingContext2D.drawOutlinedText(text: String, x: Double, y: Double):Unit = strokeText(text, x, y)
actual fun DrawingContext2D.drawText(text: String, x: Double, y: Double):Unit = fillText(text, x, y)
actual fun DrawingContext2D.font(size: Double, value: FontAndStyle) {
    font = "${value.weight} ${if(value.italic) "italic " else ""}${size}px ${value.font.cssFontFamilyName}"
}
actual fun DrawingContext2D.textAlign(alignment: TextAlign){
    textAlign = alignment.toString().asDynamic().unsafeCast<CanvasTextAlign>()
}
actual fun DrawingContext2D.fill() = fill("nonzero".asDynamic().unsafeCast<CanvasFillRule>())
actual fun DrawingContext2D.fillEvenOdd() = fill("evenodd".asDynamic().unsafeCast<CanvasFillRule>())

actual var DrawingContext2D.strokePaint: Paint
    get() = when(val it = strokeStyle) {
        is String -> Color.fromHexString(it)
        else -> Color.black
    }
    set(value) {
        when(value) {
            is Color -> strokeStyle = value.toWeb()
            is FadingColor -> TODO()
            is LinearGradient -> TODO()
            is RadialGradient -> TODO()
        }
    }
actual var DrawingContext2D.fillPaint: Paint
    get() = when(val it = fillStyle) {
        is String -> Color.fromHexString(it)
        else -> Color.black
    }
    set(value) {
        when(value) {
            is Color -> fillStyle = value.toWeb()
            is FadingColor -> TODO()
            is LinearGradient -> TODO()
            is RadialGradient -> TODO()
        }
    }
actual val DrawingContext2D.width: Double get() = canvas.width.toDouble()
actual val DrawingContext2D.height: Double get() = canvas.height.toDouble()

actual fun DrawingContext2D.clear() {
    clearRect(0.0, 0.0, width, height)
}