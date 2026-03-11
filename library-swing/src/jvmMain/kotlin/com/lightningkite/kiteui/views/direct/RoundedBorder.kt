package com.lightningkite.kiteui.views.direct

import java.awt.Component
import java.awt.Graphics

/**
 * Custom border that draws rounded rectangles with background and outline.
 * Used by all views that draw backgrounds with rounded corners.
 *
 * @param borderColor The color of the outline/border
 * @param borderThickness The thickness of the outline in pixels
 * @param cornerRadius The corner radius in pixels
 * @param backgroundColor The background color to fill
 * @param padding Additional padding insets (independent of border thickness)
 */
internal class RoundedBorder(
    private val borderColor: java.awt.Color,
    private val borderThickness: Int,
    private val cornerRadius: Int,
    private val backgroundColor: java.awt.Color,
    private val padding: java.awt.Insets = java.awt.Insets(0, 0, 0, 0)
) : javax.swing.border.AbstractBorder() {

    override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
        val g2 = g.create() as java.awt.Graphics2D
        try {
            g2.setRenderingHint(
                java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON
            )

            // Paint background with rounded corners
            g2.color = backgroundColor
            g2.fillRoundRect(
                x + borderThickness / 2,
                y + borderThickness / 2,
                width - borderThickness,
                height - borderThickness,
                cornerRadius * 2,
                cornerRadius * 2
            )

            // Paint border with rounded corners
            if (borderThickness > 0) {
                g2.color = borderColor
                g2.stroke = java.awt.BasicStroke(borderThickness.toFloat())
                g2.drawRoundRect(
                    x + borderThickness / 2,
                    y + borderThickness / 2,
                    width - borderThickness,
                    height - borderThickness,
                    cornerRadius * 2,
                    cornerRadius * 2
                )
            }
        } finally {
            g2.dispose()
        }
    }

    override fun getBorderInsets(c: Component): java.awt.Insets {
        // Return the padding insets (fixed, independent of border thickness to prevent size changes)
        return java.awt.Insets(padding.top, padding.left, padding.bottom, padding.right)
    }

    override fun getBorderInsets(c: Component, insets: java.awt.Insets): java.awt.Insets {
        // Return the padding insets (fixed, independent of border thickness to prevent size changes)
        insets.top = padding.top
        insets.left = padding.left
        insets.bottom = padding.bottom
        insets.right = padding.right
        return insets
    }
}
