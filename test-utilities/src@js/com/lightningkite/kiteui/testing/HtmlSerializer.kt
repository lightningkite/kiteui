package com.lightningkite.kiteui.testing

import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import org.w3c.dom.css.CSSStyleDeclaration

/**
 * Serializes a DOM element to a static HTML file with all styles embedded.
 * This creates a standalone HTML file that can be opened in any browser
 * without requiring JavaScript or external stylesheets.
 */
object HtmlSerializer {

    /**
     * Serializes an element and all its children to a complete HTML document
     * with all computed styles embedded as inline styles.
     *
     * @param element The root element to serialize
     * @param title Optional title for the HTML document
     * @return Complete HTML document as a string
     */
    fun serializeToHtml(element: Element, title: String = "KiteUI Snapshot"): String {
        // Clone the element so we don't modify the original
        val clone = element.cloneNode(true) as Element

        // Apply computed styles to all elements in the clone
        applyComputedStyles(clone, element)

        // Build complete HTML document
        return buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("<html>")
            appendLine("<head>")
            appendLine("    <meta charset=\"UTF-8\">")
            appendLine("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
            appendLine("    <title>$title</title>")
            appendLine("    <style>")
            appendLine("        /* Reset margins and padding */")
            appendLine("        * { box-sizing: border-box; }")
            appendLine("        body { margin: 0; padding: 0; }")
            appendLine("    </style>")
            appendLine("</head>")
            appendLine("<body>")
            appendLine(clone.outerHTML)
            appendLine("</body>")
            appendLine("</html>")
        }
    }

    /**
     * Recursively applies computed styles to an element and all its children.
     * This ensures the clone looks identical to the original even without CSS files.
     *
     * @param clone The cloned element to apply styles to
     * @param original The original element to read computed styles from
     */
    private fun applyComputedStyles(clone: Element, original: Element) {
        if (clone is HTMLElement && original is HTMLElement) {
            val computedStyle = window.getComputedStyle(original)
            val inlineStyle = buildInlineStyle(computedStyle)
            clone.setAttribute("style", inlineStyle)
        }

        // Process children
        val cloneChildren = clone.children
        val originalChildren = original.children
        val childCount = minOf(cloneChildren.length, originalChildren.length)
        for (i in 0 until childCount) {
            val cloneChild = cloneChildren.item(i)
            val originalChild = originalChildren.item(i)
            if (cloneChild != null && originalChild != null) {
                applyComputedStyles(cloneChild, originalChild)
            }
        }
    }

    /**
     * Builds an inline style string from a CSSStyleDeclaration.
     * Includes all important visual properties.
     */
    private fun buildInlineStyle(style: CSSStyleDeclaration): String {
        return buildString {
            // Layout properties
            appendProperty(style, "display")
            appendProperty(style, "position")
            appendProperty(style, "top")
            appendProperty(style, "right")
            appendProperty(style, "bottom")
            appendProperty(style, "left")
            appendProperty(style, "width")
            appendProperty(style, "height")
            appendProperty(style, "min-width")
            appendProperty(style, "min-height")
            appendProperty(style, "max-width")
            appendProperty(style, "max-height")

            // Flexbox
            appendProperty(style, "flex")
            appendProperty(style, "flex-direction")
            appendProperty(style, "flex-wrap")
            appendProperty(style, "flex-grow")
            appendProperty(style, "flex-shrink")
            appendProperty(style, "flex-basis")
            appendProperty(style, "justify-content")
            appendProperty(style, "align-items")
            appendProperty(style, "align-content")
            appendProperty(style, "align-self")
            appendProperty(style, "gap")
            appendProperty(style, "row-gap")
            appendProperty(style, "column-gap")

            // Spacing
            appendProperty(style, "margin")
            appendProperty(style, "margin-top")
            appendProperty(style, "margin-right")
            appendProperty(style, "margin-bottom")
            appendProperty(style, "margin-left")
            appendProperty(style, "padding")
            appendProperty(style, "padding-top")
            appendProperty(style, "padding-right")
            appendProperty(style, "padding-bottom")
            appendProperty(style, "padding-left")

            // Borders
            appendProperty(style, "border")
            appendProperty(style, "border-width")
            appendProperty(style, "border-style")
            appendProperty(style, "border-color")
            appendProperty(style, "border-radius")
            appendProperty(style, "border-top")
            appendProperty(style, "border-right")
            appendProperty(style, "border-bottom")
            appendProperty(style, "border-left")

            // Background
            appendProperty(style, "background")
            appendProperty(style, "background-color")
            appendProperty(style, "background-image")
            appendProperty(style, "background-size")
            appendProperty(style, "background-position")
            appendProperty(style, "background-repeat")

            // Typography
            appendProperty(style, "color")
            appendProperty(style, "font")
            appendProperty(style, "font-family")
            appendProperty(style, "font-size")
            appendProperty(style, "font-weight")
            appendProperty(style, "font-style")
            appendProperty(style, "line-height")
            appendProperty(style, "text-align")
            appendProperty(style, "text-decoration")
            appendProperty(style, "text-transform")
            appendProperty(style, "letter-spacing")
            appendProperty(style, "word-spacing")
            appendProperty(style, "white-space")

            // Visual effects
            appendProperty(style, "opacity")
            appendProperty(style, "visibility")
            appendProperty(style, "overflow")
            appendProperty(style, "overflow-x")
            appendProperty(style, "overflow-y")
            appendProperty(style, "box-shadow")
            appendProperty(style, "transform")
            appendProperty(style, "filter")

            // Z-index and stacking
            appendProperty(style, "z-index")
        }
    }

    /**
     * Helper to append a CSS property to a string builder if it has a value.
     */
    private fun StringBuilder.appendProperty(style: CSSStyleDeclaration, property: String) {
        val value = style.getPropertyValue(property)
        if (value.isNotEmpty() && value != "none" && value != "normal") {
            if (this.isNotEmpty()) append(" ")
            append("$property: $value;")
        }
    }
}
