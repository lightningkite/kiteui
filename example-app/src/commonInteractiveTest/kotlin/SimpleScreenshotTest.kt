package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.testing.GraphicsMode
import com.lightningkite.kiteui.testing.GraphicsModeEnum
import com.lightningkite.kiteui.testing.JUnitRunWith
import com.lightningkite.kiteui.testing.RobolectricConfig
import com.lightningkite.kiteui.testing.RobolectricTestRunner
import com.lightningkite.kiteui.testing.withTestHarness
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Simple screenshot test to verify the test infrastructure works.
 */
@JUnitRunWith(RobolectricTestRunner::class)
@RobolectricConfig
@GraphicsMode(GraphicsModeEnum.NATIVE)
class SimpleScreenshotTest {

    @Test
    fun testSimpleScreenshot() {
        withTestHarness { harness ->
            println("\n=== Test: Simple Screenshot ===")

            // Render simple UI
            val root = harness.render {
                col {
                    h1 { content = "Hello Screenshot Test" }
                    text("This is a test")
                }
            }

            assertNotNull(root)
            println("✓ UI rendered")

            // Take screenshot
            println("📸 Taking screenshot...")
            val screenshot = harness.screenshot("simple-test")

            if (screenshot != null) {
                println("✓ Screenshot captured (${screenshot.size} bytes)")
            } else {
                println("⚠️  Screenshot not available")
            }
        }
    }
}
