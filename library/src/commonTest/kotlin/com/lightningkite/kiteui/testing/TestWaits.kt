package com.lightningkite.kiteui.testing

import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Utilities for waiting and synchronizing in tests.
 *
 * These helpers are useful when testing reactive updates, animations,
 * or any asynchronous behavior.
 */
object TestWaits {
    /**
     * Waits until a condition becomes true, or times out.
     *
     * Polls the condition at regular intervals until it returns true
     * or the timeout is reached.
     *
     * @param timeout Maximum time to wait (default: 5 seconds)
     * @param pollInterval How often to check the condition (default: 50ms)
     * @param condition The condition to wait for
     * @return true if condition became true, false if timeout was reached
     *
     * Example:
     * ```
     * val success = waitUntil(timeout = 2.seconds) {
     *     viewModel.isLoading.value == false
     * }
     * assertTrue(success, "Loading should complete")
     * ```
     */
    suspend fun waitUntil(
        timeout: Duration = 5.seconds,
        pollInterval: Duration = 50.milliseconds,
        condition: () -> Boolean
    ): Boolean {
        val startTime = currentTimeMillis()
        val timeoutMs = timeout.inWholeMilliseconds

        while (!condition()) {
            val elapsed = currentTimeMillis() - startTime
            if (elapsed >= timeoutMs) {
                return false
            }
            delay(pollInterval)
        }
        return true
    }

    /**
     * Waits until a condition becomes true, throwing an exception on timeout.
     *
     * Similar to waitUntil, but throws AssertionError instead of returning false.
     * This is useful when the condition MUST be met for the test to continue.
     *
     * @param timeout Maximum time to wait (default: 5 seconds)
     * @param pollInterval How often to check the condition (default: 50ms)
     * @param message Error message if timeout is reached
     * @param condition The condition to wait for
     *
     * Example:
     * ```
     * waitFor(message = "Data should load") {
     *     viewModel.data.value != null
     * }
     * ```
     */
    suspend fun waitFor(
        timeout: Duration = 5.seconds,
        pollInterval: Duration = 50.milliseconds,
        message: String = "Condition not met within timeout",
        condition: () -> Boolean
    ) {
        val success = waitUntil(timeout, pollInterval, condition)
        if (!success) {
            throw AssertionError(message)
        }
    }

    /**
     * Waits for a short period to allow reactive updates to propagate.
     *
     * Use this sparingly - it's better to use waitUntil/waitFor with specific
     * conditions. However, this can be useful when you know an update is happening
     * but don't have a good way to observe it.
     *
     * @param duration How long to wait (default: 100ms)
     *
     * Example:
     * ```
     * button.click()
     * waitForUpdate()  // Give reactive system time to process
     * assertEquals("Updated", label.textContent)
     * ```
     */
    suspend fun waitForUpdate(duration: Duration = 100.milliseconds) {
        delay(duration)
    }

    /**
     * Gets the current time in milliseconds.
     * Platform-agnostic way to measure time.
     */
    private fun currentTimeMillis(): Long {
        return kotlin.js.Date.now().toLong()
    }
}

/**
 * Suspending version of withTestHarness that supports async operations.
 *
 * Use this when you need to use delay, waitUntil, or other suspend functions
 * in your tests.
 *
 * Example:
 * ```
 * @Test
 * fun testAsync() = runTest {
 *     withTestHarnessSuspend { harness ->
 *         val root = harness.render { /* UI */ }
 *
 *         button.click()
 *         TestWaits.waitFor { viewModel.isLoaded.value }
 *
 *         assertEquals("Data", label.textContent)
 *     }
 * }
 * ```
 */
suspend inline fun withTestHarnessSuspend(block: suspend (TestHarness) -> Unit) {
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
