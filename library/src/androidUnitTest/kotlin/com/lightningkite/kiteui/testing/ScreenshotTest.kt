package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.direct.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test screenshot functionality.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTest {

    @Test
    fun testBasicScreenshot() = withTestHarness { harness ->
        val theme = Theme(
            id = "test-light",
            background = Color.white,
            foreground = Color.black
        )
        val root = harness.render(theme) {
            col {
                text("Screenshot Test").apply { debugName = "title" }
                text("This view should be captured")
            }
        }

        // Capture screenshot
        val screenshot = harness.screenshot("test-screenshot")
        assertNotNull(screenshot, "Screenshot should not be null")
        assertTrue(screenshot.isNotEmpty(), "Screenshot should contain data")

        // Save to disk for manual inspection
        saveScreenshot(screenshot, "basic-screenshot")
    }

    @Test
    fun testViewScreenshot() = withTestHarness { harness ->
        val root = harness.render {
            col {
                row {
                    debugName = "test-row"
                    text("Left")
                    text("Right")
                }
                text("Below row")
            }
        }

        // Capture screenshot of specific view
        val rowView = root.findByDebugName("test-row")
        assertNotNull(rowView, "Row should be found")

        val screenshot = harness.screenshotView(rowView!!, "row-screenshot")
        assertNotNull(screenshot, "Screenshot should not be null")
        assertTrue(screenshot.isNotEmpty(), "Screenshot should contain data")

        // Save to disk
        saveScreenshot(screenshot, "row-only-screenshot")
    }

    @Test
    fun testComplexLayoutScreenshot() = withTestHarness { harness ->
        val root = harness.render {
            col {
                text("Header").apply { debugName = "header" }
                row {
                    text("Item 1")
                    text("Item 2")
                    text("Item 3")
                }
                frame {
                    text("Framed Content")
                }
                col {
                    text("Nested 1")
                    text("Nested 2")
                }
            }
        }

        val screenshot = harness.screenshot("complex-layout")
        assertNotNull(screenshot, "Screenshot should not be null")
        assertTrue(screenshot.isNotEmpty(), "Screenshot should contain data")

        saveScreenshot(screenshot, "complex-layout-screenshot")
    }

    private fun saveScreenshot(data: ByteArray, name: String) {
        try {
            // Save to project's local/screenshots directory
            val screenshotsDir = File("local/screenshots/android")
            screenshotsDir.mkdirs()

            val file = File(screenshotsDir, "$name.png")
            file.writeBytes(data)

            println("Screenshot saved to: ${file.absolutePath}")
        } catch (e: Exception) {
            println("Failed to save screenshot: ${e.message}")
        }
    }
}
