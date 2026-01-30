package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RViewWithAction
import com.lightningkite.reactive.core.MutableReactiveValue
import com.lightningkite.reactive.core.Signal
import kotlinx.datetime.*
import kotlin.time.Clock
import java.awt.BorderLayout
import java.awt.Color as AwtColor
import java.awt.Font as AwtFont
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.event.ChangeListener

actual class LocalDateTimeField actual constructor(context: RContext) : RViewWithAction(context) {
    override val native = JPanel(BorderLayout(5, 0))

    private val dateSpinner = JSpinner(SpinnerDateModel())
    private val timeSpinner = JSpinner(SpinnerDateModel())

    private var updatingFromSignal = false
    private var updatingFromSpinner = false

    private val _content = Signal<LocalDateTime?>(null)
    actual val content: MutableReactiveValue<LocalDateTime?> get() = _content

    actual var range: ClosedRange<LocalDateTime>? = null
        set(value) {
            field = value
            updateSpinnerRanges()
            // Clamp current value to range if set
            _content.value?.let { current ->
                value?.let { r ->
                    if (current < r.start) _content.value = r.start
                    else if (current > r.endInclusive) _content.value = r.endInclusive
                }
            }
        }

    var enabled: Boolean
        get() = dateSpinner.isEnabled && timeSpinner.isEnabled
        set(value) {
            dateSpinner.isEnabled = value
            timeSpinner.isEnabled = value
            refreshTheming()
        }

    init {
        // Configure date spinner
        dateSpinner.editor = JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd")
        (dateSpinner.model as SpinnerDateModel).calendarField = java.util.Calendar.DAY_OF_MONTH

        // Configure time spinner
        timeSpinner.editor = JSpinner.DateEditor(timeSpinner, "HH:mm:ss")
        (timeSpinner.model as SpinnerDateModel).calendarField = java.util.Calendar.HOUR_OF_DAY

        // Set preferred sizes to make them compact
        dateSpinner.preferredSize = java.awt.Dimension(140, 28)
        timeSpinner.preferredSize = java.awt.Dimension(100, 28)

        // Add spinners to panel
        native.add(dateSpinner, BorderLayout.CENTER)
        native.add(timeSpinner, BorderLayout.EAST)
        native.border = EmptyBorder(0, 0, 0, 0)

        // Set up change listeners
        val changeListener = ChangeListener {
            if (!updatingFromSignal) {
                updateContentFromSpinners()
            }
        }
        dateSpinner.addChangeListener(changeListener)
        timeSpinner.addChangeListener(changeListener)

        // Set up signal listener
        _content.addListener {
            if (!updatingFromSpinner) {
                updateSpinnersFromContent()
            }
        }

        // Initialize with current value or null
        updateSpinnersFromContent()
    }

    private fun updateContentFromSpinners() {
        updatingFromSpinner = true
        try {
            val dateValue = dateSpinner.value as? java.util.Date
            val timeValue = timeSpinner.value as? java.util.Date

            if (dateValue != null && timeValue != null) {
                val calendar = java.util.Calendar.getInstance()

                // Get date components
                calendar.time = dateValue
                val year = calendar.get(java.util.Calendar.YEAR)
                val month = calendar.get(java.util.Calendar.MONTH) + 1 // Calendar months are 0-based
                val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)

                // Get time components
                calendar.time = timeValue
                val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                val minute = calendar.get(java.util.Calendar.MINUTE)
                val second = calendar.get(java.util.Calendar.SECOND)

                // Create LocalDateTime
                val newValue = LocalDateTime(year, month, day, hour, minute, second)

                // Apply range constraints if set
                val constrainedValue = range?.let { r ->
                    when {
                        newValue < r.start -> r.start
                        newValue > r.endInclusive -> r.endInclusive
                        else -> newValue
                    }
                } ?: newValue

                if (_content.value != constrainedValue) {
                    _content.value = constrainedValue
                    action?.startAction(this)
                }
            }
        } finally {
            updatingFromSpinner = false
        }
    }

    private fun updateSpinnersFromContent() {
        updatingFromSignal = true
        try {
            val value = _content.value
            if (value != null) {
                val calendar = java.util.Calendar.getInstance()

                // Set date spinner
                calendar.set(value.year, value.month.number - 1, value.day, 0, 0, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                dateSpinner.value = calendar.time

                // Set time spinner
                calendar.set(1970, 0, 1, value.hour, value.minute, value.second)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                timeSpinner.value = calendar.time
            } else {
                // Set to current date/time if null
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val calendar = java.util.Calendar.getInstance()

                calendar.set(now.year, now.month.number - 1, now.day, 0, 0, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                dateSpinner.value = calendar.time

                calendar.set(1970, 0, 1, now.hour, now.minute, now.second)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                timeSpinner.value = calendar.time
            }
        } finally {
            updatingFromSignal = false
        }
    }

    private fun updateSpinnerRanges() {
        range?.let { r ->
            val calendar = java.util.Calendar.getInstance()

            // Set date range
            val dateModel = dateSpinner.model as SpinnerDateModel

            calendar.set(r.start.year, r.start.month.number - 1, r.start.day, 0, 0, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            dateModel.start = calendar.time

            calendar.set(r.endInclusive.year, r.endInclusive.month.number - 1, r.endInclusive.day, 23, 59, 59)
            calendar.set(java.util.Calendar.MILLISECOND, 999)
            dateModel.end = calendar.time

            // Time range is handled implicitly through the date-time combination
        }
    }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme[ClickableSemantic]
        if (!enabled) t = t[DisabledSemantic]
        return super.applyState(t)
    }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)
        val t = theme.theme

        // Apply foreground color
        val foregroundColor = t.foreground.closestColor()
        val fgColor = AwtColor(
            (foregroundColor.red * 255).toInt().coerceIn(0, 255),
            (foregroundColor.green * 255).toInt().coerceIn(0, 255),
            (foregroundColor.blue * 255).toInt().coerceIn(0, 255),
            (foregroundColor.alpha * 255).toInt().coerceIn(0, 255)
        )

        // Apply background color
        val backgroundColor = t.background.closestColor()
        val bgColor = AwtColor(
            (backgroundColor.red * 255).toInt().coerceIn(0, 255),
            (backgroundColor.green * 255).toInt().coerceIn(0, 255),
            (backgroundColor.blue * 255).toInt().coerceIn(0, 255),
            (backgroundColor.alpha * 255).toInt().coerceIn(0, 255)
        )

        // Apply to both spinners and their editors
        applyColorsToSpinner(dateSpinner, fgColor, bgColor)
        applyColorsToSpinner(timeSpinner, fgColor, bgColor)
        native.background = bgColor

        // Apply font
        val font = t.font
        val awtFont = AwtFont(
            AwtFont.DIALOG,
            when {
                font.italic && font.weight >= 600 -> AwtFont.BOLD or AwtFont.ITALIC
                font.italic -> AwtFont.ITALIC
                font.weight >= 600 -> AwtFont.BOLD
                else -> AwtFont.PLAIN
            },
            14
        )

        dateSpinner.font = awtFont
        timeSpinner.font = awtFont
        (dateSpinner.editor as? JComponent)?.font = awtFont
        (timeSpinner.editor as? JComponent)?.font = awtFont
    }

    private fun applyColorsToSpinner(spinner: JSpinner, fg: AwtColor, bg: AwtColor) {
        spinner.foreground = fg
        spinner.background = bg

        // Apply to editor
        val editor = spinner.editor
        if (editor is JComponent) {
            editor.foreground = fg
            editor.background = bg

            // Apply to text field inside editor
            if (editor is JSpinner.DefaultEditor) {
                editor.textField.foreground = fg
                editor.textField.background = bg
                editor.textField.caretColor = fg
            }
        }

        // Apply to all child components
        applyColorsRecursively(spinner, fg, bg)
    }

    private fun applyColorsRecursively(component: java.awt.Container, fg: AwtColor, bg: AwtColor) {
        for (i in 0 until component.componentCount) {
            val child = component.getComponent(i)
            if (child !is JButton) { // Don't change button colors (spinner arrows)
                child.foreground = fg
                child.background = bg
            }
            if (child is java.awt.Container) {
                applyColorsRecursively(child, fg, bg)
            }
        }
    }
}
