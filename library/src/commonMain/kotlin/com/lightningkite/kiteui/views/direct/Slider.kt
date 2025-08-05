package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

/**
 * A slider component that allows users to select a value from a continuous range.
 */
public expect class Slider(context: RContext) : RView {
    /**
     * Whether the slider is enabled or disabled.
     */
    public var enabled: Boolean
    
    /**
     * The current value of the slider, between 0.0 and 1.0.
     */
    public val value: MutableReactiveValue<Float>
    
    /**
     * The minimum value of the slider.
     */
    public var min: Float
    
    /**
     * The maximum value of the slider.
     */
    public var max: Float
    
    /**
     * The step size for the slider. If null, the slider is continuous.
     */
    public var step: Float?
}

/**
 * Sets the value range for the slider.
 */
public fun Slider.range(min: Float, max: Float, step: Float? = null) {
    this.min = min
    this.max = max
    this.step = step
}

/**
 * Sets the value range for the slider using integers.
 */
public fun Slider.range(min: Int, max: Int, step: Int? = null) {
    this.min = min.toFloat()
    this.max = max.toFloat()
    this.step = step?.toFloat()
}