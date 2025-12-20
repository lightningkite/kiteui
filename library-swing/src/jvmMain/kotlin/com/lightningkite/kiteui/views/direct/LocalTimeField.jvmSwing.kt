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
import javax.swing.*
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import kotlin.time.Duration.Companion.minutes

actual class LocalTimeField actual constructor(context: RContext) : RViewWithAction(context) {
    private val spinner = JSpinner()
    override val native = spinner

    private var updatingFromSignal = false
    private var updatingFromSpinner = false

    actual val content: MutableReactiveValue<LocalTime?> = Signal<LocalTime?>(null).also { signal ->
        // Set up custom spinner model for time
        spinner.model = TimeSpinnerModel(signal.value)

        // Format the spinner display
        val editor = JSpinner.DateEditor(spinner, "HH:mm")
        spinner.editor = editor

        // Listen to spinner changes
        spinner.addChangeListener(object : ChangeListener {
            override fun stateChanged(e: ChangeEvent?) {
                if (!updatingFromSignal) {
                    updatingFromSpinner = true
                    val model = spinner.model as? TimeSpinnerModel
                    signal.value = model?.currentTime
                    updatingFromSpinner = false
                }
            }
        })

        // Listen to signal changes (programmatic updates)
        signal.addListener {
            if (!updatingFromSpinner) {
                updatingFromSignal = true
                val model = spinner.model as? TimeSpinnerModel
                if (model != null) {
                    model.currentTime = signal.value
                    spinner.value = model.value // Trigger UI update
                }
                updatingFromSignal = false
            }
        }
    }

    actual var range: ClosedRange<LocalTime>? = null
        set(value) {
            field = value
            val model = spinner.model as? TimeSpinnerModel
            if (model != null) {
                model.range = value
            }
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

    /**
     * Custom SpinnerModel for handling LocalTime values
     */
    private class TimeSpinnerModel(initialTime: LocalTime?) : AbstractSpinnerModel() {
        var currentTime: LocalTime? = initialTime ?: LocalTime(12, 0)
            set(value) {
                if (field != value) {
                    field = value
                    fireStateChanged()
                }
            }

        var range: ClosedRange<LocalTime>? = null

        override fun getValue(): Any {
            // Convert LocalTime to java.util.Date for JSpinner.DateEditor
            val time = currentTime ?: LocalTime(12, 0)
            @Suppress("DEPRECATION")
            return java.util.Date(0, 0, 1, time.hour, time.minute, time.second)
        }

        override fun setValue(value: Any?) {
            when (value) {
                is java.util.Date -> {
                    @Suppress("DEPRECATION")
                    val newTime = LocalTime(
                        value.hours.coerceIn(0, 23),
                        value.minutes.coerceIn(0, 59),
                        value.seconds.coerceIn(0, 59)
                    )

                    // Apply range constraints if set
                    val constrainedTime = if (range != null) {
                        when {
                            newTime < range!!.start -> range!!.start
                            newTime > range!!.endInclusive -> range!!.endInclusive
                            else -> newTime
                        }
                    } else {
                        newTime
                    }

                    currentTime = constrainedTime
                }
                is LocalTime -> {
                    currentTime = value
                }
            }
        }

        override fun getNextValue(): Any? {
            val time = currentTime ?: return null
            val nextMinute = if (time.minute < 59) {
                LocalTime(time.hour, time.minute + 1, time.second, time.nanosecond)
            } else if (time.hour < 23) {
                LocalTime(time.hour + 1, 0, time.second, time.nanosecond)
            } else {
                LocalTime(0, 0) // Wrap to midnight
            }

            // Check range
            if (range != null && nextMinute > range!!.endInclusive) {
                return null
            }

            @Suppress("DEPRECATION")
            return java.util.Date(0, 0, 1, nextMinute.hour, nextMinute.minute, nextMinute.second)
        }

        override fun getPreviousValue(): Any? {
            val time = currentTime ?: return null
            val prevMinute = if (time.minute > 0) {
                LocalTime(time.hour, time.minute - 1, time.second, time.nanosecond)
            } else if (time.hour > 0) {
                LocalTime(time.hour - 1, 59, time.second, time.nanosecond)
            } else {
                LocalTime(23, 59) // Wrap to end of day
            }

            // Check range
            if (range != null && prevMinute < range!!.start) {
                return null
            }

            @Suppress("DEPRECATION")
            return java.util.Date(0, 0, 1, prevMinute.hour, prevMinute.minute, prevMinute.second)
        }
    }
}
