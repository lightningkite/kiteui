package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.models.toAwt
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.px
import java.awt.*
import java.awt.event.ActionListener
import javax.swing.JComponent
import javax.swing.Timer
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

actual typealias ActivityIndicator = ActivityIndicatorImpl

class ActivityIndicatorImpl(context: RContext) : RView(context) {
    private var spinnerColor: Color = Color.GRAY

    override val native = SpinnerComponent().apply {
        val defaultSize = 24
        minimumSize = Dimension(defaultSize, defaultSize)
        preferredSize = Dimension(defaultSize, defaultSize)
        maximumSize = Dimension(defaultSize, defaultSize)
    }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)
        val t = theme.theme

        // Apply foreground color for the spinner
        spinnerColor = t.foreground.closestColor().toAwt()
        native.spinnerColor = spinnerColor

        // Update size constraints based on theme
        val size = (1.rem.px).roundToInt()
        val dimension = Dimension(size, size)
        native.minimumSize = dimension
        native.preferredSize = dimension
        native.maximumSize = dimension
    }

    /**
     * Custom circular spinner component with smooth animation
     */
    inner class SpinnerComponent : JComponent() {
        var spinnerColor: Color = Color.GRAY
        private var angle: Double = 0.0
        private val numSegments = 12
        private val timer: Timer

        init {
            isOpaque = false
            // Animate at 60fps
            timer = Timer(16, ActionListener {
                angle = (angle + 6) % 360
                repaint()
            })
            timer.start()
        }

        override fun contains(x: Int, y: Int): Boolean = false // Click-through

        override fun paintComponent(g: Graphics) {
            val g2d = g as Graphics2D
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            val size = min(width, height)
            val centerX = width / 2
            val centerY = height / 2
            val outerRadius = size / 2 - 2
            val innerRadius = (outerRadius * 0.5).toInt()
            val dotRadius = ((outerRadius - innerRadius) / 3).coerceAtLeast(2)

            // Draw spinning dots with varying opacity
            for (i in 0 until numSegments) {
                val segmentAngle = Math.toRadians(angle + (i * 360.0 / numSegments))
                val x = (centerX + (outerRadius - dotRadius) * cos(segmentAngle)).toInt()
                val y = (centerY + (outerRadius - dotRadius) * sin(segmentAngle)).toInt()

                // Calculate alpha based on position (creates trailing effect)
                val alpha = (255 * (numSegments - i) / numSegments).coerceIn(40, 255)
                g2d.color = Color(
                    spinnerColor.red,
                    spinnerColor.green,
                    spinnerColor.blue,
                    alpha
                )

                g2d.fillOval(x - dotRadius, y - dotRadius, dotRadius * 2, dotRadius * 2)
            }
        }

        override fun removeNotify() {
            super.removeNotify()
            timer.stop()
        }

        override fun addNotify() {
            super.addNotify()
            if (!timer.isRunning) {
                timer.start()
            }
        }
    }
}
