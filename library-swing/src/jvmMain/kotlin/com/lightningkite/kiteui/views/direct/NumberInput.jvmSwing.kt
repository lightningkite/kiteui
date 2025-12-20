package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.utils.numberAutocommaRepair
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RViewWithAction
import com.lightningkite.reactive.core.MutableReactiveValue
import com.lightningkite.reactive.core.Signal
import java.awt.Color as AwtColor
import java.awt.Font as AwtFont
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.JTextField
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.DocumentFilter

actual class NumberInput actual constructor(context: RContext) : RViewWithAction(context) {
    override val native = JTextField()

    private var updatingFromSignal = false
    private var updatingFromNative = false
    private var placeholderText: String = ""
    private var currentRange: ClosedRange<Double>? = null

    actual val content: MutableReactiveValue<Double?> = Signal<Double?>(null).also { signal ->
        // Listen to document changes (user input)
        native.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = updateFromNative()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = updateFromNative()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = updateFromNative()

            private fun updateFromNative() {
                if (!updatingFromSignal) {
                    updatingFromNative = true
                    SwingUtilities.invokeLater {
                        try {
                            // Apply autocomma repair
                            val text = native.text ?: ""
                            if (text.isNotEmpty()) {
                                numberAutocommaRepair(
                                    dirty = text,
                                    selectionStart = native.selectionStart,
                                    selectionEnd = native.selectionEnd,
                                    allowDecimal = keyboardHints.type.allowDecimal,
                                    setResult = { formatted ->
                                        if (native.text != formatted) {
                                            native.text = formatted
                                        }
                                    },
                                    setSelectionRange = { start, end ->
                                        native.selectionStart = start.coerceIn(0, native.text.length)
                                        native.selectionEnd = end.coerceIn(0, native.text.length)
                                    }
                                )
                            }

                            // Parse the value
                            val cleanText = native.text?.filter { it.isDigit() || it in setOf('.', '-') } ?: ""
                            val parsedValue = if (cleanText.isEmpty() || cleanText == "-" || cleanText == ".") {
                                null
                            } else {
                                cleanText.toDoubleOrNull()
                            }

                            // Apply range constraints if set
                            val finalValue = if (parsedValue != null && currentRange != null) {
                                parsedValue.coerceIn(currentRange!!)
                            } else {
                                parsedValue
                            }

                            signal.value = finalValue
                        } finally {
                            updatingFromNative = false
                        }
                    }
                }
            }
        })

        // Listen to signal changes (programmatic updates)
        signal.addListener {
            if (!updatingFromNative) {
                updatingFromSignal = true
                val newText = signal.value?.let { value ->
                    // Format with commas for display
                    val str = value.toString()
                    val parts = str.split('.')
                    val intPart = parts[0].reversed().chunked(3) { it.reversed() }.reversed().joinToString(",")
                    if (parts.size > 1) "$intPart.${parts[1]}" else intPart
                } ?: ""
                if (native.text != newText) {
                    native.text = newText
                }
                updatingFromSignal = false
            }
        }
    }

    actual var enabled: Boolean
        get() = native.isEnabled
        set(value) {
            native.isEnabled = value
            refreshTheming()
        }

    actual var keyboardHints: KeyboardHints = KeyboardHints.decimal
        set(value) {
            field = value
            installNumericFilter()
        }

    actual var hint: String = ""
        set(value) {
            field = value
            placeholderText = value
            updatePlaceholder()
        }

    actual var range: ClosedRange<Double>? = null
        set(value) {
            field = value
            currentRange = value
            // Re-validate current value if range is set
            if (value != null && content.value != null) {
                val current = content.value!!
                if (current < value.start || current > value.endInclusive) {
                    content.value = current.coerceIn(value)
                }
            }
        }

    actual var align: Align? = Align.End
        set(value) {
            field = value
            value?.let {
                native.horizontalAlignment = when (it) {
                    Align.Start -> SwingConstants.LEFT
                    Align.Center -> SwingConstants.CENTER
                    Align.End -> SwingConstants.RIGHT
                    Align.Stretch -> SwingConstants.LEFT
                }
            }
        }

    init {
        // Set default alignment to End (right-aligned for numbers)
        native.horizontalAlignment = SwingConstants.RIGHT

        // Install numeric filter
        installNumericFilter()

        // Setup focus listener for placeholder
        setupFocusListener()
    }

    private fun setupFocusListener() {
        native.addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent?) {
                updatePlaceholder()
            }

            override fun focusLost(e: FocusEvent?) {
                updatePlaceholder()
            }
        })
    }

    private fun updatePlaceholder() {
        // Swing doesn't have built-in placeholder support, so we use tooltip
        if (placeholderText.isNotEmpty() && native.text.isEmpty()) {
            native.toolTipText = placeholderText
        } else {
            native.toolTipText = null
        }
    }

    private fun installNumericFilter() {
        val doc = native.document
        if (doc is AbstractDocument) {
            doc.documentFilter = object : DocumentFilter() {
                override fun insertString(fb: FilterBypass?, offset: Int, string: String?, attr: AttributeSet?) {
                    val filtered = filterNumericInput(
                        string,
                        fb?.document?.getText(0, fb.document.length).orEmpty(),
                        offset
                    )
                    if (!filtered.isNullOrEmpty()) {
                        super.insertString(fb, offset, filtered, attr)
                    }
                }

                override fun replace(fb: FilterBypass?, offset: Int, length: Int, text: String?, attrs: AttributeSet?) {
                    val filtered = filterNumericInput(
                        text,
                        fb?.document?.getText(0, fb.document.length).orEmpty(),
                        offset,
                        length
                    )
                    super.replace(fb, offset, length, filtered ?: "", attrs)
                }
            }
        }
    }

    private fun filterNumericInput(
        input: String?,
        existingText: String,
        offset: Int,
        length: Int = 0
    ): String? {
        if (input == null) return null

        val allowDecimal = keyboardHints.type.allowDecimal
        val allowNegative = keyboardHints.type == KeyboardType.DecimalWithNegative ||
                keyboardHints.type == KeyboardType.IntegerWithNegative

        return input.filter { char ->
            when {
                char.isDigit() -> true
                char == '.' && allowDecimal -> {
                    // Only allow one decimal point
                    val textBeforeInsert = existingText.substring(0, offset) +
                            existingText.substring(offset + length)
                    !textBeforeInsert.contains('.')
                }
                char == '-' && allowNegative -> {
                    // Minus sign only at the beginning
                    offset == 0 && !existingText.startsWith('-')
                }
                else -> false
            }
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

        // Update placeholder appearance
        updatePlaceholder()
    }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if (!enabled) t = t[DisabledSemantic]
        return super.applyState(t)
    }
}
