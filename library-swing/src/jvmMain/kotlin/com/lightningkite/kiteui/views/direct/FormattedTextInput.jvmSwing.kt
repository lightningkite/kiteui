package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.utils.repairFormatAndPosition
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RViewWithAction
import com.lightningkite.reactive.core.MutableReactiveValue
import com.lightningkite.reactive.core.Signal
import java.awt.Color as AwtColor
import java.awt.Font as AwtFont
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.JPasswordField
import javax.swing.JTextField
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.text.JTextComponent

actual class FormattedTextInput actual constructor(context: RContext) : RViewWithAction(context) {
    private var textField: JTextComponent = JTextField()
    override val native: JTextComponent
        get() = textField

    private var updatingFromSignal = false
    private var updatingFromNative = false
    private var placeholderText: String = ""
    private var isPassword = false

    // Formatting functions
    private var isRawData: (Char) -> Boolean = { true }
    private var formatter: (clean: String) -> String = { it }
    private var formattingEnabled = false

    actual fun format(isRawData: (Char) -> Boolean, formatter: (String) -> String) {
        this.isRawData = isRawData
        this.formatter = formatter
        this.formattingEnabled = true

        // Apply formatting to current content
        if (textField.text.isNotEmpty()) {
            val currentPos = textField.caretPosition
            repairFormatAndPosition(
                dirty = textField.text,
                selectionStart = currentPos,
                selectionEnd = currentPos,
                setResult = { textField.text = it },
                setSelectionRange = { start, _ -> textField.caretPosition = start },
                isRawData = isRawData,
                formatter = formatter
            )
        }
    }

    actual val content: MutableReactiveValue<String> = Signal("").also { signal ->
        // Initial setup of listeners on current field
        setupDocumentListener(signal)

        // Listen to signal changes (programmatic updates)
        signal.addListener {
            if (!updatingFromNative) {
                updatingFromSignal = true
                val newValue = if (formattingEnabled) {
                    formatter(signal.value.filter(isRawData))
                } else {
                    signal.value
                }
                if (textField.text != newValue) {
                    textField.text = newValue
                }
                updatingFromSignal = false
            }
        }
    }

    private fun setupDocumentListener(signal: Signal<String>) {
        textField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = updateSignal()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = updateSignal()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = updateSignal()

            private fun updateSignal() {
                if (!updatingFromSignal) {
                    if (formattingEnabled) {
                        // Schedule formatting on EDT to avoid concurrent modification
                        SwingUtilities.invokeLater {
                            applyFormatting()
                        }
                    } else {
                        updatingFromNative = true
                        signal.value = textField.text ?: ""
                        updatingFromNative = false
                    }
                }
            }
        })
    }

    private fun applyFormatting() {
        if (!updatingFromSignal && formattingEnabled) {
            updatingFromNative = true
            try {
                val currentText = textField.text ?: ""
                val selStart = textField.selectionStart
                val selEnd = textField.selectionEnd

                repairFormatAndPosition(
                    dirty = currentText,
                    selectionStart = selStart,
                    selectionEnd = selEnd,
                    setResult = { formatted ->
                        if (textField.text != formatted) {
                            textField.text = formatted
                        }
                    },
                    setSelectionRange = { start, end ->
                        textField.select(start, end)
                    },
                    isRawData = isRawData,
                    formatter = formatter
                )

                // Update signal with clean value (only raw data)
                val clean = currentText.filter(isRawData)
                if (content.value != clean) {
                    content.value = clean
                }
            } finally {
                updatingFromNative = false
            }
        }
    }

    actual var hint: String = ""
        set(value) {
            field = value
            placeholderText = value
            updatePlaceholder()
        }

    actual var keyboardHints: KeyboardHints = KeyboardHints()
        set(value) {
            field = value
            updateKeyboardHints(value)
        }

    actual var align: Align? = Align.Start
        set(value) {
            field = value
            value?.let { updateAlignment(it) }
        }

    actual var enabled: Boolean
        get() = textField.isEnabled
        set(value) {
            textField.isEnabled = value
            refreshTheming()
        }

    init {
        // Set up placeholder focus handling
        setupFocusListener()

        // Default keyboard hints
        keyboardHints = KeyboardHints(KeyboardCase.Sentences)
    }

    private fun setupFocusListener() {
        textField.addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent?) {
                updatePlaceholder()
            }
            override fun focusLost(e: FocusEvent?) {
                updatePlaceholder()
            }
        })
    }

    private fun updateAlignment(value: Align) {
        if (textField is JTextField) {
            (textField as JTextField).horizontalAlignment = when (value) {
                Align.Start -> SwingConstants.LEFT
                Align.Center -> SwingConstants.CENTER
                Align.End -> SwingConstants.RIGHT
                Align.Stretch -> SwingConstants.LEFT
            }
        }
        // JPasswordField also extends JTextField, so this covers both cases
    }

    override fun actionSet(value: Action?) {
        super.actionSet(value)
        // Set up action listener for Enter key
        if (textField is JTextField) {
            val field = textField as JTextField
            for (listener in field.actionListeners) {
                field.removeActionListener(listener)
            }
            if (value != null) {
                field.addActionListener {
                    value.startAction(this)
                }
            }
        }
    }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)
        val t = theme.theme

        // Set text color
        val foregroundColor = t.foreground.closestColor()
        textField.foreground = AwtColor(
            (foregroundColor.red * 255).toInt().coerceIn(0, 255),
            (foregroundColor.green * 255).toInt().coerceIn(0, 255),
            (foregroundColor.blue * 255).toInt().coerceIn(0, 255),
            (foregroundColor.alpha * 255).toInt().coerceIn(0, 255)
        )

        // Set background color
        val backgroundColor = t.background.closestColor()
        textField.background = AwtColor(
            (backgroundColor.red * 255).toInt().coerceIn(0, 255),
            (backgroundColor.green * 255).toInt().coerceIn(0, 255),
            (backgroundColor.blue * 255).toInt().coerceIn(0, 255),
            (backgroundColor.alpha * 255).toInt().coerceIn(0, 255)
        )

        // Set caret color (text cursor)
        textField.caretColor = textField.foreground

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

        // Update placeholder appearance
        updatePlaceholder()
    }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if (!enabled) t = t[DisabledSemantic]
        return super.applyState(t)
    }

    private fun updatePlaceholder() {
        // Swing doesn't have built-in placeholder support, so we use the tooltip
        // or we could use a custom painted placeholder (more complex)
        if (placeholderText.isNotEmpty() && textField.text.isEmpty()) {
            textField.toolTipText = placeholderText
        } else {
            textField.toolTipText = null
        }
    }

    private fun updateKeyboardHints(hints: KeyboardHints) {
        // Handle password masking - need to switch between JTextField and JPasswordField
        val wasPassword = isPassword
        isPassword = hints.autocomplete in setOf(AutoComplete.Password, AutoComplete.NewPassword)

        if (isPassword != wasPassword) {
            switchFieldType(isPassword)
        }

        // Note: Numeric filtering is NOT applied here since FormattedTextInput
        // uses custom formatting logic via the format() method
        // The caller is responsible for setting up appropriate isRawData filters
    }

    private fun switchFieldType(toPassword: Boolean) {
        val oldText = textField.text
        val oldEnabled = textField.isEnabled

        // Create new field of the appropriate type
        val newField: JTextComponent = if (toPassword) {
            JPasswordField()
        } else {
            JTextField()
        }

        // Transfer properties
        newField.text = oldText
        newField.isEnabled = oldEnabled

        // Replace the field
        textField = newField

        // Re-setup listeners
        setupDocumentListener(content as Signal<String>)
        setupFocusListener()

        // Re-apply alignment
        align?.let { updateAlignment(it) }

        // Re-apply action if exists
        actionSet(action)

        // Trigger theme re-application
        refreshTheming()
    }
}
