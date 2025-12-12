package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.direct.*
import platform.Foundation.*
import kotlinx.cinterop.*
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test screenshot functionality on iOS.
 */
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
        saveScreenshot(screenshot, "basic-screenshot-ios")
    }

    @Test
    fun testViewScreenshot() = withTestHarness { harness ->
        val theme = Theme(
            id = "test-light",
            background = Color.white,
            foreground = Color.black
        )
        val root = harness.render(theme) {
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
        saveScreenshot(screenshot, "row-only-screenshot-ios")
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun saveScreenshot(data: ByteArray, name: String) {
        try {
            // Convert ByteArray to NSData
            val nsData = data.usePinned { pinned ->
                NSData.dataWithBytes(pinned.addressOf(0), data.size.toULong())
            }

            // Use temporary directory for iOS tests
            val fileManager = NSFileManager.defaultManager
            val tempDir = NSTemporaryDirectory()
            val screenshotsDir = "${tempDir}screenshots/ios"

            // Create directory if it doesn't exist
            fileManager.createDirectoryAtPath(
                screenshotsDir,
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )

            // Write file
            val filePath = "$screenshotsDir/$name.png"
            val success = nsData.writeToFile(filePath, atomically = true)

            if (success) {
                println("✅ Screenshot saved to: $filePath")

                // Also try to copy to project directory if possible
                val projectPath = "/Users/jivie/Projects/kiteui/library/local/screenshots/ios"
                fileManager.createDirectoryAtPath(
                    projectPath,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = null
                )
                val projectFile = "$projectPath/$name.png"
                fileManager.copyItemAtPath(filePath, toPath = projectFile, error = null)
                println("📁 Also copied to: $projectFile")
            } else {
                println("❌ Failed to write file to: $filePath")
            }
        } catch (e: Exception) {
            println("Failed to save screenshot: ${e.message}")
            e.printStackTrace()
        }
    }
}
