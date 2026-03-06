// by Claude - verifies html2canvas screenshot produces a real PNG, not blank
package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.testing.uiTest
import com.lightningkite.kiteui.testing.UiTestConfig
import com.lightningkite.kiteui.views.direct.*
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Html2canvasScreenshotTest {
    @Test
    fun screenshotProducesNonTrivialPng() = uiTest(
        config = UiTestConfig(),
        content = {
            col {
                h1 { content = "Screenshot Smoke Test" }
                text { content = "If this text is visible, html2canvas works." }
                button {
                    debugName = "testButton"
                    text { content = "A Button" }
                }
            }
        }
    ) {
        val bytes = screenshot()
        assertNotNull(bytes, "Screenshot should succeed")
        println("Screenshot result: ${bytes.size} bytes")
        // A blank/trivial PNG is ~100-200 bytes. A real screenshot with text should be much larger.
        assertTrue(bytes.size > 500, "Screenshot should be non-trivial (got ${bytes.size} bytes)")
        // Verify PNG magic bytes
        assertTrue(
            bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte(),
            "Should be a valid PNG file"
        )
        // A solid-color blank PNG at typical viewport size compresses to ~1-3KB.
        // An image with text, buttons, and UI elements should be significantly larger.
        assertTrue(bytes.size > 5000, "Screenshot looks blank — only ${bytes.size} bytes (expected >5KB for UI with text)")
        println("Screenshot OK: ${bytes.size} bytes, valid PNG header")
    }
}
