package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*
import platform.UIKit.*

public actual class Slider actual constructor(context: ElementContext) : NativeInteractiveElement(context) {
    override val driverValue: String? get() = sliderDriverValue()
    override val driverActions get() = super.driverActions + sliderDriverActions()
    override val native = UISlider()
    override val control: UIControl get() = native

    private val valueProp = Signal(0.5f)
    public actual val value: MutableReactiveValue<Float> = object : MutableReactiveValue<Float> {
        override fun addListener(listener: () -> Unit): Release {
            return native.onEvent(this@Slider, UIControlEventValueChanged) {
                // Apply step if it's set
                step?.let { stepValue ->
                    if (stepValue > 0) {
                        val currentValue = native.value.toFloat()
                        val steps = ((currentValue - min) / stepValue).toInt()
                        val steppedValue = min + (steps * stepValue)
                        // Update slider position to match stepped value
                        native.value = steppedValue.toFloat()
                    }
                }
                listener()
            }
        }

        override var value: Float
            get() {
                val rawValue = native.value.toFloat()

                // Apply step if it's set
                return step?.let { stepValue ->
                    if (stepValue > 0) {
                        val steps = ((rawValue - min) / stepValue).toInt()
                        min + (steps * stepValue)
                    } else {
                        rawValue
                    }
                } ?: rawValue
            }
            set(value) {
                // Ensure value is within min/max range
                val clampedValue = value.coerceIn(min, max)

                // Apply step if it's set
                val finalValue = step?.let { stepValue ->
                    if (stepValue > 0) {
                        val steps = ((clampedValue - min) / stepValue).toInt()
                        min + (steps * stepValue)
                    } else {
                        clampedValue
                    }
                } ?: clampedValue

                if (native.value.toFloat() != finalValue) {
                    native.value = finalValue
                    // fire change event so reactive listeners are notified on programmatic updates
                    native.sendActionsForControlEvents(UIControlEventValueChanged)
                }
            }
    }

    public actual var min: Float = 0f
        set(value) {
            field = value
            native.minimumValue = value.toFloat()
            // Update current value to ensure it's within new range
            this.value.value = this.value.value
        }

    public actual var max: Float = 1f
        set(value) {
            field = value
            native.maximumValue = value.toFloat()
            // Update current value to ensure it's within new range
            this.value.value = this.value.value
        }

    public actual var step: Float? = null
        set(value) {
            field = value
            // iOS UISlider doesn't have a built-in step property
            // We handle this in the value change listener
        }

    init {
        // UISlider already has built-in VoiceOver support (traits, value announcements).
        native.isAccessibilityElement = true
        setupControl()
        native.minimumValue = min.toFloat()
        native.maximumValue = max.toFloat()
        native.value = valueProp.value.toFloat()

        // Initialize with the property value
        value.value = valueProp.value
    }

    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.nativeApplyTheme(theme)
        val t = theme.theme

        // Apply theme colors to the slider
        native.minimumTrackTintColor = t.foreground.closestColor().toUiColor()
        native.maximumTrackTintColor = t.background.closestColor().toUiColor()
        native.thumbTintColor = t.foreground.closestColor().toUiColor()
    }
}
