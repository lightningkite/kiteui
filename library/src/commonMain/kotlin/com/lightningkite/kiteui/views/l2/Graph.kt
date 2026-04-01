package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.canvas.DrawingContext2D
import com.lightningkite.kiteui.views.canvas.TextAlign
import com.lightningkite.kiteui.views.canvas.clear
import com.lightningkite.kiteui.views.canvas.drawText
import com.lightningkite.kiteui.views.canvas.fill
import com.lightningkite.kiteui.views.canvas.fillPaint
import com.lightningkite.kiteui.views.canvas.font
import com.lightningkite.kiteui.views.canvas.height
import com.lightningkite.kiteui.views.canvas.strokePaint
import com.lightningkite.kiteui.views.canvas.textAlign
import com.lightningkite.kiteui.views.canvas.width
import com.lightningkite.kiteui.views.direct.*
import kotlin.js.JsName
import kotlin.jvm.JvmName
import kotlin.math.*

/**
 * Data point for the graph.
 */
data class Point(val x: Double, val y: Double)

/**
 * Delegate class for drawing graphs on a Canvas.
 * This provides basic graphing functionality with customizable appearance.
 */
class GraphDelegate : CanvasDelegate() {
    // Data to be displayed on the graph
    var data: List<Point> = emptyList()

    // Graph appearance properties
    private var _lineColor: Color? = null // Color.blue
    var lineColor: Color
        get() = _lineColor ?: theme.foreground.closestColor()
        set(value) { _lineColor = value }
    private var _pointColor: Color? = null // Color.red
    var pointColor: Color
        get() = _pointColor ?: theme.foreground.closestColor()
        set(value) { _pointColor = value }
    private var _gridColor: Color? = null // Color(0.8f, 0.8f, 0.8f, 1.0f)
    var gridColor: Color
        get() = _gridColor ?: theme.background.closestColor().highlight(0.1f)
        set(value) { _gridColor = value }
    private var _axisColor: Color? = null // Color.black
    var axisColor: Color
        get() = _axisColor ?: theme.foreground.closestColor()
        set(value) { _axisColor = value }
    private var _textColor: Color? = null // Color.black
    var textColor: Color
        get() = _textColor ?: theme.foreground.closestColor()
        set(value) { _textColor = value }
    var showGrid: Boolean = true
    var showPoints: Boolean = true
    private var _pointSize: Dimension? = null // 5.0.dp
    var pointSize: Dimension
        get() = _pointSize ?: theme.padding.left
        set(value) { _pointSize = value }
    private var _lineWidth: Dimension? = null // 2.0.dp
    var lineWidth: Dimension
        get() = _lineWidth ?: 1.dp
        set(value) { _lineWidth = value }
    private var _padding: Dimension? = null // 0.dp
    var padding: Dimension
        get() = _padding ?: (theme.font.size * 4)
        set(value) { _padding = value }

    // Axis labels
    var xAxisLabel: String = "X"
    var yAxisLabel: String = "Y"

    // Font sizes
    var axisLabelFontSize: Dimension = 1.rem
    var tickLabelFontSize: Dimension = 0.8.rem
    var noDataMessageFontSize: Dimension = 2.rem

    override fun draw(context: DrawingContext2D) {
        if (data.isEmpty()) {
            drawEmptyGraph(context)
            return
        }

        val width = context.width
        val height = context.height

        // Calculate data bounds
        val minX = data.minOfOrNull { it.x } ?: 0.0
        val maxX = data.maxOfOrNull { it.x } ?: 0.0
        val minY = data.minOfOrNull { it.y } ?: 0.0
        val maxY = data.maxOfOrNull { it.y } ?: 0.0

        // Add some padding to the bounds
        val rangeX = (maxX - minX).coerceAtLeast(1.0)
        val rangeY = (maxY - minY).coerceAtLeast(1.0)
        val paddedMinX = minX - rangeX * 0.05
        val paddedMaxX = maxX + rangeX * 0.05
        val paddedMinY = minY - rangeY * 0.05
        val paddedMaxY = maxY + rangeY * 0.05

        // Get padding in canvas units
        val paddingCanvas = padding.canvasUnits

        // Scale factors to convert data coordinates to canvas coordinates
        val scaleX = (width - paddingCanvas * 2) / (paddedMaxX - paddedMinX)
        val scaleY = (height - paddingCanvas * 2) / (paddedMaxY - paddedMinY)

        // Function to convert data X to canvas X
        val toCanvasX = { x: Double -> (x - paddedMinX) * scaleX + paddingCanvas }

        // Function to convert data Y to canvas Y (note the inversion for Y)
        val toCanvasY = { y: Double -> height - ((y - paddedMinY) * scaleY + paddingCanvas) }

        with(context) {
            // Clear the canvas
            clear()

            // Draw grid if enabled
            if (showGrid) {
                drawGrid(context, paddedMinX, paddedMaxX, paddedMinY, paddedMaxY, toCanvasX, toCanvasY)
            }

            // Draw axes
            drawAxes(context, paddedMinX, paddedMaxX, paddedMinY, paddedMaxY, toCanvasX, toCanvasY)

            // Draw data line
            strokePaint = lineColor
            lineWidth = this@GraphDelegate.lineWidth.canvasUnits
            beginPath()
            val first = data.firstOrNull()
            if (first != null) {
                moveTo(toCanvasX(first.x), toCanvasY(first.y))
                for (point in data.drop(1)) {
                    lineTo(toCanvasX(point.x), toCanvasY(point.y))
                }
            }
            stroke()

            // Draw points if enabled
            if (showPoints) {
                fillPaint = pointColor
                val pointSizeCanvas = pointSize.canvasUnits
                for (point in data) {
                    beginPath()
                    val cx = toCanvasX(point.x)
                    val cy = toCanvasY(point.y)
                    rect(cx - pointSizeCanvas / 2, cy - pointSizeCanvas / 2, pointSizeCanvas, pointSizeCanvas)
                    fill()
                }
            }

            // Draw axis labels
            drawAxisLabels(context, width, height)
        }
    }

    private fun drawEmptyGraph(context: DrawingContext2D) {
        val width = context.width
        val height = context.height

        with(context) {
            clear()

            // Draw axes
            strokePaint = axisColor
            lineWidth = this@GraphDelegate.lineWidth.canvasUnits

            val paddingCanvas = padding.canvasUnits

            // X-axis
            beginPath()
            moveTo(paddingCanvas, height - paddingCanvas)
            lineTo(width - paddingCanvas, height - paddingCanvas)
            stroke()

            // Y-axis
            beginPath()
            moveTo(paddingCanvas, height - paddingCanvas)
            lineTo(paddingCanvas, paddingCanvas)
            stroke()

            // Draw axis labels
            drawAxisLabels(context, width, height)

            // Draw "No data" message
            fillPaint = textColor
            font(noDataMessageFontSize.canvasUnits, FontAndStyle(systemDefaultFont))
            textAlign(TextAlign.center)
            drawText("No data to display", width / 2, height / 2)
        }
    }

    private fun drawGrid(
        context: DrawingContext2D,
        minX: Double,
        maxX: Double,
        minY: Double,
        maxY: Double,
        toCanvasX: (Double) -> Double,
        toCanvasY: (Double) -> Double
    ) {
        val width = context.width
        val height = context.height

        with(context) {
            strokePaint = gridColor
            lineWidth = 0.5 * this@GraphDelegate.lineWidth.canvasUnits

            val paddingCanvas = padding.canvasUnits

            // Calculate grid line spacing
            val rangeX = maxX - minX
            val rangeY = maxY - minY

            val xStep = calculateGridStep(rangeX)
            val yStep = calculateGridStep(rangeY)

            // Draw vertical grid lines
            var x = ceil(minX / xStep) * xStep
            while (x <= maxX) {
                beginPath()
                moveTo(toCanvasX(x), paddingCanvas)
                lineTo(toCanvasX(x), height - paddingCanvas)
                stroke()
                x += xStep
            }

            // Draw horizontal grid lines
            var y = ceil(minY / yStep) * yStep
            while (y <= maxY) {
                beginPath()
                moveTo(paddingCanvas, toCanvasY(y))
                lineTo(width - paddingCanvas, toCanvasY(y))
                stroke()
                y += yStep
            }
        }
    }

    private fun drawAxes(
        context: DrawingContext2D,
        minX: Double,
        maxX: Double,
        minY: Double,
        maxY: Double,
        toCanvasX: (Double) -> Double,
        toCanvasY: (Double) -> Double
    ) {
        val width = context.width
        val height = context.height
        val paddingCanvas = padding.canvasUnits
        if(width < paddingCanvas * 2 || height < paddingCanvas * 2) return

        with(context) {
            strokePaint = axisColor
            lineWidth = this@GraphDelegate.lineWidth.canvasUnits

            // X-axis
            beginPath()
            moveTo(paddingCanvas, toCanvasY(0.0).coerceIn(paddingCanvas, height - paddingCanvas))
            lineTo(width - paddingCanvas, toCanvasY(0.0).coerceIn(paddingCanvas, height - paddingCanvas))
            stroke()

            // Y-axis
            beginPath()
            moveTo(toCanvasX(0.0).coerceIn(paddingCanvas, width - paddingCanvas), paddingCanvas)
            lineTo(toCanvasX(0.0).coerceIn(paddingCanvas, width - paddingCanvas), height - paddingCanvas)
            stroke()

            // Draw tick marks and labels
            fillPaint = axisColor
            font(tickLabelFontSize.canvasUnits, FontAndStyle(systemDefaultFont))

            // Calculate tick spacing
            val rangeX = maxX - minX
            val rangeY = maxY - minY

            val xStep = calculateGridStep(rangeX)
            val yStep = calculateGridStep(rangeY)

            // X-axis ticks and labels
            var x = ceil(minX / xStep) * xStep
            while (x <= maxX) {
                val cx = toCanvasX(x)

                // Draw tick
                beginPath()
                moveTo(cx, toCanvasY(0.0).coerceIn(paddingCanvas, height - paddingCanvas))
                lineTo(cx, toCanvasY(0.0).coerceIn(paddingCanvas, height - paddingCanvas) + 5)
                stroke()

                // Draw label
                textAlign(TextAlign.center)
                drawText(formatNumber(x), cx, height - paddingCanvas + 15)

                x += xStep
            }

            // Y-axis ticks and labels
            var y = ceil(minY / yStep) * yStep
            while (y <= maxY) {
                val cy = toCanvasY(y)

                // Draw tick
                beginPath()
                moveTo(toCanvasX(0.0).coerceIn(paddingCanvas, width - paddingCanvas), cy)
                lineTo(toCanvasX(0.0).coerceIn(paddingCanvas, width - paddingCanvas) - 5, cy)
                stroke()

                // Draw label
                textAlign(TextAlign.right)
                drawText(formatNumber(y), paddingCanvas - 8, cy + 4)

                y += yStep
            }
        }
    }

    private fun drawAxisLabels(context: DrawingContext2D, width: Double, height: Double) {
        with(context) {
            fillPaint = axisColor
            font(axisLabelFontSize.canvasUnits, FontAndStyle(systemDefaultFont))

            val paddingCanvas = padding.canvasUnits

            // X-axis label
            textAlign(TextAlign.center)
            drawText(xAxisLabel, width / 2, height - paddingCanvas / 3)

            // Y-axis label
            save()
            translate(paddingCanvas / 3, height / 2)
            rotate(-PI / 2)
            drawText(yAxisLabel, 0.0, 0.0)
            restore()
        }
    }

    private fun calculateGridStep(range: Double): Double {
        val rawStep = range / 10
        val magnitude = 10.0.pow(floor(log10(rawStep)))
        val normalized = rawStep / magnitude

        return when {
            normalized < 1.5 -> magnitude
            normalized < 3.5 -> 2 * magnitude
            normalized < 7.5 -> 5 * magnitude
            else -> 10 * magnitude
        }
    }

    private fun formatNumber(value: Double): String {
        return if (value == 0.0) "0" else {
            val rounded = (value * 1000).roundToInt() / 1000.0
            if (rounded == rounded.toInt().toDouble()) {
                rounded.toInt().toString()
            } else {
                rounded.toString()
            }
        }
    }
}

/**
 * Extension function to create a graph canvas with the given setup.
 */
inline fun ElementWriter.graph(setup: GraphDelegate.() -> Unit = {}): Canvas {
    return canvas {
        delegate = GraphDelegate().apply(setup)
    }
}

/**
 * Extension function to create a line graph with the given data.
 */
inline fun ElementWriter.lineGraph(
    data: List<Point>,
    setup: GraphDelegate.() -> Unit = {}
): Canvas {
    return graph {
        this.data = data
        setup()
    }
}

/**
 * Extension function to create a line graph from a list of y values.
 * X values will be the indices of the y values.
 */
@JvmName("lineGraphFromYValues")
@JsName("lineGraphFromYValues")
inline fun ElementWriter.lineGraph(
    yValues: List<Double>,
    setup: GraphDelegate.() -> Unit = {}
): Canvas {
    val points = yValues.mapIndexed { index, y -> Point(index.toDouble(), y) }
    return lineGraph(points, setup)
}

/**
 * Extension function to create a line graph from a list of x-y pairs.
 */
@JvmName("lineGraphFromPairs")
@JsName("lineGraphFromPairs")
inline fun ElementWriter.lineGraph(
    points: List<Pair<Double, Double>>,
    setup: GraphDelegate.() -> Unit = {}
): Canvas {
    return lineGraph(points.map { Point(it.first, it.second) }, setup)
}
