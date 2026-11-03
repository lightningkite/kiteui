package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*
import platform.UIKit.*
import com.lightningkite.kiteui.views.AiDriver

public actual class Slider actual constructor(context: ElementContext) : NativeInteractiveElement(context) {
    override val driverValue: String? get() = sliderDriverValue()
    override val driverActions: AiDriver.Actions get() = super.driverActions + sliderDriverActions()
    override val native: UISlider = UISlider()
    override val control: UIControl get() = native

    private var current = 0.5f

    public actual val value: MutableReactiveValue<Float> = object : MutableReactiveValue<Float>, BaseListenable() {
        override var value: Float
            get() = current
            set(value) {
                val held = sliderValue(value, min, max, step)
                // Always placed, since a drag leaves the thumb between steps and a new range moves it.
                native.value = held
                if (held == current) return
                current = held
                invokeAllListeners()
            }
    }

    // UISlider can't hold a max below its min; that counts as the min, as on the web.
    public actual var min: Float = 0f
        set(value) {
            field = value
            native.minimumValue = value
            native.maximumValue = max.coerceAtLeast(value)
            this.value.value = this.value.value
        }

    public actual var max: Float = 1f
        set(value) {
            field = value
            native.maximumValue = value.coerceAtLeast(min)
            this.value.value = this.value.value
        }

    public actual var step: Float? = null
        set(value) {
            field = value.validSliderStep()
            this.value.value = this.value.value
        }

    init {
        // UISlider already has built-in VoiceOver support (traits, value announcements).
        native.isAccessibilityElement = true
        setupControl()
        native.minimumValue = min
        native.maximumValue = max
        native.onEvent(this, UIControlEventValueChanged) { value.value = native.value }
        value.value = current
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
