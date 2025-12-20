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
    inner class IconComponent : JComponent() {
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

        // Simple SVG path parser - handles basic path commands
        private fun parseSvgPath(pathData: String): GeneralPath {
            val path = GeneralPath()

            if (pathData.isEmpty()) return path

            val commands = mutableListOf<Char>()
            val numbers = mutableListOf<String>()

            var currentNumber = StringBuilder()
            var lastChar = ' '

            for (char in pathData) {
                when {
                    char in "MLHVCSQTAZmlhvcsqtaz" -> {
                        if (currentNumber.isNotEmpty()) {
                            numbers.add(currentNumber.toString())
                            currentNumber.clear()
                        }
                        commands.add(char)
                    }
                    char in "0123456789.-" -> {
                        // Handle negative numbers that aren't separated by spaces
                        if (char == '-' && currentNumber.isNotEmpty() && lastChar !in "eE") {
                            numbers.add(currentNumber.toString())
                            currentNumber.clear()
                        }
                        currentNumber.append(char)
                    }
                    char in " ,\t\n\r" -> {
                        if (currentNumber.isNotEmpty()) {
                            numbers.add(currentNumber.toString())
                            currentNumber.clear()
                        }
                    }
                }
                lastChar = char
            }

            if (currentNumber.isNotEmpty()) {
                numbers.add(currentNumber.toString())
            }

            var numberIndex = 0
            var currentX = 0.0
            var currentY = 0.0
            var lastControlX = 0.0
            var lastControlY = 0.0
            var pathStartX = 0.0
            var pathStartY = 0.0

            fun nextNumber(): Double {
                if (numberIndex >= numbers.size) return 0.0
                return numbers[numberIndex++].toDoubleOrNull() ?: 0.0
            }

            var commandIndex = 0
            while (commandIndex < commands.size) {
                when (val command = commands[commandIndex++]) {
                    'M' -> {
                        currentX = nextNumber()
                        currentY = nextNumber()
                        pathStartX = currentX
                        pathStartY = currentY
                        path.moveTo(currentX.toFloat(), currentY.toFloat())
                    }
                    'm' -> {
                        currentX += nextNumber()
                        currentY += nextNumber()
                        pathStartX = currentX
                        pathStartY = currentY
                        path.moveTo(currentX.toFloat(), currentY.toFloat())
                    }
                    'L' -> {
                        currentX = nextNumber()
                        currentY = nextNumber()
                        path.lineTo(currentX.toFloat(), currentY.toFloat())
                    }
                    'l' -> {
                        currentX += nextNumber()
                        currentY += nextNumber()
                        path.lineTo(currentX.toFloat(), currentY.toFloat())
                    }
                    'H' -> {
                        currentX = nextNumber()
                        path.lineTo(currentX.toFloat(), currentY.toFloat())
                    }
                    'h' -> {
                        currentX += nextNumber()
                        path.lineTo(currentX.toFloat(), currentY.toFloat())
                    }
                    'V' -> {
                        currentY = nextNumber()
                        path.lineTo(currentX.toFloat(), currentY.toFloat())
                    }
                    'v' -> {
                        currentY += nextNumber()
                        path.lineTo(currentX.toFloat(), currentY.toFloat())
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
                    }
                    'S' -> {
                        val x2 = nextNumber()
                        val y2 = nextNumber()
                        val x1 = 2 * currentX - lastControlX
                        val y1 = 2 * currentY - lastControlY
                        currentX = nextNumber()
                        currentY = nextNumber()
                        path.curveTo(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), currentX.toFloat(), currentY.toFloat())
                        lastControlX = x2
                        lastControlY = y2
                    }
                    's' -> {
                        val x2 = currentX + nextNumber()
                        val y2 = currentY + nextNumber()
                        val x1 = 2 * currentX - lastControlX
                        val y1 = 2 * currentY - lastControlY
                        currentX += nextNumber()
                        currentY += nextNumber()
                        path.curveTo(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), currentX.toFloat(), currentY.toFloat())
                        lastControlX = x2
                        lastControlY = y2
                    }
                    'Q' -> {
                        val cx = nextNumber()
                        val cy = nextNumber()
                        currentX = nextNumber()
                        currentY = nextNumber()
                        path.quadTo(cx.toFloat(), cy.toFloat(), currentX.toFloat(), currentY.toFloat())
                        lastControlX = cx
                        lastControlY = cy
                    }
                    'q' -> {
                        val cx = currentX + nextNumber()
                        val cy = currentY + nextNumber()
                        currentX += nextNumber()
                        currentY += nextNumber()
                        path.quadTo(cx.toFloat(), cy.toFloat(), currentX.toFloat(), currentY.toFloat())
                        lastControlX = cx
                        lastControlY = cy
                    }
                    'T' -> {
                        val cx = 2 * currentX - lastControlX
                        val cy = 2 * currentY - lastControlY
                        currentX = nextNumber()
                        currentY = nextNumber()
                        path.quadTo(cx.toFloat(), cy.toFloat(), currentX.toFloat(), currentY.toFloat())
                        lastControlX = cx
                        lastControlY = cy
                    }
                    't' -> {
                        val cx = 2 * currentX - lastControlX
                        val cy = 2 * currentY - lastControlY
                        currentX += nextNumber()
                        currentY += nextNumber()
                        path.quadTo(cx.toFloat(), cy.toFloat(), currentX.toFloat(), currentY.toFloat())
                        lastControlX = cx
                        lastControlY = cy
                    }
                    'Z', 'z' -> {
                        path.closePath()
                        currentX = pathStartX
                        currentY = pathStartY
                    }
                    // Note: Arc (A/a) commands are complex and not fully implemented here
                    // For basic Material icons, the above commands should suffice
                }
            }

            return path
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

