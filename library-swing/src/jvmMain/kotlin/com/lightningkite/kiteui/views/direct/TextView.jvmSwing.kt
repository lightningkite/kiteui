package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.models.toAwtColor
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import java.awt.Color as AwtColor
import java.awt.Font as AwtFont
import com.lightningkite.kiteui.views.ClickThroughLabel
import javax.swing.JLabel
import javax.swing.SwingConstants

actual class TextView actual constructor(context: RContext) : RView(context) {
    // Use ClickThroughLabel so text doesn't block clicks from parent Link/Button
    override val native = ClickThroughLabel()

    private var isHtmlContent = false
    private var rawContent = ""

    actual var content: String
        get() = rawContent
        set(value) {
            rawContent = value
            isHtmlContent = false
            updateDisplayedContent()
        }

    actual var align: Align? = Align.Start
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

    actual var ellipsis: Boolean = true
        set(value) {
            field = value
            updateDisplayedContent()
        }

    actual var wraps: Boolean = true
        set(value) {
            field = value
            updateDisplayedContent()
        }

    actual var wordBreak: WordBreak = WordBreak.Normal
        set(value) {
            field = value
            // Swing doesn't have direct word-break control like CSS
            // This would need custom text rendering for full support
        }

    actual var lineClamp: Int? = null
        set(value) {
            field = value
            updateDisplayedContent()
        }

    private fun updateDisplayedContent() {
        val content = rawContent

        when {
            // HTML mode for wrapping or line clamping
            wraps || lineClamp != null || isHtmlContent -> {
                val htmlContent = if (isHtmlContent) content else escapeHtml(content)

                // Build inline style that includes font from the component
                val font = native.font
                val style = buildString {
                    append("style='")
                    // Include font family and size to ensure HTML rendering uses the component's font
                    append("font-family: ${font.family}; ")
                    append("font-size: ${font.size}pt; ")
                    if (font.isBold) append("font-weight: bold; ")
                    if (font.isItalic) append("font-style: italic; ")
                    if (!wraps) {
                        append("white-space: nowrap; ")
                    }
                    if (ellipsis) {
                        append("overflow: hidden; text-overflow: ellipsis; ")
                    }
                    append("'")
                }

                // For line clamping, we need to use HTML with max lines
                val displayContent = if (lineClamp != null) {
                    // Approximate line clamping using HTML
                    // This is a limitation of JLabel - true line clamping would require custom rendering
                    "<html><div $style>$htmlContent</div></html>"
                } else {
                    "<html><div $style>$htmlContent</div></html>"
                }

                native.text = displayContent
            }
            // Plain text mode
            else -> {
                native.text = content
            }
        }
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    override fun applyTheme(theme: ThemeAndBack) {
        val t = theme.theme

        // For TextView, we need to handle background specially because RoundedBorder
        // paints after the component content (which would cover the text).
        // We use a custom approach: make the label opaque and use an EmptyBorder for padding,
        // letting Swing's standard painting handle the background correctly.
        if (theme.drawBackground) {
            // Set background color - JLabel will paint this first
            native.background = t.background.closestColor().toAwt()
            native.isOpaque = true

            // Apply padding with EmptyBorder instead of RoundedBorder
            val p = if (paddingByEdge != null) {
                val pe = paddingByEdge!!
                java.awt.Insets(pe.top.px.toInt(), pe.left.px.toInt(), pe.bottom.px.toInt(), pe.right.px.toInt())
            } else if (theme.padding) {
                val tp = t.padding
                java.awt.Insets(tp.top.px.toInt(), tp.left.px.toInt(), tp.bottom.px.toInt(), tp.right.px.toInt())
            } else {
                java.awt.Insets(0, 0, 0, 0)
            }
            native.border = javax.swing.BorderFactory.createEmptyBorder(p.top, p.left, p.bottom, p.right)
        } else {
            // No background - use standard approach
            super.applyTheme(theme)
        }

        // Set text color (must be after background setup)
        native.foreground = t.foreground.toAwtColor()

        // Apply font
        val font = t.font
        native.font = createAwtFont(font)

        // If we have underline, strikethrough, or allCaps, we need to use HTML rendering
        if (font.underline || font.strikethrough || font.allCaps) {
            refreshContentWithStyling(font)
        } else {
            // Refresh content in case we were previously using styled HTML
            updateDisplayedContent()
        }
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
        val size = fontAndStyle.size.px.toFloat()

        // Derive font with style and size
        return baseFont.deriveFont(style, size)
    }

    private fun refreshContentWithStyling(fontAndStyle: FontAndStyle) {
        // For special text styling, we need to use HTML
        var content = if (isHtmlContent) rawContent else escapeHtml(rawContent)

        // Apply allCaps transformation
        if (fontAndStyle.allCaps) {
            content = content.uppercase()
        }

        // Wrap with underline/strikethrough tags
        if (fontAndStyle.underline) {
            content = "<u>$content</u>"
        }
        if (fontAndStyle.strikethrough) {
            content = "<s>$content</s>"
        }

        // Build style string
        val style = buildString {
            append("style='")
            if (!wraps) {
                append("white-space: nowrap; ")
            }
            if (ellipsis) {
                append("overflow: hidden; text-overflow: ellipsis; ")
            }
            append("'")
        }

        native.text = "<html><div $style>$content</div></html>"
    }

    actual fun setBasicHtmlContent(html: String) {
        rawContent = html
        isHtmlContent = true
        native.text = "<html>$html</html>"
    }
}

