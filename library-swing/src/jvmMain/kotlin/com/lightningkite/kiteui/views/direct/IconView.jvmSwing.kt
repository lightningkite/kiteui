package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.models.toAwt
import com.lightningkite.kiteui.models.toAwtColor
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import java.awt.BasicStroke
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.geom.GeneralPath
import javax.swing.JComponent
import java.awt.Color as AwtColor

actual class IconView actual constructor(context: RContext) : RView(context) {

    // Custom component to render SVG-based icons
    // Mouse-transparent so events pass through to clickable ancestors
    inner class IconComponent : JComponent() {
        // Don't intercept mouse events - let them pass through to the JButton layer
        override fun contains(x: Int, y: Int): Boolean = false

        var icon: Icon? = null
            set(value) {
                field = value
                revalidate()
                repaint()
            }

        var iconColor: AwtColor = AwtColor.BLACK
            set(value) {
                field = value
                repaint()
            }

        override fun getPreferredSize(): Dimension {
            val icon = icon ?: return Dimension(24, 24)
            return Dimension(icon.width.px.toInt(), icon.height.px.toInt())
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val icon = icon ?: return

            val g2d = g.create() as Graphics2D
            try {
                // Enable anti-aliasing for smooth rendering
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

                // Calculate scaling to fit the icon in the component
                val scaleX = width.toDouble() / icon.viewBoxWidth
                val scaleY = height.toDouble() / icon.viewBoxHeight
                val scale = minOf(scaleX, scaleY)

                // Create transform for the icon
                val transform = AffineTransform()

                // Center the icon
                val scaledWidth = icon.viewBoxWidth * scale
                val scaledHeight = icon.viewBoxHeight * scale
                val offsetX = (width - scaledWidth) / 2
                val offsetY = (height - scaledHeight) / 2

                transform.translate(offsetX, offsetY)
                transform.scale(scale, scale)
                transform.translate(-icon.viewBoxMinX.toDouble(), -icon.viewBoxMinY.toDouble())

                g2d.transform(transform)

                // Render filled paths
                for (pathData in icon.pathDatas) {
                    val path = parseSvgPath(pathData)
                    g2d.color = iconColor
                    g2d.fill(path)
                }

                // Render stroked paths
                for (strokePath in icon.strokePathDatas) {
                    val path = parseSvgPath(strokePath.path)

                    // Handle fill if present
                    strokePath.fill?.let { fillPaint ->
                        g2d.color = fillPaint.toAwtColor()
                        g2d.fill(path)
                    }

                    // Handle stroke
                    val strokeWidth = strokePath.strokeWidth.px.toFloat()
                    val cap = when (strokePath.strokeLineCap) {
                        Icon.StrokeLineCap.Round -> BasicStroke.CAP_ROUND
                        Icon.StrokeLineCap.Square -> BasicStroke.CAP_SQUARE
                        Icon.StrokeLineCap.Butt -> BasicStroke.CAP_BUTT
                    }
                    g2d.stroke = BasicStroke(strokeWidth, cap, BasicStroke.JOIN_MITER)
                    g2d.color = iconColor
                    g2d.draw(path)
                }

            } finally {
                g2d.dispose()
            }
        }

        // SVG path parser - properly handles implicit commands and repeated coordinates
        private fun parseSvgPath(pathData: String): GeneralPath {
            val path = GeneralPath()
            if (pathData.isEmpty()) return path

            var currentX = 0.0
            var currentY = 0.0
            var lastControlX = 0.0
            var lastControlY = 0.0
            var pathStartX = 0.0
            var pathStartY = 0.0
            var lastCommand = ' '

            // Tokenize: extract commands and numbers in order
            val tokens = mutableListOf<Any>() // Char for commands, Double for numbers
            var i = 0
            while (i < pathData.length) {
                val c = pathData[i]
                when {
                    c in "MLHVCSQTAZmlhvcsqtaz" -> {
                        tokens.add(c)
                        i++
                    }
                    c in "0123456789.-+" -> {
                        // Parse a number
                        val start = i
                        if (pathData[i] == '-' || pathData[i] == '+') i++
                        while (i < pathData.length && pathData[i] in "0123456789.") i++
                        // Handle exponent
                        if (i < pathData.length && pathData[i] in "eE") {
                            i++
                            if (i < pathData.length && pathData[i] in "+-") i++
                            while (i < pathData.length && pathData[i] in "0123456789") i++
                        }
                        val numStr = pathData.substring(start, i)
                        numStr.toDoubleOrNull()?.let { tokens.add(it) }
                    }
                    else -> i++ // Skip whitespace, commas, etc.
                }
            }

            // Process tokens
            var tokenIndex = 0
            fun hasMoreNumbers(): Boolean {
                return tokenIndex < tokens.size && tokens[tokenIndex] is Double
            }
            fun nextNumber(): Double {
                return if (tokenIndex < tokens.size && tokens[tokenIndex] is Double) {
                    tokens[tokenIndex++] as Double
                } else 0.0
            }
            fun nextCommand(): Char? {
                return if (tokenIndex < tokens.size && tokens[tokenIndex] is Char) {
                    tokens[tokenIndex++] as Char
                } else null
            }
            fun peekIsCommand(): Boolean {
                return tokenIndex < tokens.size && tokens[tokenIndex] is Char
            }

            while (tokenIndex < tokens.size) {
                val cmd = if (peekIsCommand()) {
                    nextCommand()!!
                } else {
                    // Implicit command: after M use L, after m use l, otherwise repeat last command
                    when (lastCommand) {
                        'M' -> 'L'
                        'm' -> 'l'
                        else -> lastCommand
                    }
                }

                when (cmd) {
                    'M' -> {
                        currentX = nextNumber()
                        currentY = nextNumber()
                        pathStartX = currentX
                        pathStartY = currentY
                        path.moveTo(currentX.toFloat(), currentY.toFloat())
                        lastCommand = 'M'
                    }
                    'm' -> {
                        currentX += nextNumber()
                        currentY += nextNumber()
                        pathStartX = currentX
                        pathStartY = currentY
                        path.moveTo(currentX.toFloat(), currentY.toFloat())
                        lastCommand = 'm'
                    }
                    'L' -> {
                        currentX = nextNumber()
                        currentY = nextNumber()
                        path.lineTo(currentX.toFloat(), currentY.toFloat())
                        lastCommand = 'L'
                    }
                    'l' -> {
                        currentX += nextNumber()
                        currentY += nextNumber()
                        path.lineTo(currentX.toFloat(), currentY.toFloat())
                        lastCommand = 'l'
                    }
                    'H' -> {
                        currentX = nextNumber()
                        path.lineTo(currentX.toFloat(), currentY.toFloat())
                        lastCommand = 'H'
                    }
                    'h' -> {
                        currentX += nextNumber()
                        path.lineTo(currentX.toFloat(), currentY.toFloat())
                        lastCommand = 'h'
                    }
                    'V' -> {
                        currentY = nextNumber()
                        path.lineTo(currentX.toFloat(), currentY.toFloat())
                        lastCommand = 'V'
                    }
                    'v' -> {
                        currentY += nextNumber()
                        path.lineTo(currentX.toFloat(), currentY.toFloat())
                        lastCommand = 'v'
                    }
                    'C' -> {
                        val x1 = nextNumber()
                        val y1 = nextNumber()
                        val x2 = nextNumber()
                        val y2 = nextNumber()
                        currentX = nextNumber()
                        currentY = nextNumber()
                        path.curveTo(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), currentX.toFloat(), currentY.toFloat())
                        lastControlX = x2
                        lastControlY = y2
                        lastCommand = 'C'
                    }
                    'c' -> {
                        val x1 = currentX + nextNumber()
                        val y1 = currentY + nextNumber()
                        val x2 = currentX + nextNumber()
                        val y2 = currentY + nextNumber()
                        currentX += nextNumber()
                        currentY += nextNumber()
                        path.curveTo(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), currentX.toFloat(), currentY.toFloat())
                        lastControlX = x2
                        lastControlY = y2
                        lastCommand = 'c'
                    }
                    'S' -> {
                        val x1 = 2 * currentX - lastControlX
                        val y1 = 2 * currentY - lastControlY
                        val x2 = nextNumber()
                        val y2 = nextNumber()
                        currentX = nextNumber()
                        currentY = nextNumber()
                        path.curveTo(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), currentX.toFloat(), currentY.toFloat())
                        lastControlX = x2
                        lastControlY = y2
                        lastCommand = 'S'
                    }
                    's' -> {
                        val x1 = 2 * currentX - lastControlX
                        val y1 = 2 * currentY - lastControlY
                        val x2 = currentX + nextNumber()
                        val y2 = currentY + nextNumber()
                        currentX += nextNumber()
                        currentY += nextNumber()
                        path.curveTo(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), currentX.toFloat(), currentY.toFloat())
                        lastControlX = x2
                        lastControlY = y2
                        lastCommand = 's'
                    }
                    'Q' -> {
                        val cx = nextNumber()
                        val cy = nextNumber()
                        currentX = nextNumber()
                        currentY = nextNumber()
                        path.quadTo(cx.toFloat(), cy.toFloat(), currentX.toFloat(), currentY.toFloat())
                        lastControlX = cx
                        lastControlY = cy
                        lastCommand = 'Q'
                    }
                    'q' -> {
                        val cx = currentX + nextNumber()
                        val cy = currentY + nextNumber()
                        currentX += nextNumber()
                        currentY += nextNumber()
                        path.quadTo(cx.toFloat(), cy.toFloat(), currentX.toFloat(), currentY.toFloat())
                        lastControlX = cx
                        lastControlY = cy
                        lastCommand = 'q'
                    }
                    'T' -> {
                        val cx = 2 * currentX - lastControlX
                        val cy = 2 * currentY - lastControlY
                        currentX = nextNumber()
                        currentY = nextNumber()
                        path.quadTo(cx.toFloat(), cy.toFloat(), currentX.toFloat(), currentY.toFloat())
                        lastControlX = cx
                        lastControlY = cy
                        lastCommand = 'T'
                    }
                    't' -> {
                        val cx = 2 * currentX - lastControlX
                        val cy = 2 * currentY - lastControlY
                        currentX += nextNumber()
                        currentY += nextNumber()
                        path.quadTo(cx.toFloat(), cy.toFloat(), currentX.toFloat(), currentY.toFloat())
                        lastControlX = cx
                        lastControlY = cy
                        lastCommand = 't'
                    }
                    'A' -> {
                        // Arc command: rx ry x-axis-rotation large-arc-flag sweep-flag x y
                        val rx = nextNumber()
                        val ry = nextNumber()
                        val xAxisRotation = nextNumber()
                        val largeArcFlag = nextNumber() != 0.0
                        val sweepFlag = nextNumber() != 0.0
                        val x = nextNumber()
                        val y = nextNumber()
                        arcTo(path, currentX, currentY, rx, ry, xAxisRotation, largeArcFlag, sweepFlag, x, y)
                        currentX = x
                        currentY = y
                        lastCommand = 'A'
                    }
                    'a' -> {
                        val rx = nextNumber()
                        val ry = nextNumber()
                        val xAxisRotation = nextNumber()
                        val largeArcFlag = nextNumber() != 0.0
                        val sweepFlag = nextNumber() != 0.0
                        val x = currentX + nextNumber()
                        val y = currentY + nextNumber()
                        arcTo(path, currentX, currentY, rx, ry, xAxisRotation, largeArcFlag, sweepFlag, x, y)
                        currentX = x
                        currentY = y
                        lastCommand = 'a'
                    }
                    'Z', 'z' -> {
                        path.closePath()
                        currentX = pathStartX
                        currentY = pathStartY
                        lastCommand = cmd
                    }
                }
            }

            return path
        }

        // Convert SVG arc to cubic Bezier curves
        private fun arcTo(
            path: GeneralPath,
            x1: Double, y1: Double,
            rx: Double, ry: Double,
            phi: Double,
            largeArc: Boolean,
            sweep: Boolean,
            x2: Double, y2: Double
        ) {
            if (rx == 0.0 || ry == 0.0) {
                path.lineTo(x2.toFloat(), y2.toFloat())
                return
            }

            val rxAbs = kotlin.math.abs(rx)
            val ryAbs = kotlin.math.abs(ry)
            val phiRad = Math.toRadians(phi)
            val cosPhi = kotlin.math.cos(phiRad)
            val sinPhi = kotlin.math.sin(phiRad)

            // Step 1: Compute (x1', y1')
            val dx = (x1 - x2) / 2
            val dy = (y1 - y2) / 2
            val x1p = cosPhi * dx + sinPhi * dy
            val y1p = -sinPhi * dx + cosPhi * dy

            // Correct radii if necessary
            var rxSq = rxAbs * rxAbs
            var rySq = ryAbs * ryAbs
            val x1pSq = x1p * x1p
            val y1pSq = y1p * y1p
            val lambda = x1pSq / rxSq + y1pSq / rySq
            var rxCorrected = rxAbs
            var ryCorrected = ryAbs
            if (lambda > 1) {
                val sqrtLambda = kotlin.math.sqrt(lambda)
                rxCorrected = sqrtLambda * rxAbs
                ryCorrected = sqrtLambda * ryAbs
                rxSq = rxCorrected * rxCorrected
                rySq = ryCorrected * ryCorrected
            }

            // Step 2: Compute (cx', cy')
            val sq = maxOf(0.0, (rxSq * rySq - rxSq * y1pSq - rySq * x1pSq) / (rxSq * y1pSq + rySq * x1pSq))
            val coef = (if (largeArc != sweep) 1 else -1) * kotlin.math.sqrt(sq)
            val cxp = coef * rxCorrected * y1p / ryCorrected
            val cyp = -coef * ryCorrected * x1p / rxCorrected

            // Step 3: Compute (cx, cy)
            val cx = cosPhi * cxp - sinPhi * cyp + (x1 + x2) / 2
            val cy = sinPhi * cxp + cosPhi * cyp + (y1 + y2) / 2

            // Step 4: Compute theta1 and dtheta
            fun angle(ux: Double, uy: Double, vx: Double, vy: Double): Double {
                val n = kotlin.math.sqrt(ux * ux + uy * uy) * kotlin.math.sqrt(vx * vx + vy * vy)
                if (n == 0.0) return 0.0
                var c = (ux * vx + uy * vy) / n
                c = c.coerceIn(-1.0, 1.0)
                val angle = kotlin.math.acos(c)
                return if (ux * vy - uy * vx < 0) -angle else angle
            }

            val theta1 = angle(1.0, 0.0, (x1p - cxp) / rxCorrected, (y1p - cyp) / ryCorrected)
            var dtheta = angle(
                (x1p - cxp) / rxCorrected, (y1p - cyp) / ryCorrected,
                (-x1p - cxp) / rxCorrected, (-y1p - cyp) / ryCorrected
            )

            if (!sweep && dtheta > 0) dtheta -= 2 * Math.PI
            if (sweep && dtheta < 0) dtheta += 2 * Math.PI

            // Approximate arc with cubic Bezier curves
            val segments = kotlin.math.ceil(kotlin.math.abs(dtheta) / (Math.PI / 2)).toInt().coerceAtLeast(1)
            val delta = dtheta / segments

            for (i in 0 until segments) {
                val t1 = theta1 + i * delta
                val t2 = theta1 + (i + 1) * delta

                val alpha = kotlin.math.sin(delta) * (kotlin.math.sqrt(4 + 3 * kotlin.math.tan(delta / 2).let { it * it }) - 1) / 3

                val cosT1 = kotlin.math.cos(t1)
                val sinT1 = kotlin.math.sin(t1)
                val cosT2 = kotlin.math.cos(t2)
                val sinT2 = kotlin.math.sin(t2)

                val ex1 = rxCorrected * cosT1
                val ey1 = ryCorrected * sinT1
                val ex2 = rxCorrected * cosT2
                val ey2 = ryCorrected * sinT2

                val dx1 = -rxCorrected * sinT1
                val dy1 = ryCorrected * cosT1
                val dx2 = -rxCorrected * sinT2
                val dy2 = ryCorrected * cosT2

                val bx1 = ex1 + alpha * dx1
                val by1 = ey1 + alpha * dy1
                val bx2 = ex2 - alpha * dx2
                val by2 = ey2 - alpha * dy2

                // Transform back
                fun transform(px: Double, py: Double): Pair<Double, Double> {
                    return Pair(
                        cosPhi * px - sinPhi * py + cx,
                        sinPhi * px + cosPhi * py + cy
                    )
                }

                val (cp1x, cp1y) = transform(bx1, by1)
                val (cp2x, cp2y) = transform(bx2, by2)
                val (epx, epy) = transform(ex2, ey2)

                path.curveTo(cp1x.toFloat(), cp1y.toFloat(), cp2x.toFloat(), cp2y.toFloat(), epx.toFloat(), epy.toFloat())
            }
        }
    }

    private val iconComponent = IconComponent()
    override val native = iconComponent

    actual var source: Icon? = null
        set(value) {
            field = value
            iconComponent.icon = value
        }

    actual var description: String? = null
        set(value) {
            field = value
            iconComponent.toolTipText = value
        }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)
        val t = theme.theme
        iconComponent.iconColor = t.icon.toAwtColor()
    }
}

