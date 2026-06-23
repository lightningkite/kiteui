package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.debugMode
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.canvas.DrawingContext2D
import com.lightningkite.kiteui.views.canvas.TextAlign
import com.lightningkite.kiteui.views.canvas.clear
import com.lightningkite.kiteui.views.canvas.drawText
import com.lightningkite.kiteui.views.canvas.ellipse
import com.lightningkite.kiteui.views.canvas.fill
import com.lightningkite.kiteui.views.canvas.fillPaint
import com.lightningkite.kiteui.views.canvas.font
import com.lightningkite.kiteui.views.canvas.height
import com.lightningkite.kiteui.views.canvas.strokePaint
import com.lightningkite.kiteui.views.canvas.textAlign
import com.lightningkite.kiteui.views.canvas.width
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.write
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
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
open class GraphDelegate : CanvasDelegate() {
    // Data to be displayed on the graph
    var data: List<Point> = emptyList()

    enum class PointShape {
        Square,
        Circle,
    }

    // Graph appearance properties
    private var _lineColor: Color? = null // Color.blue
    var lineColor: Color
        get() = _lineColor ?: theme.foreground.closestColor()
        set(value) {
            _lineColor = value
        }

    private var _pointColor: Color? = null // Color.red
    var pointColor: Color
        get() = _pointColor ?: theme.foreground.closestColor()
        set(value) {
            _pointColor = value
        }

    private var _gridColor: Color? = null // Color(0.8f, 0.8f, 0.8f, 1.0f)
    var gridColor: Color
        get() = _gridColor ?: theme.background.closestColor().highlight(0.1f)
        set(value) {
            _gridColor = value
        }

    private var _axisColor: Color? = null // Color.black
    var axisColor: Color
        get() = _axisColor ?: theme.foreground.closestColor()
        set(value) {
            _axisColor = value
        }

    private var _textColor: Color? = null // Color.black
    var textColor: Color
        get() = _textColor ?: theme.foreground.closestColor()
        set(value) {
            _textColor = value
        }
    var showGrid: Boolean = true
    var showPoints: Boolean = true

    private var _pointSize: Dimension? = null // 5.0.dp
    var pointSize: Dimension
        get() = _pointSize ?: theme.padding.left
        set(value) { _pointSize = value }
    private var _pointShape: PointShape? = null // PointShape.Square
    var pointShape: PointShape
        get() = _pointShape ?: PointShape.Square
        set(value) { _pointShape = value }
    private var _lineWidth: Dimension? = null // 2.0.dp
    var lineWidth: Dimension
        get() = _lineWidth ?: 1.dp
        set(value) {
            _lineWidth = value
        }

    private var _padding: Dimension? = null // 0.dp
    var padding: Dimension
        get() = _padding ?: (theme.font.size * 4)
        set(value) {
            _padding = value
        }

    // Axis labels
    var xAxisLabel: String = "X"
    var yAxisLabel: String = "Y"

    var xAxisLabels: List<String>? = null
    var yAxisLabels: List<String>? = null

    // Font sizes
    var axisLabelFontSize: Dimension = 1.rem
    var tickLabelFontSize: Dimension = 0.8.rem
    var noDataMessageFontSize: Dimension = 2.rem

    // Calculate data bounds
    private val rawMinX get() = min(0.0, data.minOfOrNull { it.x } ?: 0.0)
    private val rawMaxX get() = max(0.0, data.maxOfOrNull { it.x } ?: 0.0)
    private val rawMinY get() = min(0.0, data.minOfOrNull { it.y } ?: 0.0)
    private val rawMaxY get() = max(0.0, data.maxOfOrNull { it.y } ?: 0.0)

    // Add some padding to the bounds
    private val rawRangeX get() = (rawMaxX - rawMinX).coerceAtLeast(1.0)
    private val rawRangeY get() = (rawMaxY - rawMinY).coerceAtLeast(1.0)
    private val minX get() = rawMinX - rawRangeX * 0.05
    private val maxX get() = rawMaxX + rawRangeX * 0.05
    private val minY get() = rawMinY - rawRangeY * 0.05
    private val maxY get() = rawMaxY + rawRangeY * 0.05
    private val xStep get() = xAxisLabels?.let { rawRangeX / (it.size - 1) } ?: calculateGridStep(maxX - minX)
    private val yStep get() = yAxisLabels?.let { rawRangeY / (it.size - 1) } ?: calculateGridStep(maxY - minY)

    private val xAxisLabelHeight = 2.5.rem.canvasUnits
    private val yAxisLabelWidth: Double
        get() = (2 + (yAxisLabels?.maxOf { it.length } ?: run {
            var longestLabelSize = 0
            var y = ceil(minY / yStep) * yStep
            while (y <= maxY) {
                val nextLabelSize = formatNumber(y).length
                if (nextLabelSize > longestLabelSize) {
                    longestLabelSize = nextLabelSize
                }
                y += yStep
            }
            longestLabelSize
        }) / 2.0).rem.canvasUnits

    override fun draw(context: DrawingContext2D) {
        if (data.isEmpty()) {
            drawEmptyGraph(context)
            return
        }

        val width = context.width
        val height = context.height

        // Get padding in canvas units
        val paddingCanvas = padding.canvasUnits

        // Scale factors to convert data coordinates to canvas coordinates
        val scaleX = (width - paddingCanvas * 2 - yAxisLabelWidth) / (maxX - minX)
        val scaleY = (height - paddingCanvas * 2 - xAxisLabelHeight) / (maxY - minY)

        // Function to convert data X to canvas X
        val toCanvasX = { x: Double -> (x - minX) * scaleX + paddingCanvas + yAxisLabelWidth }

        // Function to convert data Y to canvas Y (note the inversion for Y)
        val toCanvasY = { y: Double -> height - ((y - minY) * scaleY + paddingCanvas + xAxisLabelHeight) }

        with(context) {
            // Clear the canvas
            clear()

            // draw debug (Padding and Label Blocks)
            if (debugMode) {
                fillPaint = Color.fromHexString("#808050")
                beginPath()
                rect(0.0, 0.0, paddingCanvas, height)
                rect(0.0, 0.0, width, paddingCanvas)
                rect(width - paddingCanvas, 0.0, paddingCanvas, height)
                rect(0.0, height - paddingCanvas, width, paddingCanvas)
                fill()
                fillPaint = Color.fromHexString("#65548a")
                beginPath()
                rect(paddingCanvas, paddingCanvas, yAxisLabelWidth, height - 2 * paddingCanvas)
                rect(paddingCanvas, height - paddingCanvas - xAxisLabelHeight, width - 2 * paddingCanvas, xAxisLabelHeight)
                fill()
            }

            // Draw grid if enabled
            if (showGrid) {
                drawGrid(context, toCanvasX, toCanvasY)
            }

            // Draw axes
            drawAxes(context, toCanvasX, toCanvasY)

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
                    when (pointShape) {
                        PointShape.Square -> {
                            rect(cx - pointSizeCanvas / 2, cy - pointSizeCanvas / 2, pointSizeCanvas, pointSizeCanvas)
                        }
                        PointShape.Circle -> {
                            ellipse(cx, cy, pointSizeCanvas / 2, pointSizeCanvas / 2, 0.0, 0.0, 2*PI)
                        }
                    }
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
        toCanvasX: (Double) -> Double,
        toCanvasY: (Double) -> Double
    ) {
        with(context) {
            strokePaint = gridColor
            lineWidth = 0.5 * this@GraphDelegate.lineWidth.canvasUnits

            // Draw vertical grid lines
            var x = ceil(minX / xStep) * xStep
            while (x <= maxX) {
                beginPath()
                moveTo(toCanvasX(x), toCanvasY(minY))
                lineTo(toCanvasX(x), toCanvasY(maxY))
                stroke()
                x += xStep
            }

            // Draw horizontal grid lines
            var y = ceil(minY / yStep) * yStep
            while (y <= maxY) {
                beginPath()
                moveTo(toCanvasX(minX), toCanvasY(y))
                lineTo(toCanvasX(maxX), toCanvasY(y))
                stroke()
                y += yStep
            }
        }
    }

    private fun drawAxes(
        context: DrawingContext2D,
        toCanvasX: (Double) -> Double,
        toCanvasY: (Double) -> Double
    ) {
        val width = context.width
        val height = context.height
        val paddingCanvas = padding.canvasUnits
        if (width < paddingCanvas * 2 || height < paddingCanvas * 2) return

        with(context) {
            strokePaint = axisColor
            lineWidth = this@GraphDelegate.lineWidth.canvasUnits

            // X-axis
            beginPath()
            println("minX: $minX")
            println("maxX: $maxX")
            moveTo(toCanvasX(minX), toCanvasY(0.0))
            lineTo(toCanvasX(maxX), toCanvasY(0.0))
            stroke()

            // Y-axis
            beginPath()
            moveTo(toCanvasX(0.0), toCanvasY(minY))
            lineTo(toCanvasX(0.0), toCanvasY(maxY))
            stroke()

            // Draw tick marks and labels
            fillPaint = axisColor
            font(tickLabelFontSize.canvasUnits, FontAndStyle(systemDefaultFont))

            // X-axis ticks and labels
            var x = ceil(minX / xStep) * xStep
            while (x <= maxX) {
                val cx = toCanvasX(x)

                // Draw tick
                beginPath()
                moveTo(cx, toCanvasY(0.0))
                lineTo(cx, toCanvasY(0.0) + (tickLabelFontSize / 3).canvasUnits)
                stroke()

                // Draw label
                textAlign(TextAlign.center)
                drawText(xAxisLabels?.let { it[(x / xStep).roundToInt()] } ?: formatNumber(x), cx, height - paddingCanvas - xAxisLabelHeight + 1.rem.canvasUnits)

                x += xStep
            }

            // Y-axis ticks and labels
            var y = ceil(minY / yStep) * yStep
            while (y <= maxY) {
                val cy = toCanvasY(y)

                // Draw tick
                beginPath()
                moveTo(toCanvasX(0.0), cy)
                lineTo(toCanvasX(0.0) - (tickLabelFontSize / 3).canvasUnits, cy)
                stroke()

                // Draw label
                textAlign(TextAlign.right)
                drawText(yAxisLabels?.let { it[(y / yStep).roundToInt()] } ?: formatNumber(y), paddingCanvas + yAxisLabelWidth - 0.5.rem.canvasUnits, cy + 0.3.rem.canvasUnits)

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
            drawText(xAxisLabel, width / 2 + yAxisLabelWidth / 2, height - paddingCanvas - 0.15.rem.canvasUnits)

            // Y-axis label
            save()
            translate(paddingCanvas + 1.rem.canvasUnits, height / 2 - xAxisLabelHeight / 2)
            rotate(-PI / 2)
            textAlign(TextAlign.center)
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

class GraphCanvas(private val canvas: Canvas) : Element by canvas, GraphDelegate() {
    constructor(context: ElementContext) : this(Canvas(context))

    init {
        canvas.delegate = this as GraphDelegate
    }
}

/**
 * Create a [GraphCanvas] with the given setup.
 */
@OptIn(ExperimentalContracts::class)
inline fun ElementWriter.graph(setup: GraphCanvas.() -> Unit = {}): GraphCanvas {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(GraphCanvas(context), setup)
}

/**
 * Extension function to create a line graph with the given data.
 */
inline fun ElementWriter.lineGraph(
    data: List<Point>,
    setup: GraphCanvas.() -> Unit = {}
): GraphCanvas {
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
    setup: GraphCanvas.() -> Unit = {}
): GraphCanvas {
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
    setup: GraphCanvas.() -> Unit = {}
): GraphCanvas {
    return lineGraph(points.map { Point(it.first, it.second) }, setup)
}
