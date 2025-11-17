package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.root
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.frame
import org.w3c.dom.HTMLElement
import kotlin.js.json

// External reference to JavaScript's JSON object
external object JSON {
    fun stringify(value: Any): String
}

/**
 * JavaScript/Web implementation of TestHarness.
 *
 * Uses the `root(Theme) {}` pattern which is the standard way to
 * initialize KiteUI on the web platform.
 *
 * Screenshots are captured by serializing the DOM to a static HTML file
 * with all computed styles embedded as inline styles. This creates a
 * standalone HTML file that can be opened in any browser for visual inspection.
 */
actual class TestHarness {
    actual val supported: Boolean = true
    private var rootView: RView? = null

    actual fun render(theme: Theme, content: ViewWriter.() -> Unit): RView {
        lateinit var capturedRoot: RView
        root(theme) {
            frame {
                content()
            }.also { capturedRoot = it }
        }
        rootView = capturedRoot
        return capturedRoot
    }

    actual fun screenshot(name: String): ByteArray? {
        val root = rootView ?: return null
        // native is a FutureElement - we need to get the actual created Element
        val element = root.native.create() as? HTMLElement ?: return null
        return captureElementSnapshot(element, name)
    }

    actual fun screenshotView(view: RView, name: String): ByteArray? {
        // native is a FutureElement - we need to get the actual created Element
        val element = view.native.create() as? HTMLElement ?: return null
        return captureElementSnapshot(element, name)
    }

    private fun captureElementSnapshot(element: HTMLElement, name: String): ByteArray? {
        return try {
            // Serialize the element to HTML with embedded styles
            val html = HtmlSerializer.serializeToHtml(element, name)

            // Convert to UTF-8 bytes
            val bytes = html.encodeToByteArray()

            println("HTML snapshot captured for: $name (${bytes.size} bytes, ${html.length} characters)")

            // Save to filesystem via Karma middleware
            saveSnapshotToFile(name, html)

            bytes
        } catch (e: Exception) {
            println("Failed to capture HTML snapshot: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves an HTML snapshot to the filesystem via standalone snapshot server.
     *
     * Requires the snapshot server to be running:
     *   node local/snapshot-server.js
     *
     * The server saves files to: library/local/screenshots/js/
     */
    private fun saveSnapshotToFile(name: String, html: String) {
        try {
            val xhr = org.w3c.xhr.XMLHttpRequest()
            xhr.open("POST", "http://localhost:3001/save-snapshot", async = false) // Synchronous for simplicity
            xhr.setRequestHeader("Content-Type", "application/json")

            val payload = json(
                "name" to name,
                "html" to html
            )

            xhr.send(JSON.stringify(payload))

            if (xhr.status.toInt() == 200) {
                println("✅ Snapshot saved to file: $name.html")
            } else {
                println("⚠️  Failed to save snapshot: ${xhr.status} ${xhr.responseText}")
            }
        } catch (e: Exception) {
            println("⚠️  Could not save snapshot to file: ${e.message}")
            println("   💡 Make sure the snapshot server is running:")
            println("      node local/snapshot-server.js")
        }
    }

    actual fun cleanup() {
        rootView?.shutdown()
        rootView = null
    }
}
