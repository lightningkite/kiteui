package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*
import com.lightningkite.kiteui.views.AiDriver

public actual class Slider actual constructor(context: ElementContext) : NativeInteractiveElement(context) {
    override val driverValue: String? get() = sliderDriverValue()
    override val driverActions: AiDriver.Actions get() = super.driverActions + sliderDriverActions()
    init {
        native.tag = "input"
        native.attributes.type = "range"
        native.classes.add("slider")
        // Properties, not attributes: an element created later gets its properties in the order they were first
        // set, and all of them before its attributes. Set here, before the value, the range is in place when the
        // browser takes the value; as attributes it would arrive after, and the browser would re-fit the value
        // (10 in 0.5..10 by 0.5 became 9.5, via the interim default step of 1).
        native.attributes.minString = "0"
        native.attributes.maxString = "1"
        native.attributes.step = "any"
        native.style.width = "100%"
    }

    private val valueProp = Signal(0.5f)
    public actual val value: MutableReactiveValue<Float> = native.vprop(
        "input",
        { attributes.valueString?.toFloatOrNull() ?: 0.5f },
        { newValue -> 
            // Ensure value is within min/max range
            val clampedValue = newValue.coerceIn(min, max)

            // Apply step if it's set
            val finalValue = step?.let { stepValue ->
                if (stepValue > 0) {
                    val steps = ((clampedValue - min) / stepValue).toInt()
                    min + (steps * stepValue)
                } else {
                    clampedValue
                }
            } ?: clampedValue

            attributes.valueString = finalValue.toString()
        }
    )

    public actual var min: Float = 0f
        set(value) {
            field = value
            native.attributes.minString = value.toString()
            // Update current value to ensure it's within new range
            this.value.value = this.value.value
        }

    public actual var max: Float = 1f
        set(value) {
            field = value
            native.attributes.maxString = value.toString()
            // Update current value to ensure it's within new range
            this.value.value = this.value.value
        }

    public actual var step: Float? = null
        set(value) {
            field = value.validSliderStep()
            native.attributes.step = field?.toString() ?: "any"
        }

    init {
        // Initialize with the property value
        value.value = valueProp.value
    }
}
