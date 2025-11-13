package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.root
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.frame
import kotlinx.browser.document
import kotlinx.coroutines.await
import org.khronos.webgl.Uint8Array
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import kotlin.js.Promise

/**
 * External declaration for html2canvas library.
 * https://html2canvas.hertzen.com/
 */
@JsModule("html2canvas")
@JsNonModule
external fun html2canvas(element: HTMLElement, options: dynamic = definedExternally): Promise<HTMLCanvasElement>

/**
 * JavaScript/Web implementation of TestHarness.
 *
 * Uses the `root(Theme) {}` pattern which is the standard way to
 * initialize KiteUI on the web platform.
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
        return captureElementScreenshot(root.native, name)
    }

    actual fun screenshotView(view: RView, name: String): ByteArray? {
        return captureElementScreenshot(view.native, name)
    }

    private fun captureElementScreenshot(element: HTMLElement, name: String): ByteArray? {
        return try {
            // Note: html2canvas is async, but this API requires synchronous return.
            // In a real browser environment, we kick off the screenshot process
            // and it completes asynchronously. The canvas is added to the DOM
            // for manual inspection/download.

            println("Starting screenshot capture for: $name")

            // Start async capture process
            kotlinx.coroutines.GlobalScope.promise {
                try {
                    val canvas = html2canvas(element).await()

                    // Convert canvas to data URL
                    val dataUrl = canvas.toDataURL("image/png")

                    // Convert base64 data URL to ByteArray
                    val base64 = dataUrl.substring(dataUrl.indexOf(",") + 1)
                    val bytes = base64ToByteArray(base64)

                    println("✓ Screenshot '$name' captured successfully (${bytes.size} bytes)")

                    // Append canvas to document for debugging and manual download
                    if (js("typeof document !== 'undefined'") as Boolean) {
                        canvas.setAttribute("data-screenshot-name", name)
                        canvas.setAttribute("id", "screenshot-$name")
                        canvas.style.border = "2px solid #00ff00"
                        canvas.style.margin = "10px"

                        // Create a download link
                        val link = document.createElement("a") as org.w3c.dom.HTMLAnchorElement
                        link.href = dataUrl
                        link.download = "$name.png"
                        link.textContent = "Download $name.png"
                        link.style.display = "block"
                        link.style.padding = "10px"
                        link.style.background = "#f0f0f0"
                        link.style.margin = "10px"

                        // Add to page for manual interaction
                        document.body?.appendChild(canvas)
                        document.body?.appendChild(link)

                        println("Screenshot canvas and download link added to page")
                        println("You can right-click the canvas or use the download link to save the image")
                    }

                    bytes
                } catch (e: Throwable) {
                    console.error("Failed to capture screenshot '$name': ${e.message}")
                    e.printStackTrace()
                    null
                }
            }

            // Since this is async and we can't block in JS, we return null immediately
            // The screenshot will complete in the background and be available in the DOM
            println("Note: Screenshot is being captured asynchronously")
            println("Check the browser for the screenshot canvas and download link")
            null
        } catch (e: Throwable) {
            console.error("Failed to start screenshot capture: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Converts a base64 string to a ByteArray.
     */
    private fun base64ToByteArray(base64: String): ByteArray {
        val binaryString = js("atob")(base64) as String
        val bytes = ByteArray(binaryString.length)
        for (i in binaryString.indices) {
            bytes[i] = binaryString[i].code.toByte()
        }
        return bytes
    }

    actual fun cleanup() {
        rootView?.shutdown()
        rootView = null
    }
}
