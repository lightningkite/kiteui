package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.toAwt
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.core.MutableReactiveValue
import com.lightningkite.reactive.core.Signal
import javax.swing.JRadioButton

actual class RadioButton actual constructor(context: RContext) : RView(context) {
    override val driverValue: String? get() = radioDriverValue()
    override val driverActions get() = super.driverActions + radioDriverActions()
    override val native = JRadioButton()

    actual val checked: MutableReactiveValue<Boolean> = Signal(false).also { signal ->
        native.addItemListener {
            signal.value = native.isSelected
        }
        signal.addListener {
            if (native.isSelected != signal.value) {
                native.isSelected = signal.value
            }
        }
    }

    actual var enabled: Boolean
        get() = native.isEnabled
        set(value) {
            native.isEnabled = value
            refreshTheming()
        }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)
        val t = theme.theme

        // Apply foreground color for the radio button icon
        val iconColor = t.let { it.iconOverride ?: it.foreground }.closestColor()

        // JRadioButton doesn't support direct tinting in the same way as Android
        // The foreground color controls the check mark color
        native.foreground = iconColor.toAwt()

        // Set background to transparent to match theme expectations
        native.isOpaque = false
    }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if (!enabled) t = t[DisabledSemantic]
        return super.applyState(t)
    }
}
