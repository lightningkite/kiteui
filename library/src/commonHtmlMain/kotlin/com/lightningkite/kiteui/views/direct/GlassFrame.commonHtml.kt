package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.*

/**
 * JavaScript implementation of GlassFrame.
 * Uses CSS backdrop-filter to create a blurred background effect.
 */
actual class GlassFrame actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "div"
        native.style.lineHeight = "0px !important"
    }

    actual var blurStrength: Double = 10.0
        set(value) {
            field = value
            updateBlur()
        }

    init {
        updateBlur()
    }

    private fun updateBlur() {
        // Apply backdrop-filter for blur effect
        // The blur radius is calculated based on blurStrength
        val blurRadius = "${blurStrength}px"
        native.setStyleProperty("backdrop-filter", "blur($blurRadius)")
        // For Safari support
        native.setStyleProperty("-webkit-backdrop-filter", "blur($blurRadius)")
    }

    override fun internalAddChild(index: Int, view: RView) {
        super.internalAddChild(index, view)
        Frame.internalAddChildStack(this, index, view)
    }
}
