package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.ImageVector
import com.lightningkite.kiteui.models.LinearGradient
import com.lightningkite.kiteui.models.RadialGradient
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.objc.toObjcId
import com.lightningkite.kiteui.views.toUiColor
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.*
import platform.Foundation.NSNumber
import platform.Foundation.numberWithFloat
import platform.QuartzCore.*
import platform.UIKit.*
import kotlin.math.*

fun <E> MutableList<E>.unshift(): E {
    return removeAt(0)
}

val pathLetters = charArrayOf(
    'M',
    'L',
    'Z',
    'H',
    'V',
    'Q',
    'T',
    'C',
    'S',
    'A'
)
val spaceOrComma = Regex("[ ,]+")

@OptIn(ExperimentalForeignApi::class) inline fun CGMutablePathRef.move(x: CGFloat, y: CGFloat) = CGPathMoveToPoint(this, null, x, y)
@OptIn(ExperimentalForeignApi::class) inline fun CGMutablePathRef.addLine(x: CGFloat, y: CGFloat) = CGPathAddLineToPoint(this, null, x, y)
@OptIn(ExperimentalForeignApi::class) inline fun CGMutablePathRef.addQuadCurve(cx: CGFloat, cy: CGFloat, x: CGFloat, y: CGFloat) = CGPathAddQuadCurveToPoint(this, null, cx, cy, x, y)
@OptIn(ExperimentalForeignApi::class) inline fun CGMutablePathRef.addCurve(c1x: CGFloat, c1y: CGFloat, c2x: CGFloat, c2y: CGFloat, x: CGFloat, y: CGFloat) = CGPathAddCurveToPoint(this, null, c1x, c1y, c2x, c2y, x, y)
@OptIn(ExperimentalForeignApi::class) fun CGMutablePathRef.arcTo(lastX: CGFloat, lastY: CGFloat, x: CGFloat, y: CGFloat, radiusX: CGFloat, radiusY: CGFloat, rotation: CGFloat, largeArcFlag: Boolean, sweepFlag: Boolean) {
//    println("x: $x, y: $y, radiusX: $radiusX, radiusY: $radiusY, theta: $rotation, largeArcFlag: $largeArcFlag, sweepFlag: $sweepFlag")
    if (radiusX == 0.0 || radiusY == 0.0) {
        addLine(x, y)
        return
    }


    val rotationRadians = (rotation * PI/180).rem(PI * 2)
    val cosRotation = cos(rotationRadians)
    val sinRotation = sin(rotationRadians)

    // Calculate arc center
    val p1x = cosRotation * (lastX - x) / 2 + sinRotation * (lastY - y) / 2
    val p1y = -sinRotation * (lastX - x) / 2 + cosRotation * (lastY - y) / 2

    val delta = (p1x * p1x) / (radiusX * radiusX) + (p1y * p1y) / (radiusY * radiusY)
    val transformedRadiusX = if(delta <= 1.0) radiusX else radiusX * sqrt(delta)
    val transformedRadiusY = if(delta <= 1.0) radiusY else radiusY * sqrt(delta)
    val numerator = (transformedRadiusX * transformedRadiusX) * (transformedRadiusY * transformedRadiusY) - (transformedRadiusX * transformedRadiusX) * (p1y * p1y) - (transformedRadiusY * transformedRadiusY) * (p1x * p1x)
    val denom = (transformedRadiusX * transformedRadiusX) * (p1y * p1y) + (transformedRadiusY * transformedRadiusY) * (p1x * p1x)
    val lhs = if(denom == 0.0) 0.0 else (if(largeArcFlag == sweepFlag) -1 else 1) * sqrt(max(numerator, 0.0) / denom)
    val cxp = lhs * transformedRadiusX * p1y / transformedRadiusY
    val cyp = lhs * -transformedRadiusY * p1x / transformedRadiusX
    val cx = cosRotation * cxp - sinRotation * cyp + (lastX + x) / 2
    val cy = sinRotation * cxp + cosRotation * cyp + (lastY + y) / 2

    // Transform ellipse into unit circle and calculate angles
    var transform = CGAffineTransformMakeScale(1/transformedRadiusX, 1/transformedRadiusY)
    transform = CGAffineTransformRotate(transform, -rotationRadians)
    transform = CGAffineTransformTranslate(transform, -cx, -cy)


    val startAngle = angle(1.0, 0.0, (p1x - cxp) / transformedRadiusX, (p1y - cyp) / transformedRadiusY)
    var deltaAngle = angle((p1x - cxp) / transformedRadiusX, (p1y - cyp) / transformedRadiusY, (-p1x - cxp) / transformedRadiusX, (-p1y - cyp) / transformedRadiusY)
    if(sweepFlag) {
        if(deltaAngle < 0f) deltaAngle += PI * 2
    } else {
        if(deltaAngle > 0f) deltaAngle -= PI * 2
    }

    // Draw
    val reversedTransform = CGAffineTransformInvert(transform)
    CGPathAddRelativeArc(
        path = this,
        x = 0.0,
        y = 0.0,
        radius = 1.0,
        startAngle = startAngle,
        delta = deltaAngle,
        matrix = reversedTransform
    )
}
fun angle(x1: CGFloat, y1: CGFloat, x2: CGFloat, y2: CGFloat): CGFloat {
    return (atan2(x1, y1) - atan2(x2, y2)) % (PI * 2)
}
@OptIn(ExperimentalForeignApi::class) inline fun CGMutablePathRef.close() = CGPathCloseSubpath(this)

@OptIn(ExperimentalForeignApi::class)
private fun CGMutablePathRef.render(pathData: String, translateX: CGFloat = 0.0, translateY: CGFloat = 0.0, scaleX: CGFloat = 1.0, scaleY: CGFloat = 1.0) {
    // TODO: Handle StrokeLineCap
    fun CGFloat.posX() = ((this + translateX) * scaleX)
    fun CGFloat.posY() = ((this + translateY) * scaleY)
    fun CGFloat.sizeX() = (this * scaleX)
    fun CGFloat.sizeY() = (this * scaleY)
    var firstSet = false
    var startX: Double = 0.0
    var startY: Double = 0.0
    var referenceX: Double = 0.0
    var referenceY: Double = 0.0
    var previousC2X: Double = 0.0
    var previousC2Y: Double = 0.0
    var stringIndex = pathData.indexOfAny(pathLetters, 0, true)
    while (true) {
        var nextLetterIndex = pathData.indexOfAny(pathLetters, stringIndex + 1, true)
        if (nextLetterIndex == -1) nextLetterIndex = pathData.length

        val rawInstruction: Char = pathData[stringIndex]
        val arguments = ArrayList<Double>()
        val currentNumber = StringBuilder()
        val substring = pathData.substring(stringIndex + 1, nextLetterIndex)
        var sawE = false
        for (c in substring) {
            when (c) {
                ' ', ',' -> {
                    if (currentNumber.length > 0) {
                        arguments.add(currentNumber.toString().toDouble())
                        currentNumber.setLength(0)
                    }
                }
                '-' -> {
                    if (currentNumber.length > 0 && !sawE) {
                        arguments.add(currentNumber.toString().toDouble())
                        currentNumber.setLength(0)
                    }
                    currentNumber.append('-')
                }
                in '0'..'9' -> {
                    currentNumber.append(c)
                }
                'e' -> {
                    currentNumber.append(c)
                    sawE = true
                    continue
                }
                '.' -> {
                    if (currentNumber.contains('.')) {
                        if (currentNumber.length > 0) {
                            arguments.add(currentNumber.toString().toDouble())
                            currentNumber.setLength(0)
                        }
                        currentNumber.append(c)
                    } else {
                        currentNumber.append(c)
                    }
                }
            }
            sawE = false
        }
        if (currentNumber.length > 0) {
            arguments.add(currentNumber.toString().toDouble())
        }

        var instruction = rawInstruction.toLowerCase()
        val isAbsolute: Boolean = rawInstruction.isUpperCase()
        inline fun offsetX(): Double = if (isAbsolute) 0.0 else referenceX
        inline fun offsetY(): Double = if (isAbsolute) 0.0 else referenceY
        var updateReference = true
        try {
            do {
                when (instruction) {
                    'm' -> {
                        val destX = arguments.unshift() + offsetX()
                        val destY = arguments.unshift() + offsetY()
                        referenceX = destX
                        previousC2X = referenceX
                        startX = referenceX
                        referenceY = destY
                        previousC2Y = referenceY
                        startY = referenceY
                        move(x = destX.posX(), y = destY.posY())
                        instruction = 'l'
                    }
                    'l' -> {
                        val destX = arguments.unshift() + offsetX()
                        val destY = arguments.unshift() + offsetY()
                        referenceX = destX
                        previousC2X = referenceX
                        referenceY = destY
                        previousC2Y = referenceY
                        this.addLine(x = destX.posX(), y = destY.posY())
                    }
                    'z' -> {
                        this.close()
                        referenceX = startX
                        previousC2X = referenceX
                        referenceY = startY
                        previousC2Y = referenceY
                        firstSet = false
                    }
                    'h' -> {
                        updateReference = false
                        referenceX = arguments.unshift() + offsetX()
                        previousC2X = referenceX
                        this.addLine(x = referenceX.posX(), y = referenceY.posY())
                    }
                    'v' -> {
                        updateReference = false
                        referenceY = arguments.unshift() + offsetY()
                        previousC2Y = referenceY
                        this.addLine(x = referenceX.posX(), y = referenceY.posY())
                    }
                    'q' -> {
                        val controlX = arguments.unshift() + offsetX()
                        val controlY = arguments.unshift() + offsetY()
                        val destX = arguments.unshift() + offsetX()
                        val destY = arguments.unshift() + offsetY()
                        previousC2X = controlX
                        previousC2Y = controlY
                        referenceX = destX
                        referenceY = destY
                        this.addQuadCurve(x = destX.posX(), y = destY.posY(), cx = controlX.posX(), cy = controlY.posY())
                    }
                    't' -> {
                        val destX = arguments.unshift() + offsetX()
                        val destY = arguments.unshift() + offsetY()
                        val controlX = referenceX + (referenceX - previousC2X)
                        val controlY = referenceY + (referenceY - previousC2Y)
                        referenceX = destX
                        referenceY = destY
                        previousC2X = controlX
                        previousC2Y = controlY
                        this.addQuadCurve(x = destX.posX(), y = destY.posY(), cx = controlX.posX(), cy = controlY.posY())
                    }
                    'c' -> {
                        val control1X = arguments.unshift() + offsetX()
                        val control1Y = arguments.unshift() + offsetY()
                        val control2X = arguments.unshift() + offsetX()
                        val control2Y = arguments.unshift() + offsetY()
                        val destX = arguments.unshift() + offsetX()
                        val destY = arguments.unshift() + offsetY()
                        previousC2X = control2X
                        previousC2Y = control2Y
                        referenceX = destX
                        referenceY = destY
                        this.addCurve(x = destX.posX(), y = destY.posY(), c1x = control1X.posX(), c1y = control1Y.posY(), c2x = control2X.posX(), c2y = control2Y.posY())
                    }
                    's' -> {
                        val control2X = arguments.unshift() + offsetX()
                        val control2Y = arguments.unshift() + offsetY()
                        val destX = arguments.unshift() + offsetX()
                        val destY = arguments.unshift() + offsetY()
                        val c1x = referenceX + (referenceX - previousC2X)
                        val c1y = referenceY + (referenceY - previousC2Y)
                        previousC2X = control2X
                        previousC2Y = control2Y
                        referenceX = destX
                        referenceY = destY
                        this.addCurve(x = destX.posX(), y = destY.posY(), c1x = c1x.posX(), c1y = c1y.posY(), c2x = control2X.posX(), c2y = control2Y.posY())
                    }
                    'a' -> {
                        val lastX = referenceX
                        val lastY = referenceY
                        val radiusX = arguments.unshift()
                        val radiusY = arguments.unshift()
                        val xAxisRotation = arguments.unshift()
                        val largeArcFlag = arguments.unshift()
                        val sweepFlag = arguments.unshift()
                        val destX = arguments.unshift() + offsetX()
                        val destY = arguments.unshift() + offsetY()
                        referenceX = destX
                        previousC2X = referenceX
                        referenceY = destY
                        previousC2Y = referenceY
                        this.arcTo(lastX = lastX.posX(), lastY = lastY.posY(), radiusX = radiusX.sizeX(), radiusY = radiusY.sizeY(), x = destX.posX(), y = destY.posY(), rotation = xAxisRotation, largeArcFlag = largeArcFlag > 0.5, sweepFlag = sweepFlag > 0.5)
                    }
                    else -> throw IllegalStateException("Non-legal command ${instruction}")
                }
                if(!firstSet){
                    firstSet = true
                    startX = referenceX
                    startY = referenceY
                }
            } while(arguments.isNotEmpty())
        } catch(e:Exception){
            throw Exception("Error at ${stringIndex}: '${substring}'", e)
        }

        stringIndex = nextLetterIndex
        if (nextLetterIndex == pathData.length) break
    }
}

class CAShapeLayerScaling: CAShapeLayer() {

}

@OptIn(ExperimentalForeignApi::class)
fun ImageVector.caLayer(): CALayer {
    val layer = CALayer()
    layer.bounds = CGRectMake(0.0, 0.0, width.px, height.px)
    val scaleX = this.width.px / this.viewBoxWidth
    val scaleY = this.height.px / this.viewBoxHeight
    val translateX = -this.viewBoxMinX.toDouble()
    val translateY = -this.viewBoxMinY.toDouble()
    for(path in paths) {
        val p = CGPathCreateMutable()!!
        p.render(path.path, translateX, translateY, scaleX, scaleY)
        layer.addSublayer(when(val f = path.fillColor) {
            is LinearGradient -> CAGradientLayer.layer().apply {
                frame = layer.bounds
                this.mask = CAShapeLayer().apply {
                    frame = layer.bounds
                    this.path = p
                }
                this.type = kCAGradientLayerAxial
                this.locations = f.stops.map {
                    NSNumber.numberWithFloat(it.ratio)
                }
                this.colors = f.stops.map { it.color.toUiColor().CGColor!!.toObjcId() }
                this.startPoint = CGPointMake(-f.angle.cos() * .5 + .5, -f.angle.sin() * .5 + .5)
                this.endPoint = CGPointMake(f.angle.cos() * .5 + .5, f.angle.sin() * .5 + .5)
            }
            is RadialGradient -> CAGradientLayer.layer().apply {
                frame = layer.bounds
                this.mask = CAShapeLayer().apply {
                    frame = layer.bounds
                    this.path = p
                }
                this.type = kCAGradientLayerRadial
                this.locations = f.stops.map {
                    NSNumber.numberWithFloat(it.ratio)
                }
                this.colors = f.stops.map { it.color.toUiColor().CGColor!!.toObjcId() }
                this.startPoint = CGPointMake(0.5, 0.5)
                this.endPoint = CGPointMake(0.0, 0.0)
            }
            is Color -> CAShapeLayer().apply {
                frame = layer.bounds
                this.path = p
                this.fillColor = f.toUiColor().CGColor
                this.strokeColor = path.strokeColor?.toUiColor()?.CGColor ?: UIColor.blackColor.CGColor
                this.lineWidth = path.strokeWidth?.times(scaleX) ?: 0.0
            }
            null -> CAShapeLayer().apply {
                frame = layer.bounds
                this.path = p
                this.fillColor = UIColor.clearColor.CGColor
                this.strokeColor = path.strokeColor?.toUiColor()?.CGColor ?: UIColor.blackColor.CGColor
                this.lineWidth = path.strokeWidth?.times(scaleX) ?: 0.0
            }
        })
    }
    return layer
}

@OptIn(ExperimentalForeignApi::class)
fun ImageVector.render(): UIImage {
    UIGraphicsBeginImageContext(CGSizeMake(width.px, height.px))
    try {
        with(UIGraphicsGetCurrentContext()) {
            caLayer().renderInContext(this)
        }
        return UIGraphicsGetImageFromCurrentImageContext()!!
    } finally {
        UIGraphicsEndImageContext()
    }
}