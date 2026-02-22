package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.toAwt
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.core.MutableReactiveValue
import com.lightningkite.reactive.core.Signal
import javax.swing.JCheckBox

actual class Checkbox actual constructor(context: RContext) : RView(context) {
    override val native = JCheckBox()

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

        // Apply foreground color for the checkmark/icon
        val iconColor = (t.iconOverride ?: t.foreground).closestColor()
        val alphaMultiplier = if (native.isSelected) 1.0f else 0.75f
        native.foreground = iconColor.withAlpha((iconColor.alpha * alphaMultiplier).coerceIn(0f, 1f)).toAwt()

        // Apply background color
        val backgroundColor = t.background.closestColor()
        native.background = backgroundColor.toAwt()

        // Apply font styling
        val font = native.font
        val style = when {
            t.font.bold && t.font.italic -> java.awt.Font.BOLD or java.awt.Font.ITALIC
            t.font.bold -> java.awt.Font.BOLD
            t.font.italic -> java.awt.Font.ITALIC
            else -> java.awt.Font.PLAIN
        }
        native.font = font.deriveFont(style, t.font.size.value.toFloat())

        native.isOpaque = theme.drawBackground
    }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if (!enabled) t = t[DisabledSemantic]
        return super.applyState(t)
    }
}
