package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.*
import platform.UIKit.*
import kotlinx.cinterop.*
import platform.CoreGraphics.*

/**
 * iOS implementation of GlassFrame.
 * Uses a custom GlassFrameLayout that extends UIVisualEffectView to create a blurred background effect.
 */
actual class GlassFrame actual constructor(context: RContext) : RView(context) {

    // Create a GlassFrameLayout for our content
    private val glassFrameLayout = GlassFrameLayout()

    // Use the glassFrameLayout as our native view
    override val native: UIView = glassFrameLayout

    // Implement the blurStrength property
    actual var blurStrength: Double = 0.5
        set(value) {
            field = value
            updateBlurEffect(value)
        }

    init {
        // Enable user interaction
        glassFrameLayout.userInteractionEnabled = true

        // Set initial blur effect
        updateBlurEffect(blurStrength)
    }

    // Update the blur effect based on the blur strength
    private fun updateBlurEffect(strength: Double) {
        // Create a new blur effect with the appropriate style based on strength
        val style = when {
            strength <= 0.3 -> UIBlurEffectStyle.UIBlurEffectStyleExtraLight
            strength <= 0.7 -> UIBlurEffectStyle.UIBlurEffectStyleLight
            else -> UIBlurEffectStyle.UIBlurEffectStyleRegular
        }

        // Create and set the blur effect
        val newBlurEffect = UIBlurEffect.effectWithStyle(style)
        glassFrameLayout.effect = newBlurEffect
    }
}
