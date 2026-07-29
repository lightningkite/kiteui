package com.lightningkite.kiteui.views.direct

import android.view.View
import android.widget.SeekBar
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*

public actual class Slider actual constructor(context: ElementContext) : NativeInteractiveElement(context) {
    override val driverValue: String? get() = sliderDriverValue()
    override val driverActions: Map<String, suspend (List<String>) -> String> get() = super.driverActions + sliderDriverActions()
    private val nativeSeekBar = SeekBar(context.activity)
    override val native: SeekBar = nativeSeekBar

    private val valueProp = Signal(0.5f)
    public actual val value: MutableReactiveValue<Float>
        get() {
            return object : MutableReactiveValue<Float> {
                override fun addListener(listener: () -> Unit): () -> Unit {
                    val seekBarListener = object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                            if (fromUser) {
                                listener()
                            }
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar) {}

                        override fun onStopTrackingTouch(seekBar: SeekBar) {}
                    }

                    nativeSeekBar.setOnSeekBarChangeListener(seekBarListener)

                    return {
                        nativeSeekBar.setOnSeekBarChangeListener(null)
                    }
                }

                override var value: Float
                    get() {
                        val progress = nativeSeekBar.progress
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
                        val normalizedValue = (finalValue - min) / range
                        val progress = (normalizedValue * 1000).toInt()

                        if (nativeSeekBar.progress != progress) {
                            nativeSeekBar.progress = progress
                        }
                    }
            }
        }

    public actual var min: Float = 0f
        set(value) {
            field = value
            // Update current value to ensure it's within new range
            this.value.value = this.value.value
        }

    public actual var max: Float = 1f
        set(value) {
            field = value
            // Update current value to ensure it's within new range
            this.value.value = this.value.value
        }

    public actual var step: Float? = null
        set(value) {
            field = value
            // Android SeekBar doesn't have a built-in step property
            // We handle this in the value setter
        }

    init {
        // Android SeekBar uses integers for progress (0-1000)
        nativeSeekBar.max = 1000
        nativeSeekBar.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE

        // Initialize with the property value
        value.value = valueProp.value
    }

    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.nativeApplyTheme(theme)
        val t = theme.theme

        // Apply theme colors to the slider
        nativeSeekBar.progressTintList = android.content.res.ColorStateList.valueOf(t.foreground.closestColor().colorInt())
        nativeSeekBar.progressBackgroundTintList = android.content.res.ColorStateList.valueOf(t.background.closestColor().colorInt())
        nativeSeekBar.thumbTintList = android.content.res.ColorStateList.valueOf(t.foreground.closestColor().colorInt())
    }
}
