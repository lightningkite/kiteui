package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.toAwt
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.core.MutableReactiveValue
import javax.swing.JSlider
import javax.swing.event.ChangeListener

actual class Slider actual constructor(context: RContext) : RView(context) {
    override val native = JSlider(0, 1000, 500)

    private var suppressChangeEvents = false

    actual val value: MutableReactiveValue<Float>
        get() {
            return object : MutableReactiveValue<Float> {
                private var listener: ChangeListener? = null

                override fun addListener(listener: () -> Unit): () -> Unit {
                    val changeListener = ChangeListener {
                        if (!suppressChangeEvents && !native.valueIsAdjusting) {
                            listener()
                        }
                    }
                    this.listener = changeListener
                    native.addChangeListener(changeListener)

                    return {
                        native.removeChangeListener(changeListener)
                        this.listener = null
                    }
                }

                override var value: Float
                    get() {
                        val progress = native.value
                        val range = max - min
                        val rawValue = min + (progress / 1000f) * range

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

                        // Convert to progress (0-1000)
                        val range = max - min
                        val normalizedValue = if (range > 0) {
                            (finalValue - min) / range
                        } else {
                            0f
                        }
                        val progress = (normalizedValue * 1000).toInt().coerceIn(0, 1000)

                        if (native.value != progress) {
                            suppressChangeEvents = true
                            native.value = progress
                            suppressChangeEvents = false
                        }
                    }
            }
        }

    actual var min: Float = 0f
        set(value) {
            field = value
            // Update current value to ensure it's within new range
            this.value.value = this.value.value
        }

    actual var max: Float = 1f
        set(value) {
            field = value
            // Update current value to ensure it's within new range
            this.value.value = this.value.value
        }

    actual var step: Float? = null
        set(value) {
            field = value
            // Update current value to apply step
            this.value.value = this.value.value
        }

    actual var enabled: Boolean
        get() = native.isEnabled
        set(value) {
            native.isEnabled = value
            refreshTheming()
        }

    init {
        // Initialize with default value (middle of range)
        value.value = 0.5f
    }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if (!enabled) t = t[DisabledSemantic]
        return super.applyState(t)
    }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)
        val t = theme.theme

        // Apply foreground color to the slider
        val foregroundColor = t.foreground.closestColor()
        native.foreground = foregroundColor.toAwt()

        // Apply background color
        val backgroundColor = t.background.closestColor()
        native.background = backgroundColor.toAwt()
    }
}
