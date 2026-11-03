package com.lightningkite.kiteui.views.direct

import android.view.View
import android.widget.SeekBar
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*
import com.lightningkite.kiteui.views.AiDriver
import kotlin.math.roundToInt

public actual class Slider actual constructor(context: ElementContext) : NativeInteractiveElement(context) {
    override val driverValue: String? get() = sliderDriverValue()
    override val driverActions: AiDriver.Actions get() = super.driverActions + sliderDriverActions()
    private val nativeSeekBar = SeekBar(context.activity)
    override val native: SeekBar = nativeSeekBar

    // The value is kept here rather than read back from the SeekBar, whose progress has only 1000 positions.
    private var current = 0.5f

    // A stable val backed by BaseListenable, so every addListener() call multicasts off of one
    // underlying SeekBar listener instead of each call replacing SeekBar's single listener slot.
    public actual val value: MutableReactiveValue<Float> = object : MutableReactiveValue<Float>, BaseListenable() {
        private val seekBarListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) value = min + progress / 1000f * (max - min)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        }

        override fun activate() { nativeSeekBar.setOnSeekBarChangeListener(seekBarListener) }
        override fun deactivate() { nativeSeekBar.setOnSeekBarChangeListener(null) }

        override var value: Float
            get() = current
            set(value) {
                val held = sliderValue(value, min, max, step)
                // Always placed, since a new range moves the thumb even when the value stays.
                val range = max - min
                nativeSeekBar.progress = if (range > 0f) ((held - min) / range * 1000).roundToInt() else 0
                if (held == current) return
                current = held
                invokeAllListeners()
            }
    }

    public actual var min: Float = 0f
        set(value) {
            field = value
            this.value.value = this.value.value
        }

    public actual var max: Float = 1f
        set(value) {
            field = value
            this.value.value = this.value.value
        }

    public actual var step: Float? = null
        set(value) {
            field = value.validSliderStep()
            this.value.value = this.value.value
        }

    init {
        // Android SeekBar uses integers for progress (0-1000)
        nativeSeekBar.max = 1000
        nativeSeekBar.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        value.value = current
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
