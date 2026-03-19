package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*

actual class Slider actual constructor(context: ElementContext) : NativeElement(context) {
    override val driverValue: String? get() = sliderDriverValue()
    override val driverActions get() = super.driverActions + sliderDriverActions()
    init {
        native.tag = "input"
        native.attributes.type = "range"
        native.classes.add("slider")
        native.setAttribute("min", "0")
        native.setAttribute("max", "1")
        native.setAttribute("step", "any")
        native.style.width = "100%"
    }

    private val valueProp = Signal(0.5f)
    actual val value: MutableReactiveValue<Float> = native.vprop(
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

    actual var min: Float = 0f
        set(value) {
            field = value
            native.setAttribute("min", value.toString())
            // Update current value to ensure it's within new range
            this.value.value = this.value.value
        }

    actual var max: Float = 1f
        set(value) {
            field = value
            native.setAttribute("max", value.toString())
            // Update current value to ensure it's within new range
            this.value.value = this.value.value
        }

    actual var step: Float? = null
        set(value) {
            field = value
            native.setAttribute("step", value?.toString() ?: "any")
        }

    init {
        // Initialize with the property value
        value.value = valueProp.value
    }

    actual var enabled: Boolean
        get() = !(native.attributes.disabled ?: false)
        set(value) {
            native.attributes.disabled = !value
        }
}
