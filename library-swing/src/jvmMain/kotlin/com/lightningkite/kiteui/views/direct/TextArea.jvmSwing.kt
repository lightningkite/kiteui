package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.models.toAwt
import com.lightningkite.kiteui.models.toAwtColor
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RViewWithAction
import com.lightningkite.reactive.core.MutableReactiveValue
import com.lightningkite.reactive.core.Signal
import java.awt.Color as AwtColor
import java.awt.Font as AwtFont
import java.awt.Graphics
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.JTextArea
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

actual class TextArea actual constructor(context: RContext) : RViewWithAction(context) {
    private var placeholderText: String = ""
    private var placeholderColorValue: AwtColor = AwtColor.GRAY

    override val native = object : JTextArea() {
        init {
            // Enable line wrapping by default for multi-line text
            lineWrap = true
            wrapStyleWord = true

            // Add focus listener to repaint when focus changes (for placeholder visibility)
            addFocusListener(object : FocusListener {
                override fun focusGained(e: FocusEvent?) {
                    repaint()
                }
                override fun focusLost(e: FocusEvent?) {
                    repaint()
                }
            })
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)

            // Draw placeholder if text is empty and not focused
            if (text.isEmpty() && placeholderText.isNotEmpty() && !isFocusOwner) {
                val g2d = g.create() as Graphics
                try {
                    g2d.color = placeholderColorValue
                    g2d.font = font
                    val insets = insets
                    val fm = g2d.fontMetrics
                    g2d.drawString(
                        placeholderText,
                        insets.left,
                        insets.top + fm.ascent
                    )
                } finally {
                    g2d.dispose()
                }
            }
        }
    }

    actual val content: MutableReactiveValue<String> = Signal("").also { signal ->
        native.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                if (native.text != signal.value) {
                    signal.value = native.text ?: ""
                }
            }
            override fun removeUpdate(e: DocumentEvent?) {
                if (native.text != signal.value) {
                    signal.value = native.text ?: ""
                }
            }
            override fun changedUpdate(e: DocumentEvent?) {
                if (native.text != signal.value) {
                    signal.value = native.text ?: ""
                }
            }
        })
        signal.addListener {
            if (native.text != signal.value) {
                native.text = signal.value
            }
        }
    }

    actual var enabled: Boolean
        get() = native.isEnabled
        set(value) {
            native.isEnabled = value
            refreshTheming()
        }

    actual var keyboardHints: KeyboardHints = KeyboardHints()
        set(value) {
            field = value
            // Keyboard hints are not fully applicable to Swing desktop
            // but could be used for validation or input filters in the future
        }

    actual var hint: String
        get() = placeholderText
        set(value) {
            placeholderText = value
            native.repaint()
        }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)
        val t = theme.theme

        // Set text color
        native.foreground = t.foreground.toAwtColor()

        // Set background color (for text area itself)
        native.background = t.background.toAwtColor()

        // Set placeholder color (lighter version of foreground)
        val placeholderColor = t.foreground.closestColor().withAlpha(0.5f)
        placeholderColorValue = placeholderColor.toAwt()

        // Apply font
        val font = t.font
        native.font = createAwtFont(font)

        // Set caret color to match foreground
        native.caretColor = t.foreground.toAwtColor()

        // Set selection colors
        val selectionBg = t.foreground.closestColor().withAlpha(0.3f)
        native.selectionColor = selectionBg.toAwt()

        native.selectedTextColor = t.foreground.toAwtColor()
    }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if (!enabled) t = t[DisabledSemantic]
        return super.applyState(t)
    }

    private fun createAwtFont(fontAndStyle: FontAndStyle): AwtFont {
        val baseFont = fontAndStyle.font

        // Determine font style
        var style = AwtFont.PLAIN
        if (fontAndStyle.italic) {
            style = style or AwtFont.ITALIC
        }
        if (fontAndStyle.weight >= 700) {
            style = style or AwtFont.BOLD
        }

        // Create derived font with size
        val size = fontAndStyle.size.value.toFloat()

        // Derive font with style and size
        return baseFont.deriveFont(style, size)
    }

}
