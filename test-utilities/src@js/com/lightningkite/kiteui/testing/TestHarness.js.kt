package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.root
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.kiteui.views.native
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
    actual val async: AsyncTestSupport = AsyncTestSupport()
    private var rootView: Element? = null

    actual fun render(theme: Theme, content: ViewWriter.() -> Unit): Element {
        lateinit var capturedRoot: Element
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

    actual fun screenshotView(view: Element, name: String): ByteArray? {
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
     *
     * Note: This is a best-effort operation. If the server is not running,
     * it will silently skip saving the file.
     */
    private fun saveSnapshotToFile(name: String, html: String) {
        try {
            val xhr = org.w3c.xhr.XMLHttpRequest()
            // Use async to avoid blocking and prevent network errors from propagating
            xhr.open("POST", "http://localhost:3001/save-snapshot", async = true)
            xhr.setRequestHeader("Content-Type", "application/json")

            xhr.onload = {
                if (xhr.status.toInt() == 200) {
                    println("✅ Snapshot saved to file: $name.html")
                } else {
                    println("⚠️  Failed to save snapshot: ${xhr.status} ${xhr.responseText}")
                }
            }

            xhr.onerror = {
                // Silently ignore network errors - snapshot server is optional
                println("⚠️  Snapshot server not available (this is optional)")
            }

            val payload = json(
                "name" to name,
                "html" to html
            )

            xhr.send(JSON.stringify(payload))
        } catch (e: Exception) {
            // Silently ignore - saving snapshots is optional for tests
            println("⚠️  Could not save snapshot to file: ${e.message}")
        }
    }

    @OptIn(OverrideOnly::class)
    actual fun cleanup() {
        rootView?.onShutdown()
        rootView = null
    }

    // Convenience methods that delegate to AsyncTestSupport

    actual suspend fun waitUntilIdle() {
        async.waitUntilIdle()
    }

    actual suspend fun waitFor(timeout: kotlin.time.Duration, condition: () -> Boolean) {
        async.waitFor(timeout, condition)
    }

    actual suspend fun advanceTimeBy(duration: kotlin.time.Duration) {
        async.advanceTimeBy(duration)
    }
}
