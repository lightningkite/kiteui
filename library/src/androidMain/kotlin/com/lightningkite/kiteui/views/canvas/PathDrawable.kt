package com.lightningkite.kiteui.views.Path

import android.graphics.*
import android.graphics.Paint
import android.graphics.drawable.Drawable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.LinearGradient
import com.lightningkite.kiteui.models.RadialGradient
import com.lightningkite.kiteui.views.AndroidAppContext
import com.lightningkite.kiteui.views.direct.colorInt
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

fun Paint.match(kiteui: com.lightningkite.kiteui.models.Paint, parentOffsetX: Float, parentWidth: Float, parentOffsetY: Float, parentHeight: Float) {
    when (val it = kiteui) {
        is Color -> this.color = it.colorInt()
        is LinearGradient -> {
            val smallest = min(parentWidth, parentHeight) / 2
            val x0 = parentOffsetX + parentWidth / 2 - it.angle.cos() * smallest
            val x1 = parentOffsetX + parentWidth / 2 + it.angle.cos() * smallest
            val y0 = parentOffsetY + parentWidth / 2 - it.angle.sin() * smallest
            val y1 = parentOffsetY + parentWidth / 2 + it.angle.sin() * smallest
            this.shader = android.graphics.LinearGradient(
                x0,
                y0,
                x1,
                y1,
                it.stops.map { it.color.colorInt() }.toIntArray(),
                it.stops.map { it.ratio }.toFloatArray(),
                Shader.TileMode.CLAMP
            )
        }

        is RadialGradient -> {
            val smallest = min(parentWidth, parentHeight) / 2
            this.shader = android.graphics.RadialGradient(
                parentOffsetX + parentWidth / 2,
                parentOffsetY + parentHeight / 2,
                smallest,
                it.stops.map { it.color.colorInt() }.toIntArray(),
                it.stops.map { it.ratio }.toFloatArray(),
                Shader.TileMode.CLAMP
            )
        }
    }
}

class PathDrawable(val vector: ImageVector) : Drawable() {
    val drawingResources = DrawingResources()

    class PathInfo(
        val path: Path,
        val outline: Paint? = null,
        val fill: Paint? = null,
    )

    val paths = run {
        val scaleX = vector.width.value / vector.viewBoxWidth
        val scaleY = vector.height.value / vector.viewBoxHeight
        val translateX = -vector.viewBoxMinX.toFloat()
        val translateY = -vector.viewBoxMinY.toFloat()

        vector.paths.map {
            PathInfo(
                path = Path().apply { render(drawingResources, it.path, translateX, translateY, scaleX, scaleY) },
                outline = it.strokeColor?.let { color ->
                    Paint().apply {
                        match(
                            color,
                            translateX,
                            vector.width.value,
                            translateY,
                            vector.height.value,
                        )
                        style = Paint.Style.STROKE
                        strokeWidth = it.strokeWidth?.times(scaleX)?.div(AndroidAppContext.density) ?: 0f
                        it.strokeCap?.let {
                            strokeCap = when (it) {
                                Icon.StrokeLineCap.Square -> Paint.Cap.SQUARE
                                Icon.StrokeLineCap.Butt -> Paint.Cap.BUTT
                                Icon.StrokeLineCap.Round -> Paint.Cap.ROUND
                            }
                        }
                    }
                },
                fill = it.fillColor?.let { color ->
                    Paint().apply {
                        match(
                            color,
                            translateX,
                            vector.width.value,
                            translateY,
                            vector.height.value,
                        )
                        style = Paint.Style.FILL
                    }
                },
            )
        }
    }

    override fun draw(canvas: Canvas) {
        paths.forEach {
            it.fill?.let { fill -> canvas.drawPath(it.path, it.fill) }
            it.outline?.let { outline -> canvas.drawPath(it.path, it.outline) }
        }
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getIntrinsicHeight(): Int = vector.height.value.toInt()
    override fun getIntrinsicWidth(): Int = vector.width.value.toInt()
}

private fun Path.render(
    drawingResources: DrawingResources,
    pathData: String,
    translateX: Float = 0.0f,
    translateY: Float = 0.0f,
    scaleX: Float = 1.0f,
    scaleY: Float = 1.0f
) {
    fun Float.posX() = ((this + translateX) * scaleX)
    fun Float.posY() = ((this + translateY) * scaleY)
    fun Float.sizeX() = (this * scaleX)
    fun Float.sizeY() = (this * scaleY)
    var firstSet = false
    var startX: Float = 0.0f
    var startY: Float = 0.0f
    var referenceX: Float = 0.0f
    var referenceY: Float = 0.0f
    var previousC2X: Float = 0.0f
    var previousC2Y: Float = 0.0f
    var stringIndex = pathData.indexOfAny(pathLetters, 0, true)
    while (true) {
        var nextLetterIndex = pathData.indexOfAny(pathLetters, stringIndex + 1, true)
        if (nextLetterIndex == -1) nextLetterIndex = pathData.length

        val rawInstruction: Char = pathData[stringIndex]
        val arguments = ArrayList<Float>()
        val currentNumber = StringBuilder()
        val substring = pathData.substring(stringIndex + 1, nextLetterIndex)
        var sawE = false
        for (c in substring) {
            when (c) {
                ' ', ',' -> {
                    if (currentNumber.length > 0) {
                        arguments.add(currentNumber.toString().toFloat())
                        currentNumber.setLength(0)
                    }
                }

                '-' -> {
                    if (currentNumber.length > 0 && !sawE) {
                        arguments.add(currentNumber.toString().toFloat())
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
                            arguments.add(currentNumber.toString().toFloat())
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
            arguments.add(currentNumber.toString().toFloat())
        }

        var instruction = rawInstruction.toLowerCase()
        val isAbsolute: Boolean = rawInstruction.isUpperCase()
        fun offsetX(): Float = if (isAbsolute) 0.0f else referenceX
        fun offsetY(): Float = if (isAbsolute) 0.0f else referenceY
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
                        moveTo(destX.posX(), destY.posY())
                        instruction = 'l'
                    }

                    'l' -> {
                        val destX = arguments.unshift() + offsetX()
                        val destY = arguments.unshift() + offsetY()
                        referenceX = destX
                        previousC2X = referenceX
                        referenceY = destY
                        previousC2Y = referenceY
                        lineTo(destX.posX(), destY.posY())
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
                        lineTo(referenceX.posX(), referenceY.posY())
                    }

                    'v' -> {
                        updateReference = false
                        referenceY = arguments.unshift() + offsetY()
                        previousC2Y = referenceY
                        lineTo(referenceX.posX(), referenceY.posY())
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
                        quadTo(controlX.posX(), controlY.posY(), destX.posX(), destY.posY())
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
                        quadTo(controlX.posX(), controlY.posY(), destX.posX(), destY.posY())
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
                        this.cubicTo(
                            control1X.posX(),
                            control1Y.posY(),
                            control2X.posX(),
                            control2Y.posY(),
                            destX.posX(),
                            destY.posY()
                        )
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
                        this.cubicTo(
                            c1x.posX(),
                            c1y.posY(),
                            control2X.posX(),
                            control2Y.posY(),
                            destX.posX(),
                            destY.posY()
                        )
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
                        drawingResources.drawArc(
                            path = this,
                            lastX = lastX.posX(),
                            lastY = lastY.posY(),
                            radiusX = radiusX.sizeX(),
                            radiusY = radiusY.sizeY(),
                            x = destX.posX(),
                            y = destY.posY(),
                            theta = xAxisRotation,
                            largeArcFlag = largeArcFlag > 0.5,
                            sweepFlag = sweepFlag > 0.5
                        )
                    }

                    else -> throw IllegalStateException("Non-legal command ${instruction}")
                }
                if (!firstSet) {
                    firstSet = true
                    startX = referenceX
                    startY = referenceY
                }
            } while (arguments.isNotEmpty())
        } catch (e: Exception) {
            throw Exception("Error at ${stringIndex}: '${substring}'", e)
        }

        stringIndex = nextLetterIndex
        if (nextLetterIndex == pathData.length) break
    }
}

class DrawingResources() {
    val arcRectf = RectF()
    val arcMatrix: Matrix = Matrix()
    val arcMatrix2: Matrix = Matrix()
}

fun DrawingResources.drawArc(
    path: Path, lastX: Float, lastY: Float, x: Float, y: Float, radiusX: Float, radiusY: Float, theta: Float,
    largeArcFlag: Boolean, sweepFlag: Boolean
) {
//    println("x: $x, y: $y, radiusX: $radiusX, radiusY: $radiusY, theta: $theta, largeArcFlag: $largeArcFlag, sweepFlag: $sweepFlag")
    // Log.d("drawArc", "from (" + lastX + "," + lastY + ") to (" + x + ","+ y + ") r=(" + rx + "," + ry +
    // ") theta=" + theta + " flags="+ largeArc + "," + sweepArc);

    // http://www.w3.org/TR/SVG/implnote.html#ArcImplementationNotes
    if (radiusX == 0f || radiusY == 0f) {
        path.lineTo(x, y)
        return
    }
    if (x == lastX && y == lastY) {
        return  // nothing to draw
    }
    val thrad = theta * Math.PI.toFloat() / 180

    val sinRotation: Float = sin(thrad)
    val cosRotation: Float = cos(thrad)
    val xc = (lastX - x) / 2
    val yc = (lastY - y) / 2
    val p1x = cosRotation * xc + sinRotation * yc
    val p1y = -sinRotation * xc + cosRotation * yc

    val p1xSquared = p1x * p1x
    val p1ySquared = p1y * p1y
    var radiusXSquared = radiusX * radiusX
    var radiusYSquared = radiusY * radiusY
    val delta = (p1xSquared / radiusXSquared + p1ySquared / radiusYSquared)
    val deltaSqrt: Float = sqrt(delta)
    // limited precision
    val transformedRadiusX = if (delta < 1.001f) radiusX else radiusX * deltaSqrt
    val transformedRadiusY = if (delta < 1.001f) radiusY else radiusY * deltaSqrt
    val numerator =
        (transformedRadiusX * transformedRadiusX) * (transformedRadiusY * transformedRadiusY) - (transformedRadiusX * transformedRadiusX) * (p1y * p1y) - (transformedRadiusY * transformedRadiusY) * (p1x * p1x)
    val denom =
        (transformedRadiusX * transformedRadiusX) * (p1y * p1y) + (transformedRadiusY * transformedRadiusY) * (p1x * p1x)
    val lhs = if (denom == 0f) 0f else (if (largeArcFlag == sweepFlag) -1 else 1) * sqrt(max(numerator, 0f) / denom)

    val cxp = lhs * transformedRadiusX * p1y / transformedRadiusY
    val cyp = -lhs * transformedRadiusY * p1x / transformedRadiusX
    val cx = cosRotation * cxp - sinRotation * cyp + (lastX + x) / 2
    val cy = sinRotation * cxp + cosRotation * cyp + (lastY + y) / 2

    val startAngle: Float = angle(1f, 0f, (p1x - cxp) / transformedRadiusX, (p1y - cyp) / transformedRadiusY)
    var deltaAngle: Float = angle(
        (p1x - cxp) / transformedRadiusX,
        (p1y - cyp) / transformedRadiusY,
        (-p1x - cxp) / transformedRadiusX,
        (-p1y - cyp) / transformedRadiusY
    )
    if (sweepFlag) {
        if (deltaAngle < 0f) deltaAngle += 360f
    } else {
        if (deltaAngle > 0f) deltaAngle -= 360f
    }

    // draw
    if (theta % 360 == 0f) {
        // no rotate and translate need
        arcRectf.set(cx - transformedRadiusX, cy - transformedRadiusY, cx + transformedRadiusX, cy + transformedRadiusY)
        path.arcTo(arcRectf, startAngle, deltaAngle)
    } else {
        // this is the hard and slow part :-)
        arcRectf.set(-transformedRadiusX, -transformedRadiusY, transformedRadiusX, transformedRadiusY)
        arcMatrix.reset()
        arcMatrix.postRotate(theta)
        arcMatrix.postTranslate(cx, cy)
        arcMatrix.invert(arcMatrix2)
        path.transform(arcMatrix2)
        path.arcTo(arcRectf, startAngle, deltaAngle)
        path.transform(arcMatrix)
    }
}

private fun angle(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    return Math.toDegrees(Math.atan2(x1.toDouble(), y1.toDouble()) - Math.atan2(x2.toDouble(), y2.toDouble()))
        .toFloat() % 360
}