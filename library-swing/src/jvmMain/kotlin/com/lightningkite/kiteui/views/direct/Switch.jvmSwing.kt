package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.ImportantSemantic
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.toAwt
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.core.*
import java.awt.Color as AwtColor
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JToggleButton

actual class Switch actual constructor(context: RContext) : RView(context) {
    override val native = object : JToggleButton() {
        override fun paintComponent(g: Graphics) {
            val g2d = g as Graphics2D
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            val width = width
            val height = height
            val trackHeight = height * 0.6f
            val trackY = (height - trackHeight) / 2
            val thumbSize = trackHeight * 0.8f
            val trackWidth = width.toFloat()

            // Draw track
            val trackColor = if (isSelected) {
                foreground.brighter()
            } else {
                background.darker()
            }
            g2d.color = trackColor
            g2d.fillRoundRect(
                0,
                trackY.toInt(),
                trackWidth.toInt(),
                trackHeight.toInt(),
                trackHeight.toInt(),
                trackHeight.toInt()
            )

            // Draw thumb
            val thumbX = if (isSelected) {
                trackWidth - thumbSize - (trackHeight - thumbSize) / 2
            } else {
                (trackHeight - thumbSize) / 2
            }
            val thumbY = trackY + (trackHeight - thumbSize) / 2

            g2d.color = if (isSelected) foreground else background.brighter()
            g2d.fillOval(
                thumbX.toInt(),
                thumbY.toInt(),
                thumbSize.toInt(),
                thumbSize.toInt()
            )
        }
    }.apply {
        text = null
        isBorderPainted = false
        isFocusPainted = false
        isContentAreaFilled = false
    }

    actual var enabled: Boolean
        get() = native.isEnabled
        set(value) {
            native.isEnabled = value
            refreshTheming()
        }

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

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)
        val t = theme.theme

        // Unchecked state: use highlighted background
        val uncheckedColor = t.background.closestColor().highlight(0.3f)
        native.background = AwtColor(
            uncheckedColor.red,
            uncheckedColor.green,
            uncheckedColor.blue,
            uncheckedColor.alpha
        )

        // Checked state: use important semantic background
        val checkedColor = t[ImportantSemantic].theme.background.closestColor()
        native.foreground = checkedColor.toAwt()

        native.repaint()
    }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if (!enabled) t = t[DisabledSemantic]
        return super.applyState(t)
    }
}
