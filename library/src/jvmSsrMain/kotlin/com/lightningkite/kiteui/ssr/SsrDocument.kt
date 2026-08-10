package com.lightningkite.kiteui.ssr

import kotlinx.serialization.json.Json

/**
 * Renders a complete HTML document from an SsrResult.
 *
 * Usage:
 * ```kotlin
 * val result = ssrContext.render { ... }
 * val html = SsrDocument().render(result)
 * ```
 */
public class SsrDocument(
    internal val lang: String = "en",
    internal val charset: String = "UTF-8",
    internal val baseHref: String? = null,
    internal val additionalHeadContent: String = "",
    internal val additionalBodyContent: String = "",
    internal val includeResetCss: Boolean = true,
) {
    /**
     * Render an SSR result to a complete HTML document.
     *
     * @param result The SSR result containing HTML, CSS, and metadata
     * @param resourceData Optional map of resource key to serialized JSON data for client hydration
     * @return Complete HTML document string
     */
    internal fun render(result: SsrResult, resourceData: Map<String, String> = emptyMap()): String = buildString {
        appendLine("<!DOCTYPE html>")
        appendLine("<html lang=\"$lang\">")
        appendLine("<head>")
        appendLine("  <meta charset=\"$charset\">")
        appendLine("  <meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")

        // Title
        result.title?.let {
            appendLine("  <title>${escapeHtml(it)}</title>")
        }

        // Meta description
        result.description?.let {
            appendLine("  <meta name=\"description\" content=\"${escapeAttr(it)}\">")
        }

        // Canonical URL
        result.canonicalUrl?.let {
            appendLine("  <link rel=\"canonical\" href=\"${escapeAttr(it)}\">")
        }

        // OpenGraph and other meta tags
        result.metaTags.forEach { (property, content) ->
            when {
                // OpenGraph uses property attribute
                property.startsWith("og:") || property.startsWith("article:") || property.startsWith("product:") ->
                    appendLine("  <meta property=\"${escapeAttr(property)}\" content=\"${escapeAttr(content)}\">")
                // Twitter uses name attribute (though property also works)
                property.startsWith("twitter:") ->
                    appendLine("  <meta name=\"${escapeAttr(property)}\" content=\"${escapeAttr(content)}\">")
                // All other meta tags use name attribute
                else ->
                    appendLine("  <meta name=\"${escapeAttr(property)}\" content=\"${escapeAttr(content)}\">")
            }
        }

        // Base href
        baseHref?.let {
            appendLine("  <base href=\"${escapeAttr(it)}\">")
        }

        // Head elements from DynamicCss (fonts, etc.)
        result.headElements.forEach { element ->
            appendLine("  $element")
        }

        // CSS Reset (optional)
        if (includeResetCss) {
            appendLine("  <style>")
            appendLine(RESET_CSS)
            appendLine("  </style>")
        }

        // Generated CSS
        if (result.css.isNotBlank()) {
            appendLine("  <style>")
            appendLine(result.css)
            appendLine("  </style>")
        }

        // Additional head content
        if (additionalHeadContent.isNotBlank()) {
            appendLine(additionalHeadContent)
        }

        appendLine("</head>")
        appendLine("<body>")

        // Rendered content
        appendLine(result.html)

        // Inject SSR resource data for client-side hydration
        // Always emit __SSR_DATA__ to signal SSR was used (enables client-side hydration detection)
        // by Claude
        val jsonData = Json.encodeToString(kotlinx.serialization.serializer<Map<String, String>>(), resourceData)
        appendLine("<script id=\"__SSR_DATA__\" type=\"application/json\">")
        appendLine(escapeScriptContent(jsonData))
        appendLine("</script>")

        // Additional body content (for hydration scripts, etc.)
        if (additionalBodyContent.isNotBlank()) {
            appendLine(additionalBodyContent)
        }

        appendLine("</body>")
        appendLine("</html>")
    }

    /**
     * Escape content for safe embedding in a script tag.
     * Prevents script injection by escaping </script sequences.
     */
    private fun escapeScriptContent(json: String): String {
        return json.replace("</", "<\\/")
    }

    private fun escapeHtml(text: String): String = buildString {
        for (char in text) {
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#x27;")
                else -> append(char)
            }
        }
    }

    private fun escapeAttr(text: String): String = escapeHtml(text)

    internal companion object {
        /**
         * Minimal CSS reset based on Eric Meyer's reset.
         */
        internal val RESET_CSS: String = """
            /* CSS Reset */
            html, body, div, span, applet, object, iframe,
            h1, h2, h3, h4, h5, h6, p, blockquote, pre,
            a, abbr, acronym, address, big, cite, code,
            del, dfn, em, img, ins, kbd, q, s, samp,
            small, strike, strong, sub, sup, tt, var,
            b, u, i, center,
            dl, dt, dd, ol, ul, li,
            fieldset, form, label, legend,
            table, caption, tbody, tfoot, thead, tr, th, td,
            article, aside, canvas, details, embed,
            figure, figcaption, footer, header, hgroup,
            menu, nav, output, ruby, section, summary,
            time, mark, audio, video {
                margin: 0;
                padding: 0;
                border: 0;
                font-size: 100%;
                font: inherit;
                vertical-align: baseline;
            }
            article, aside, details, figcaption, figure,
            footer, header, hgroup, menu, nav, section {
                display: block;
            }
            body {
                line-height: 1;
            }
            ol, ul {
                list-style: none;
            }
            blockquote, q {
                quotes: none;
            }
            blockquote:before, blockquote:after,
            q:before, q:after {
                content: '';
                content: none;
            }
            table {
                border-collapse: collapse;
                border-spacing: 0;
            }
        """.trimIndent()
    }
}
