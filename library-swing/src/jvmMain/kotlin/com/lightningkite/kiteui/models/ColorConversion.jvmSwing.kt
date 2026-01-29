package com.lightningkite.kiteui.models

import java.awt.Color as AwtColor

/**
 * Converts a KiteUI Color to a java.awt.Color.
 * KiteUI uses 0.0-1.0 float values for color components,
 * while AWT uses 0-255 integer values.
 */
fun Color.toAwt(): AwtColor {
    return AwtColor(
        red.coerceIn(0f, 1f),
        green.coerceIn(0f, 1f),
        blue.coerceIn(0f, 1f),
        alpha.coerceIn(0f, 1f)
    )
}

/**
 * Converts a KiteUI Paint to a java.awt.Color.
 * For non-Color paints (gradients, etc.), this will use the closest solid color.
 */
fun Paint.toAwtColor(): AwtColor {
    val color = this.closestColor()
    return AwtColor(
        (color.red * 255).toInt().coerceIn(0, 255),
        (color.green * 255).toInt().coerceIn(0, 255),
        (color.blue * 255).toInt().coerceIn(0, 255),
        (color.alpha * 255).toInt().coerceIn(0, 255)
    )
}
