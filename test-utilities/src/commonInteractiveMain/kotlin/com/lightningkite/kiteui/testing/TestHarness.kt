package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.ViewWriter
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Cross-platform test harness for KiteUI testing.
 *
 * This class provides a platform-independent API for setting up UI tests.
 * Each platform has its own implementation using expect/actual pattern.
 */
expect class TestHarness() {
    val supported: Boolean

    /**
     * Async testing support for waiting and time control.
     * Use this to wait for async operations or advance virtual time.
     */
    val async: AsyncTestSupport

    /**
     * Renders UI using the provided ViewWriter lambda and returns the root view.
     * This sets up the appropriate test environment for each platform.
     *
     * @param theme The theme to use for rendering (defaults to test theme)
     * @param content The ViewWriter lambda that builds the UI
     * @return The root RView that was created
     */
    fun render(theme: Theme = Theme(id = "test"), content: ViewWriter.() -> Unit): Element

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
    fun screenshotView(view: Element, name: String = "screenshot"): ByteArray?

    /**
     * Cleans up the test environment.
     * Call this after your test is done to avoid memory leaks.
     */
    fun cleanup()

    // Convenience methods that delegate to AsyncTestSupport

    /**
     * Waits until all pending async operations are complete.
     * Convenience method that delegates to async.waitUntilIdle()
     */
    suspend fun waitUntilIdle()

    /**
     * Waits until a condition becomes true or timeout is reached.
     * Convenience method that delegates to async.waitFor()
     */
    suspend fun waitFor(timeout: Duration = 5.seconds, condition: () -> Boolean)

    /**
     * Advances virtual time by the specified duration.
     * Convenience method that delegates to async.advanceTimeBy()
     */
    suspend fun advanceTimeBy(duration: Duration)
}

/**
 * Helper function to run a test with automatic cleanup.
 *
 * Skips the test body on platforms where the harness is unsupported. Every current platform
 * reports [TestHarness.supported] as true, so this only guards a platform added later; skipping
 * keeps such a platform's build green rather than failing every interactive test at once.
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
