package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import javax.swing.JEditorPane
import javax.swing.event.HyperlinkEvent
import java.awt.Desktop
import java.net.URI

/**
 * WebView implementation for JVM Swing using JEditorPane.
 *
 * IMPORTANT LIMITATIONS:
 * - JEditorPane only supports very basic HTML (HTML 3.2 level)
 * - No JavaScript support (permitJs is ignored)
 * - Limited CSS support (basic styling only)
 * - No modern HTML5 features
 * - Best suited for simple formatted text or basic HTML content
 *
 * For full web browser capabilities in Swing applications, consider:
 * - JavaFX WebView (embedded via JFXPanel) - requires JavaFX dependencies
 * - JxBrowser (commercial, embedded Chromium) - requires license
 * - DJ Native Swing (uses native OS browser) - complex setup
 */
actual class WebView actual constructor(context: RContext) : RView(context) {
    override val native = JEditorPane().apply {
        isEditable = false
        contentType = "text/html"

        // Enable hyperlink clicks to open in system browser
        addHyperlinkListener { event ->
            if (event.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                if (Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().browse(event.url.toURI())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    actual var url: String = ""
        set(value) {
            field = value
            if (value.isNotEmpty()) {
                try {
                    native.page = URI(value).toURL()
                } catch (e: Exception) {
                    e.printStackTrace()
                    // If URL loading fails, show error in the pane
                    native.text = "<html><body><p>Failed to load URL: $value</p><p>Error: ${e.message}</p></body></html>"
                }
            }
        }

    /**
     * Note: JEditorPane does not support JavaScript.
     * This property is provided for API compatibility but has no effect.
     */
    actual var permitJs: Boolean = false

    actual var content: String = ""
        set(value) {
            field = value
            native.contentType = "text/html"
            native.text = value
        }
}
