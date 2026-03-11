package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.models.toAwt
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.core.MutableReactiveValue
import com.lightningkite.reactive.core.Signal
import javax.swing.JRadioButton

actual class RadioToggleButton actual constructor(context: RContext) : RView(context) {
    override val native = JRadioButton()

    actual val checked: MutableReactiveValue<Boolean> = Signal(false).also { signal ->
        native.addItemListener {
            signal.value = native.isSelected
        }
        signal.addListener {
            if (native.isSelected != signal.value) {
                native.isSelected = signal.value
            }
            refreshTheming()
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

        // Apply foreground color
        val foregroundColor = t.foreground.closestColor()
        native.foreground = foregroundColor.toAwt()

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

        // Apply border for outline if needed
        if (theme.drawBackground) {
            val outlineColor = t.outline.closestColor()
            val borderColor = outlineColor.toAwt()
            val borderThickness = t.outlineWidth.value.toInt().coerceAtLeast(0)
            if (borderThickness > 0) {
                native.border = javax.swing.BorderFactory.createLineBorder(borderColor, borderThickness)
            } else {
                native.border = null
            }
        } else {
            native.border = null
        }

        native.isOpaque = theme.drawBackground
    }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme[ClickableSemantic]
        if (checked.value) t = t[SelectedSemantic]
        else t = t[UnselectedSemantic]
        if (!enabled) t = t[DisabledSemantic]
        return super.applyState(t)
    }
}
