package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.locale.renderToString
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RViewWithAction
import com.lightningkite.reactive.core.MutableReactiveValue
import com.lightningkite.reactive.core.Signal
import kotlinx.datetime.*
import java.awt.Color as AwtColor
import java.awt.Font as AwtFont
import java.util.Calendar
import java.util.Date
import javax.swing.*
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

actual class LocalTimeField actual constructor(context: RContext) : RViewWithAction(context) {
    private val spinner = JSpinner()
    override val native = spinner

    private var updatingFromSignal = false
    private var updatingFromSpinner = false

    // Use SpinnerDateModel for proper JSpinner.DateEditor compatibility
    private val dateModel = SpinnerDateModel().apply {
        calendarField = Calendar.MINUTE
    }

    actual val content: MutableReactiveValue<LocalTime?> = Signal<LocalTime?>(null).also { signal ->
        // Set up SpinnerDateModel
        spinner.model = dateModel

        // Format the spinner display
        val editor = JSpinner.DateEditor(spinner, "HH:mm")
        spinner.editor = editor

        // Listen to spinner changes
        spinner.addChangeListener(object : ChangeListener {
            override fun stateChanged(e: ChangeEvent?) {
                if (!updatingFromSignal) {
                    updatingFromSpinner = true
                    val date = spinner.value as? Date
                    if (date != null) {
                        val cal = Calendar.getInstance()
                        cal.time = date
                        signal.value = LocalTime(
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            cal.get(Calendar.SECOND)
                        )
                    }
                    updatingFromSpinner = false
                }
            }
        })

        // Listen to signal changes (programmatic updates)
        signal.addListener {
            if (!updatingFromSpinner) {
                updatingFromSignal = true
                val time = signal.value
                if (time != null) {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.HOUR_OF_DAY, time.hour)
                    cal.set(Calendar.MINUTE, time.minute)
                    cal.set(Calendar.SECOND, time.second)
                    spinner.value = cal.time
                }
                updatingFromSignal = false
            }
        }
    }

    actual var range: ClosedRange<LocalTime>? = null
        set(value) {
            field = value
            // SpinnerDateModel doesn't directly support LocalTime range,
            // but we could add validation if needed
        }

    var enabled: Boolean
        get() = spinner.isEnabled
        set(value) {
            spinner.isEnabled = value
            refreshTheming()
        }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)
        val t = theme.theme

        // Set foreground color
        val foregroundColor = t.foreground.closestColor()
        spinner.foreground = AwtColor(
            (foregroundColor.red * 255).toInt().coerceIn(0, 255),
            (foregroundColor.green * 255).toInt().coerceIn(0, 255),
            (foregroundColor.blue * 255).toInt().coerceIn(0, 255),
            (foregroundColor.alpha * 255).toInt().coerceIn(0, 255)
        )

        // Set background color
        val backgroundColor = t.background.closestColor()
        spinner.background = AwtColor(
            (backgroundColor.red * 255).toInt().coerceIn(0, 255),
            (backgroundColor.green * 255).toInt().coerceIn(0, 255),
            (backgroundColor.blue * 255).toInt().coerceIn(0, 255),
            (backgroundColor.alpha * 255).toInt().coerceIn(0, 255)
        )

        // Apply the same colors to the editor (text field inside spinner)
        val editor = spinner.editor
        if (editor is JSpinner.DefaultEditor) {
            val textField = editor.textField
            textField.foreground = spinner.foreground
            textField.background = spinner.background
            textField.caretColor = spinner.foreground

            // Apply font
            val fontStyle = t.font
            var awtFontStyle = AwtFont.PLAIN
            if (fontStyle.italic) {
                awtFontStyle = awtFontStyle or AwtFont.ITALIC
            }
            if (fontStyle.weight >= 700) {
                awtFontStyle = awtFontStyle or AwtFont.BOLD
            }

            val fontSize = fontStyle.size.value.toFloat()
            textField.font = fontStyle.font.deriveFont(awtFontStyle, fontSize)
            spinner.font = textField.font
        }
    }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if (!enabled) t = t[DisabledSemantic]
        return super.applyState(t)
    }
}
