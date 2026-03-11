package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RViewWithAction
import com.lightningkite.reactive.core.MutableReactiveValue
import com.lightningkite.reactive.core.Signal
import java.awt.Color as AwtColor
import java.awt.Font as AwtFont
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

actual class AutoCompleteTextField actual constructor(context: RContext) : RViewWithAction(context) {
    override val native = JTextField()

    private var updatingFromSignal = false
    private var updatingFromNative = false
    private var placeholderText: String = ""
    private val popup = JPopupMenu()
    private val suggestionItems = mutableListOf<JMenuItem>()

    actual val content: MutableReactiveValue<String> = Signal("").also { signal ->
        // Set up document listener for content changes
        native.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = updateSignal()
            override fun removeUpdate(e: DocumentEvent?) = updateSignal()
            override fun changedUpdate(e: DocumentEvent?) = updateSignal()

            private fun updateSignal() {
                if (!updatingFromSignal) {
                    updatingFromNative = true
                    val newValue = native.text ?: ""
                    signal.value = newValue
                    updatingFromNative = false

                    // Update suggestions when content changes
                    updateSuggestions()
                }
            }
        })

        // Listen to signal changes (programmatic updates)
        signal.addListener {
            if (!updatingFromNative) {
                updatingFromSignal = true
                if (native.text != signal.value) {
                    native.text = signal.value
                    updateSuggestions()
                }
                updatingFromSignal = false
            }
        }
    }

    actual var keyboardHints: KeyboardHints = KeyboardHints()
        set(value) {
            field = value
            updateKeyboardHints(value)
        }

    actual var suggestions: List<String> = listOf()
        set(value) {
            field = value
            updateSuggestions()
        }

    var hint: String = ""
        set(value) {
            field = value
            placeholderText = value
            updatePlaceholder()
        }

    var align: Align = Align.Start
        set(value) {
            field = value
            updateAlignment(value)
        }

    var enabled: Boolean
        get() = native.isEnabled
        set(value) {
            native.isEnabled = value
            refreshTheming()
        }

    init {
        // Set up focus listener for placeholder
        setupFocusListener()

        // Set default keyboard hints
        keyboardHints = KeyboardHints(KeyboardCase.Sentences)

        // Hide popup when focus is lost
        native.addFocusListener(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent?) {
                // Small delay to allow selection click to process
                SwingUtilities.invokeLater {
                    popup.isVisible = false
                }
            }
        })
    }

    private fun setupFocusListener() {
        native.addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent?) {
                updatePlaceholder()
                updateSuggestions()
            }
            override fun focusLost(e: FocusEvent?) {
                updatePlaceholder()
            }
        })
    }

    private fun updateAlignment(value: Align) {
        native.horizontalAlignment = when (value) {
            Align.Start -> SwingConstants.LEFT
            Align.Center -> SwingConstants.CENTER
            Align.End -> SwingConstants.RIGHT
            Align.Stretch -> SwingConstants.LEFT
        }
    }

    override fun actionSet(value: Action?) {
        super.actionSet(value)
        // Set up action listener for Enter key
        for (listener in native.actionListeners) {
            native.removeActionListener(listener)
        }
        if (value != null) {
            native.addActionListener {
                popup.isVisible = false
                value.startAction(this)
            }
        }
    }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)
        val t = theme.theme

        // Set text color
        val foregroundColor = t.foreground.closestColor()
        native.foreground = AwtColor(
            (foregroundColor.red * 255).toInt().coerceIn(0, 255),
            (foregroundColor.green * 255).toInt().coerceIn(0, 255),
            (foregroundColor.blue * 255).toInt().coerceIn(0, 255),
            (foregroundColor.alpha * 255).toInt().coerceIn(0, 255)
        )

        // Set background color
        val backgroundColor = t.background.closestColor()
        native.background = AwtColor(
            (backgroundColor.red * 255).toInt().coerceIn(0, 255),
            (backgroundColor.green * 255).toInt().coerceIn(0, 255),
            (backgroundColor.blue * 255).toInt().coerceIn(0, 255),
            (backgroundColor.alpha * 255).toInt().coerceIn(0, 255)
        )

        // Set caret color (text cursor)
        native.caretColor = native.foreground

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
        native.font = fontStyle.font.deriveFont(awtFontStyle, fontSize)

        // Apply theme to popup
        popup.background = native.background
        popup.foreground = native.foreground
        popup.border = BorderFactory.createLineBorder(native.foreground, 1)

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
        if (placeholderText.isNotEmpty() && native.text.isEmpty()) {
            native.toolTipText = placeholderText
        } else {
            native.toolTipText = null
        }
    }

    private fun updateKeyboardHints(hints: KeyboardHints) {
        // Handle keyboard input type if needed
        // For now, AutoCompleteTextField typically doesn't need special keyboard filtering
        // as it's primarily for text-based autocomplete
    }

    private fun updateSuggestions() {
        // Clear existing items
        popup.removeAll()
        suggestionItems.clear()

        val currentText = native.text ?: ""

        // Filter suggestions based on current input
        val filtered = if (currentText.isEmpty()) {
            suggestions.take(10) // Show first 10 when empty
        } else {
            suggestions.filter {
                it.contains(currentText, ignoreCase = true)
            }.take(10) // Limit to 10 items
        }

        if (filtered.isEmpty()) {
            popup.isVisible = false
            return
        }

        // Add filtered suggestions to popup
        filtered.forEach { suggestion ->
            val item = JMenuItem(suggestion)
            item.font = native.font
            item.background = popup.background
            item.foreground = popup.foreground

            item.addActionListener {
                selectSuggestion(suggestion)
            }

            // Add mouse hover effect
            item.addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent?) {
                    item.isSelected = true
                }
                override fun mouseExited(e: MouseEvent?) {
                    item.isSelected = false
                }
            })

            popup.add(item)
            suggestionItems.add(item)
        }

        // Show popup below the text field if we have suggestions and the field has focus
        if (filtered.isNotEmpty() && native.hasFocus()) {
            showPopup()
        }
    }

    private fun showPopup() {
        if (!popup.isVisible && suggestionItems.isNotEmpty()) {
            // Position popup below the text field
            popup.show(native, 0, native.height)

            // Set popup width to match text field
            popup.preferredSize = java.awt.Dimension(native.width, popup.preferredSize.height)
            popup.revalidate()
        }
    }

    private fun selectSuggestion(suggestion: String) {
        updatingFromSignal = true
        native.text = suggestion
        content.value = suggestion
        updatingFromSignal = false

        popup.isVisible = false

        // Move cursor to end
        native.caretPosition = suggestion.length

        // Request focus back to the text field
        native.requestFocusInWindow()
    }
}
