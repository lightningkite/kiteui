package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewWriter

/**
 * Cross-platform test harness for KiteUI testing.
 *
 * This class provides a platform-independent API for setting up UI tests.
 * Each platform has its own implementation using expect/actual pattern.
 */
expect class TestHarness() {
    val supported: Boolean

    /**
     * Renders UI using the provided ViewWriter lambda and returns the root view.
     * This sets up the appropriate test environment for each platform.
     *
     * @param theme The theme to use for rendering (defaults to test theme)
     * @param content The ViewWriter lambda that builds the UI
     * @return The root RView that was created
     */
    fun render(theme: Theme = Theme(id = "test"), content: ViewWriter.() -> Unit): RView

    /**
     * Captures a screenshot of the entire rendered UI.
     *
     * @param name Optional name for the screenshot file (without extension)
     * @return ByteArray containing the PNG image data, or null if screenshots not supported
     */
    fun screenshot(name: String = "screenshot"): ByteArray?

    /**
     * Captures a screenshot of a specific view.
     *
     * @param view The view to capture
     * @param name Optional name for the screenshot file (without extension)
     * @return ByteArray containing the PNG image data, or null if screenshots not supported
     */
    fun screenshotView(view: RView, name: String = "screenshot"): ByteArray?

    /**
     * Cleans up the test environment.
     * Call this after your test is done to avoid memory leaks.
     */
    fun cleanup()
}

/**
 * Helper function to run a test with automatic cleanup.
 * Automatically skips test if platform is not supported.
 *
 * Example:
 * ```
 * @Test
 * fun myTest() = withTestHarness { harness ->
 *     val root = harness.render {
 *         text("Hello, World!")
 *     }
 *     // assertions here
 * }
 * ```
 */
inline fun withTestHarness(block: (TestHarness) -> Unit) {
    val harness = TestHarness()
    if (!harness.supported) {
        println("Skipping test - platform not supported")
        return
    }
    try {
        block(harness)
    } finally {
        harness.cleanup()
    }
}
