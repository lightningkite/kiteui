package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.core.*

/**
 * A slider component that allows users to select a value from a continuous range.
 */
expect class Slider(context: ElementContext) : RView {
    /**
     * Whether the slider is enabled or disabled.
     */
    var enabled: Boolean
    
    /**
     * The current value of the slider, between 0.0 and 1.0.
     */
    val value: MutableReactiveValue<Float>
    
    /**
     * The minimum value of the slider.
     */
    var min: Float
    
    /**
     * The maximum value of the slider.
     */
    var max: Float
    
    /**
     * The step size for the slider. If null, the slider is continuous.
     */
    var step: Float?
}

/**
 * Sets the value range for the slider.
 */
fun Slider.range(min: Float, max: Float, step: Float? = null) {
    this.min = min
    this.max = max
    this.step = step
}

/**
 * Sets the value range for the slider using integers.
 */
fun Slider.range(min: Int, max: Int, step: Int? = null) {
    this.min = min.toFloat()
    this.max = max.toFloat()
    this.step = step?.toFloat()
}