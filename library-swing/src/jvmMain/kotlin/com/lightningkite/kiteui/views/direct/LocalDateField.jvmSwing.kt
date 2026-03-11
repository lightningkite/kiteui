package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RViewWithAction
import com.lightningkite.reactive.core.MutableReactiveValue
import com.lightningkite.reactive.core.Signal
import kotlinx.datetime.*
import org.jdesktop.swingx.JXDatePicker
import java.awt.Color as AwtColor
import java.awt.Font as AwtFont
import java.time.ZoneId
import java.util.Date

/**
 * JVM Swing implementation of LocalDateField using JXDatePicker from SwingX.
 *
 * Features:
 * - Calendar-based date picker with visual date selection
 * - Reactive two-way binding with kotlinx.datetime.LocalDate
 * - Automatic date format conversion between java.util.Date and kotlinx.datetime.LocalDate
 * - Full theme support (colors, fonts, borders)
 * - Enabled/disabled state with semantic theming
 * - Action support (triggers when date is selected)
 */
actual class LocalDateField actual constructor(context: RContext) : RViewWithAction(context) {
    private var updatingFromSignal = false
    private var updatingFromNative = false

    override val native = JXDatePicker().apply {
        // Set up the date picker
        setFormats("yyyy-MM-dd") // Default format
    }

    actual val content: MutableReactiveValue<LocalDate?> = Signal<LocalDate?>(null).also { signal ->
        // Listen to native date picker changes
        native.addActionListener {
            if (!updatingFromSignal) {
                updatingFromNative = true
                val selectedDate = native.date
                signal.value = selectedDate?.let { date ->
                    val instant = date.toInstant()
                    val javaLocalDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
                    LocalDate(javaLocalDate.year, javaLocalDate.month.value, javaLocalDate.dayOfMonth)
                }
                updatingFromNative = false

                // Trigger action if set
                action?.startAction(this@LocalDateField)
            }
        }

        // Listen to signal changes (programmatic updates)
        signal.addListener {
            if (!updatingFromNative) {
                updatingFromSignal = true
                val kotlinxDate = signal.value
                native.date = kotlinxDate?.let { kDate ->
                    val javaLocalDate = java.time.LocalDate.of(kDate.year, kDate.monthNumber, kDate.dayOfMonth)
                    val instant = javaLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                    Date.from(instant)
                }
                updatingFromSignal = false
            }
        }
    }

    actual var range: ClosedRange<LocalDate>? = null
        set(value) {
            field = value
            // JXDatePicker doesn't have built-in range support, but we could add validation
            // For now, we'll just store the range and could add validation in the listener if needed
        }

    var enabled: Boolean
        get() = native.isEnabled
        set(value) {
            native.isEnabled = value
            refreshTheming()
        }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)
        val t = theme.theme

        // Apply foreground color to the editor component
        val foregroundColor = t.foreground.closestColor()
        val fgColor = AwtColor(
            (foregroundColor.red * 255).toInt().coerceIn(0, 255),
            (foregroundColor.green * 255).toInt().coerceIn(0, 255),
            (foregroundColor.blue * 255).toInt().coerceIn(0, 255),
            (foregroundColor.alpha * 255).toInt().coerceIn(0, 255)
        )
        native.editor.foreground = fgColor

        // Apply background color
        val backgroundColor = t.background.closestColor()
        val bgColor = AwtColor(
            (backgroundColor.red * 255).toInt().coerceIn(0, 255),
            (backgroundColor.green * 255).toInt().coerceIn(0, 255),
            (backgroundColor.blue * 255).toInt().coerceIn(0, 255),
            (backgroundColor.alpha * 255).toInt().coerceIn(0, 255)
        )
        native.editor.background = bgColor
        native.background = bgColor

        // Apply font styling
        val fontStyle = t.font
        var awtFontStyle = AwtFont.PLAIN
        if (fontStyle.italic) {
            awtFontStyle = awtFontStyle or AwtFont.ITALIC
        }
        if (fontStyle.weight >= 700) {
            awtFontStyle = awtFontStyle or AwtFont.BOLD
        }

        val fontSize = fontStyle.size.value.toFloat()
        native.editor.font = fontStyle.font.deriveFont(awtFontStyle, fontSize)

        // Apply border if needed
        if (theme.drawBackground) {
            val outlineColor = t.outline.closestColor()
            val borderColor = AwtColor(
                (outlineColor.red * 255).toInt().coerceIn(0, 255),
                (outlineColor.green * 255).toInt().coerceIn(0, 255),
                (outlineColor.blue * 255).toInt().coerceIn(0, 255),
                (outlineColor.alpha * 255).toInt().coerceIn(0, 255)
            )
            val borderThickness = t.outlineWidth.value.toInt().coerceAtLeast(1)
            native.border = javax.swing.BorderFactory.createLineBorder(borderColor, borderThickness)
        } else {
            native.border = null
        }
    }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme[ClickableSemantic]
        if (!enabled) t = t[DisabledSemantic]
        return super.applyState(t)
    }
}
