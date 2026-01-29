package com.lightningkite.kiteui.lottie

import com.lightningkite.kiteui.lottie.models.*
import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.GradientStop
import com.lightningkite.kiteui.models.LinearGradient
import com.lightningkite.kiteui.models.RadialGradient
import com.lightningkite.kiteui.views.canvas.DrawingContext2D
import com.lightningkite.kiteui.views.canvas.fill
import com.lightningkite.kiteui.views.canvas.fillEvenOdd
import com.lightningkite.kiteui.views.canvas.fillPaint
import com.lightningkite.kiteui.views.canvas.strokePaint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders Lottie animations to a KiteUI Canvas.
 */
class LottieRenderer(
    private val animation: LottieAnimation
) {
    /**
     * Render the animation at the given frame.
     */
    fun render(ctx: DrawingContext2D, frame: Double) {
        ctx.save()

        // Render layers in order (back to front)
        // Lottie layers are typically ordered from front to back, so render in reverse
        for (i in animation.layers.indices.reversed()) {
            val layer = animation.layers[i]
            renderLayer(ctx, layer, frame)
        }

        ctx.restore()
    }

    /**
     * Render a single layer.
     */
    private fun renderLayer(ctx: DrawingContext2D, layer: LottieLayer, frame: Double) {
        // Skip hidden layers
        if (layer.hidden) return

        // Check if layer is visible at current frame
        val adjustedFrame = (frame - layer.startTime) * layer.stretch
        if (adjustedFrame < layer.inPoint || adjustedFrame >= layer.outPoint) return

        ctx.save()

        // Apply layer transform
        layer.transform?.let { applyTransform(ctx, it, adjustedFrame) }

        when (layer.type) {
            LayerType.SHAPE -> renderShapeLayer(ctx, layer, adjustedFrame)
            LayerType.SOLID -> renderSolidLayer(ctx, layer)
            LayerType.PRECOMP -> renderPrecompLayer(ctx, layer, adjustedFrame)
            // Other layer types not yet implemented
        }

        ctx.restore()
    }

    /**
     * Apply a transform to the context.
     */
    private fun applyTransform(ctx: DrawingContext2D, transform: LottieTransform, frame: Double) {
        // Get position (either combined or split)
        val position = if (transform.position != null) {
            transform.position.at(frame)
        } else {
            val x = transform.positionX?.at(frame) ?: 0.0
            val y = transform.positionY?.at(frame) ?: 0.0
            listOf(x, y)
        }

        val anchorPoint = transform.anchorPoint?.at(frame) ?: listOf(0.0, 0.0)
        val scale = transform.scale?.at(frame) ?: listOf(100.0, 100.0)
        val rotation = transform.rotation?.at(frame) ?: transform.rotationZ?.at(frame) ?: 0.0
        val opacity = transform.opacity?.at(frame) ?: 100.0

        // Apply transforms in order: translate to position, rotate, scale, translate by anchor
        ctx.translate(position.getOrElse(0) { 0.0 }, position.getOrElse(1) { 0.0 })
        ctx.rotate(rotation * PI / 180.0)
        ctx.scale(scale.getOrElse(0) { 100.0 } / 100.0, scale.getOrElse(1) { 100.0 } / 100.0)
        ctx.translate(-anchorPoint.getOrElse(0) { 0.0 }, -anchorPoint.getOrElse(1) { 0.0 })

        // Apply layer opacity
        ctx.globalAlpha = ctx.globalAlpha * (opacity / 100.0)
        // TODO: Handle skew
    }

    /**
     * Render a shape layer.
     */
    private fun renderShapeLayer(ctx: DrawingContext2D, layer: LottieLayer, frame: Double) {
        for (shape in layer.shapes) {
            renderShape(ctx, shape, frame)
        }
    }

    /**
     * Render a solid color layer.
     */
    private fun renderSolidLayer(ctx: DrawingContext2D, layer: LottieLayer) {
        val color = layer.solidColor?.let { parseHexColor(it) } ?: Color.black
        val width = layer.solidWidth?.toDouble() ?: animation.width.toDouble()
        val height = layer.solidHeight?.toDouble() ?: animation.height.toDouble()

        ctx.fillPaint = color
        ctx.fillRect(0.0, 0.0, width, height)
    }

    /**
     * Render a precomp (nested composition) layer.
     */
    private fun renderPrecompLayer(ctx: DrawingContext2D, layer: LottieLayer, frame: Double) {
        val refId = layer.refId ?: return
        val asset = animation.assets.find { it.id == refId } ?: return
        val assetLayers = asset.layers ?: return

        // Handle time remapping if present
        val timeRemappedFrame = layer.timeRemapping?.let {
            it.at(frame) * animation.frameRate
        } ?: frame

        for (i in assetLayers.indices.reversed()) {
            val assetLayer = assetLayers[i]
            renderLayer(ctx, assetLayer, timeRemappedFrame)
        }
    }

    /**
     * Render a shape item.
     */
    private fun renderShape(ctx: DrawingContext2D, shape: LottieShape, frame: Double) {
        if (shape.hidden) return

        when (shape) {
            is LottieShapeGroup -> renderShapeGroup(ctx, shape, frame)
            else -> {
                // Individual shapes are rendered as part of groups
            }
        }
    }

    /**
     * Render a shape group.
     * Groups contain paths, fills, strokes, and transforms.
     */
    private fun renderShapeGroup(ctx: DrawingContext2D, group: LottieShapeGroup, frame: Double) {
        ctx.save()

        // Find and apply the group's transform
        val groupTransform = group.items.filterIsInstance<LottieShapeTransform>().firstOrNull()
        groupTransform?.let { applyShapeTransform(ctx, it, frame) }

        // Collect path shapes
        val paths = mutableListOf<PathData>()

        // Collect fills and strokes
        val fills = group.items.filterIsInstance<LottieShapeFill>()
        val strokes = group.items.filterIsInstance<LottieShapeStroke>()
        val gradientFills = group.items.filterIsInstance<LottieShapeGradientFill>()
        val gradientStrokes = group.items.filterIsInstance<LottieShapeGradientStroke>()
        val trims = group.items.filterIsInstance<LottieShapeTrim>()

        // Build paths from shapes
        for (item in group.items) {
            when (item) {
                is LottieShapeGroup -> {
                    // Nested group
                    renderShapeGroup(ctx, item, frame)
                }
                is LottieShapeEllipse -> {
                    paths.add(buildEllipsePath(item, frame))
                }
                is LottieShapeRectangle -> {
                    paths.add(buildRectanglePath(item, frame))
                }
                is LottieShapePath -> {
                    item.path?.at(frame)?.let { bezierPath ->
                        paths.add(PathData.fromBezierPath(bezierPath))
                    }
                }
                is LottieShapePolystar -> {
                    paths.add(buildPolystarPath(item, frame))
                }
                else -> {
                    // Fills, strokes, transforms handled separately
                }
            }
        }

        // Draw paths with fills and strokes
        if (paths.isNotEmpty()) {
            // Apply trims if present
            // TODO: Implement trim path

            // Draw fills
            for (fill in fills) {
                if (fill.hidden) continue
                drawPathsWithFill(ctx, paths, fill, frame)
            }

            for (gradientFill in gradientFills) {
                if (gradientFill.hidden) continue
                drawPathsWithGradientFill(ctx, paths, gradientFill, frame)
            }

            // Draw strokes
            for (stroke in strokes) {
                if (stroke.hidden) continue
                drawPathsWithStroke(ctx, paths, stroke, frame)
            }

            for (gradientStroke in gradientStrokes) {
                if (gradientStroke.hidden) continue
                drawPathsWithGradientStroke(ctx, paths, gradientStroke, frame)
            }
        }

        ctx.restore()
    }

    /**
     * Apply a shape transform.
     */
    private fun applyShapeTransform(ctx: DrawingContext2D, transform: LottieShapeTransform, frame: Double) {
        val position = transform.position?.at(frame) ?: listOf(0.0, 0.0)
        val anchorPoint = transform.anchorPoint?.at(frame) ?: listOf(0.0, 0.0)
        val scale = transform.scale?.at(frame) ?: listOf(100.0, 100.0)
        val rotation = transform.rotation?.at(frame) ?: 0.0

        ctx.translate(position.getOrElse(0) { 0.0 }, position.getOrElse(1) { 0.0 })
        ctx.rotate(rotation * PI / 180.0)
        ctx.scale(scale.getOrElse(0) { 100.0 } / 100.0, scale.getOrElse(1) { 100.0 } / 100.0)
        ctx.translate(-anchorPoint.getOrElse(0) { 0.0 }, -anchorPoint.getOrElse(1) { 0.0 })
    }

    /**
     * Build an ellipse path.
     */
    private fun buildEllipsePath(ellipse: LottieShapeEllipse, frame: Double): PathData {
        val position = ellipse.position?.at(frame) ?: listOf(0.0, 0.0)
        val size = ellipse.size?.at(frame) ?: listOf(0.0, 0.0)

        val cx = position.getOrElse(0) { 0.0 }
        val cy = position.getOrElse(1) { 0.0 }
        val rx = (size.getOrElse(0) { 0.0 }) / 2.0
        val ry = (size.getOrElse(1) { 0.0 }) / 2.0

        // Approximate ellipse with bezier curves (4 segments)
        // Magic number for bezier approximation of circle: 0.5522847498
        val k = 0.5522847498

        return PathData(
            commands = listOf(
                PathCommand.MoveTo(cx + rx, cy),
                PathCommand.BezierTo(cx + rx, cy + ry * k, cx + rx * k, cy + ry, cx, cy + ry),
                PathCommand.BezierTo(cx - rx * k, cy + ry, cx - rx, cy + ry * k, cx - rx, cy),
                PathCommand.BezierTo(cx - rx, cy - ry * k, cx - rx * k, cy - ry, cx, cy - ry),
                PathCommand.BezierTo(cx + rx * k, cy - ry, cx + rx, cy - ry * k, cx + rx, cy),
                PathCommand.Close
            )
        )
    }

    /**
     * Build a rectangle path.
     */
    private fun buildRectanglePath(rect: LottieShapeRectangle, frame: Double): PathData {
        val position = rect.position?.at(frame) ?: listOf(0.0, 0.0)
        val size = rect.size?.at(frame) ?: listOf(0.0, 0.0)
        val radius = rect.radius?.at(frame) ?: 0.0

        val cx = position.getOrElse(0) { 0.0 }
        val cy = position.getOrElse(1) { 0.0 }
        val w = size.getOrElse(0) { 0.0 }
        val h = size.getOrElse(1) { 0.0 }
        val r = radius.coerceAtMost(minOf(w, h) / 2.0)

        val left = cx - w / 2
        val right = cx + w / 2
        val top = cy - h / 2
        val bottom = cy + h / 2

        if (r <= 0) {
            // Simple rectangle
            return PathData(
                commands = listOf(
                    PathCommand.MoveTo(left, top),
                    PathCommand.LineTo(right, top),
                    PathCommand.LineTo(right, bottom),
                    PathCommand.LineTo(left, bottom),
                    PathCommand.Close
                )
            )
        } else {
            // Rounded rectangle
            val k = 0.5522847498 * r
            return PathData(
                commands = listOf(
                    PathCommand.MoveTo(left + r, top),
                    PathCommand.LineTo(right - r, top),
                    PathCommand.BezierTo(right - r + k, top, right, top + r - k, right, top + r),
                    PathCommand.LineTo(right, bottom - r),
                    PathCommand.BezierTo(right, bottom - r + k, right - r + k, bottom, right - r, bottom),
                    PathCommand.LineTo(left + r, bottom),
                    PathCommand.BezierTo(left + r - k, bottom, left, bottom - r + k, left, bottom - r),
                    PathCommand.LineTo(left, top + r),
                    PathCommand.BezierTo(left, top + r - k, left + r - k, top, left + r, top),
                    PathCommand.Close
                )
            )
        }
    }

    /**
     * Build a polystar (star/polygon) path.
     */
    private fun buildPolystarPath(star: LottieShapePolystar, frame: Double): PathData {
        val position = star.position?.at(frame) ?: listOf(0.0, 0.0)
        val points = (star.points?.at(frame) ?: 5.0).toInt()
        val rotation = (star.rotation?.at(frame) ?: 0.0) * PI / 180.0
        val outerRadius = star.outerRadius?.at(frame) ?: 100.0
        val outerRoundness = (star.outerRoundness?.at(frame) ?: 0.0) / 100.0
        val innerRadius = star.innerRadius?.at(frame) ?: 50.0
        val innerRoundness = (star.innerRoundness?.at(frame) ?: 0.0) / 100.0

        val cx = position.getOrElse(0) { 0.0 }
        val cy = position.getOrElse(1) { 0.0 }

        val commands = mutableListOf<PathCommand>()
        val isStar = star.starType == 1 // 1 = star, 2 = polygon

        val numPoints = if (isStar) points * 2 else points
        val angleStep = 2 * PI / numPoints

        for (i in 0 until numPoints) {
            val angle = rotation - PI / 2 + angleStep * i
            val radius = if (isStar && i % 2 == 1) innerRadius else outerRadius

            val x = cx + cos(angle) * radius
            val y = cy + sin(angle) * radius

            if (i == 0) {
                commands.add(PathCommand.MoveTo(x, y))
            } else {
                commands.add(PathCommand.LineTo(x, y))
            }
        }

        commands.add(PathCommand.Close)
        return PathData(commands)
    }

    /**
     * Draw paths with a solid fill.
     */
    private fun drawPathsWithFill(
        ctx: DrawingContext2D,
        paths: List<PathData>,
        fill: LottieShapeFill,
        frame: Double
    ) {
        val color = fill.color?.at(frame) ?: listOf(0.0, 0.0, 0.0, 1.0)
        val opacity = (fill.opacity?.at(frame) ?: 100.0) / 100.0

        ctx.fillPaint = Color(
            red = color.getOrElse(0) { 0.0 }.toFloat(),
            green = color.getOrElse(1) { 0.0 }.toFloat(),
            blue = color.getOrElse(2) { 0.0 }.toFloat(),
            alpha = (color.getOrElse(3) { 1.0 } * opacity).toFloat()
        )

        ctx.beginPath()
        for (path in paths) {
            drawPath(ctx, path)
        }

        if (fill.fillRule == FillRule.EVEN_ODD) {
            ctx.fillEvenOdd()
        } else {
            ctx.fill()
        }
    }

    /**
     * Draw paths with a gradient fill.
     */
    private fun drawPathsWithGradientFill(
        ctx: DrawingContext2D,
        paths: List<PathData>,
        fill: LottieShapeGradientFill,
        frame: Double
    ) {
        val stops = parseGradientStops(fill.colors, frame)
        val opacity = (fill.opacity?.at(frame) ?: 100.0) / 100.0

        // Apply opacity to stops
        val adjustedStops = stops.map { GradientStop(it.ratio, it.color.applyAlpha(opacity.toFloat())) }

        ctx.fillPaint = if (fill.gradientType == GradientType.RADIAL) {
            RadialGradient(adjustedStops)
        } else {
            LinearGradient(adjustedStops)
        }

        ctx.beginPath()
        for (path in paths) {
            drawPath(ctx, path)
        }

        if (fill.fillRule == FillRule.EVEN_ODD) {
            ctx.fillEvenOdd()
        } else {
            ctx.fill()
        }
    }

    /**
     * Draw paths with a solid stroke.
     */
    private fun drawPathsWithStroke(
        ctx: DrawingContext2D,
        paths: List<PathData>,
        stroke: LottieShapeStroke,
        frame: Double
    ) {
        val color = stroke.color?.at(frame) ?: listOf(0.0, 0.0, 0.0, 1.0)
        val opacity = (stroke.opacity?.at(frame) ?: 100.0) / 100.0
        val width = stroke.width?.at(frame) ?: 1.0

        ctx.strokePaint = Color(
            red = color.getOrElse(0) { 0.0 }.toFloat(),
            green = color.getOrElse(1) { 0.0 }.toFloat(),
            blue = color.getOrElse(2) { 0.0 }.toFloat(),
            alpha = (color.getOrElse(3) { 1.0 } * opacity).toFloat()
        )
        ctx.lineWidth = width
        ctx.miterLimit = stroke.miterLimit

        // TODO: Apply line cap and join (requires resolving name conflict with Lottie's LineCap/LineJoin)

        ctx.beginPath()
        for (path in paths) {
            drawPath(ctx, path)
        }
        ctx.stroke()
    }

    /**
     * Draw paths with a gradient stroke.
     */
    private fun drawPathsWithGradientStroke(
        ctx: DrawingContext2D,
        paths: List<PathData>,
        stroke: LottieShapeGradientStroke,
        frame: Double
    ) {
        val stops = parseGradientStops(stroke.colors, frame)
        val opacity = (stroke.opacity?.at(frame) ?: 100.0) / 100.0
        val width = stroke.width?.at(frame) ?: 1.0

        val adjustedStops = stops.map { GradientStop(it.ratio, it.color.applyAlpha(opacity.toFloat())) }

        ctx.strokePaint = if (stroke.gradientType == GradientType.RADIAL) {
            RadialGradient(adjustedStops)
        } else {
            LinearGradient(adjustedStops)
        }
        ctx.lineWidth = width
        ctx.miterLimit = stroke.miterLimit

        // TODO: Apply line cap and join (requires resolving name conflict with Lottie's LineCap/LineJoin)

        ctx.beginPath()
        for (path in paths) {
            drawPath(ctx, path)
        }
        ctx.stroke()
    }

    /**
     * Draw a path to the canvas context.
     */
    private fun drawPath(ctx: DrawingContext2D, path: PathData) {
        for (cmd in path.commands) {
            when (cmd) {
                is PathCommand.MoveTo -> ctx.moveTo(cmd.x, cmd.y)
                is PathCommand.LineTo -> ctx.lineTo(cmd.x, cmd.y)
                is PathCommand.BezierTo -> ctx.bezierCurveTo(cmd.cp1x, cmd.cp1y, cmd.cp2x, cmd.cp2y, cmd.x, cmd.y)
                is PathCommand.Close -> ctx.closePath()
            }
        }
    }

    /**
     * Parse gradient color stops from Lottie format.
     */
    private fun parseGradientStops(colors: LottieGradientColors?, frame: Double): List<GradientStop> {
        if (colors == null) return emptyList()

        val numColors = colors.numColors
        val values = colors.colors?.at(frame) ?: return emptyList()

        // Lottie gradient format: [pos1, r1, g1, b1, pos2, r2, g2, b2, ...]
        // May also include alpha values at the end
        val stops = mutableListOf<GradientStop>()
        var i = 0
        while (i + 3 < values.size && stops.size < numColors) {
            val pos = values[i].toFloat()
            val r = values[i + 1].toFloat()
            val g = values[i + 2].toFloat()
            val b = values[i + 3].toFloat()
            stops.add(GradientStop(pos, Color(alpha = 1f, red = r, green = g, blue = b)))
            i += 4
        }

        return stops
    }

    /**
     * Parse hex color string to Color.
     */
    private fun parseHexColor(hex: String): Color {
        return try {
            Color.fromHexString(hex)
        } catch (e: Exception) {
            Color.black
        }
    }

    companion object {
        /**
         * Create path data from a Lottie bezier path.
         */
        private fun PathData.Companion.fromBezierPath(path: LottieBezierPath): PathData {
            val commands = mutableListOf<PathCommand>()

            if (path.vertices.isEmpty()) return PathData(commands)

            // First vertex is moveTo
            val firstVertex = path.vertices[0]
            commands.add(PathCommand.MoveTo(
                firstVertex.getOrElse(0) { 0.0 },
                firstVertex.getOrElse(1) { 0.0 }
            ))

            // Draw curves between vertices
            for (i in 1 until path.vertices.size) {
                val prevVertex = path.vertices[i - 1]
                val vertex = path.vertices[i]
                val outTangent = path.outTangents.getOrNull(i - 1) ?: listOf(0.0, 0.0)
                val inTangent = path.inTangents.getOrNull(i) ?: listOf(0.0, 0.0)

                // Tangents are relative to their vertices
                val cp1x = prevVertex.getOrElse(0) { 0.0 } + outTangent.getOrElse(0) { 0.0 }
                val cp1y = prevVertex.getOrElse(1) { 0.0 } + outTangent.getOrElse(1) { 0.0 }
                val cp2x = vertex.getOrElse(0) { 0.0 } + inTangent.getOrElse(0) { 0.0 }
                val cp2y = vertex.getOrElse(1) { 0.0 } + inTangent.getOrElse(1) { 0.0 }
                val x = vertex.getOrElse(0) { 0.0 }
                val y = vertex.getOrElse(1) { 0.0 }

                // Check if it's a straight line (tangents are zero)
                val isLine = outTangent.all { it == 0.0 } && inTangent.all { it == 0.0 }
                if (isLine) {
                    commands.add(PathCommand.LineTo(x, y))
                } else {
                    commands.add(PathCommand.BezierTo(cp1x, cp1y, cp2x, cp2y, x, y))
                }
            }

            // Close the path if needed
            if (path.closed && path.vertices.size > 1) {
                val lastVertex = path.vertices.last()
                val firstVertex2 = path.vertices.first()
                val outTangent = path.outTangents.lastOrNull() ?: listOf(0.0, 0.0)
                val inTangent = path.inTangents.firstOrNull() ?: listOf(0.0, 0.0)

                val cp1x = lastVertex.getOrElse(0) { 0.0 } + outTangent.getOrElse(0) { 0.0 }
                val cp1y = lastVertex.getOrElse(1) { 0.0 } + outTangent.getOrElse(1) { 0.0 }
                val cp2x = firstVertex2.getOrElse(0) { 0.0 } + inTangent.getOrElse(0) { 0.0 }
                val cp2y = firstVertex2.getOrElse(1) { 0.0 } + inTangent.getOrElse(1) { 0.0 }

                val isLine = outTangent.all { it == 0.0 } && inTangent.all { it == 0.0 }
                if (!isLine) {
                    commands.add(PathCommand.BezierTo(cp1x, cp1y, cp2x, cp2y,
                        firstVertex2.getOrElse(0) { 0.0 }, firstVertex2.getOrElse(1) { 0.0 }))
                }

                commands.add(PathCommand.Close)
            }

            return PathData(commands)
        }
    }
}

/**
 * Path data representation.
 */
data class PathData(
    val commands: List<PathCommand>
) {
    companion object
}

/**
 * Path commands.
 */
sealed class PathCommand {
    data class MoveTo(val x: Double, val y: Double) : PathCommand()
    data class LineTo(val x: Double, val y: Double) : PathCommand()
    data class BezierTo(
        val cp1x: Double, val cp1y: Double,
        val cp2x: Double, val cp2y: Double,
        val x: Double, val y: Double
    ) : PathCommand()
    data object Close : PathCommand()
}
