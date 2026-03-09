package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import platform.UIKit.*

actual class Slider actual constructor(context: RContext) : RView(context) {
    override val native = UISlider()

    private val valueProp = Signal(0.5f)
    actual val value: MutableReactiveValue<Float>
        get() {
            return object : MutableReactiveValue<Float> {
                override fun addListener(listener: () -> Unit): () -> Unit {
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
                        }
                    }
            }
        }

    actual var min: Float = 0f
        set(value) {
            field = value
            native.minimumValue = value.toFloat()
            // Update current value to ensure it's within new range
            this.value.value = this.value.value
        }

    actual var max: Float = 1f
        set(value) {
            field = value
            native.maximumValue = value.toFloat()
            // Update current value to ensure it's within new range
            this.value.value = this.value.value
        }

    actual var step: Float? = null
        set(value) {
            field = value
            // iOS UISlider doesn't have a built-in step property
            // We handle this in the value change listener
        }

    init {
        native.minimumValue = min.toFloat()
        native.maximumValue = max.toFloat()
        native.value = valueProp.value.toFloat()

        // Initialize with the property value
        value.value = valueProp.value
    }

    actual var enabled: Boolean
        get() = native.enabled
        set(value) {
            native.enabled = value
            refreshTheming()
        }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if (!enabled) t = t[DisabledSemantic]
        return super.applyState(t)
    }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)
        val t = theme.theme

        // Apply theme colors to the slider
        native.minimumTrackTintColor = t.foreground.closestColor().toUiColor()
        native.maximumTrackTintColor = t.background.closestColor().toUiColor()
        native.thumbTintColor = t.foreground.closestColor().toUiColor()
    }

    // by Claude
    override var accessibilityValue: String?
        get() = readNonNullValue(value)
        set(v) = writeNonNullValue(value, v) { it.toFloatOrNull() ?: throw IllegalArgumentException("Cannot parse '$it' as Float") }
    override val accessibilityActions get() = SET_VALUE_ACTIONS
    override fun performAccessibilityAction(action: String, value: String?) =
        performNonNullSetValueAction(this.value, { it.toFloatOrNull() ?: throw IllegalArgumentException("Cannot parse '$it' as Float") }, action, value) { a, v -> super.performAccessibilityAction(a, v) }
}
