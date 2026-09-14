package com.lightningkite.kiteui.testing

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay
import platform.Foundation.*
import kotlin.time.Duration

/**
 * iOS implementation of AsyncTestSupport.
 *
 * Uses RunLoop processing to handle async operations in tests:
 * - NSRunLoop.currentRunLoop().runMode() processes pending events on the main run loop
 * - kotlinx.coroutines.delay() for actual delays (iOS can't advance virtual time)
 *
 * Note: Unlike Android/Robolectric, iOS tests must use real time delays.
 * There's no virtual time advancement capability, so tests will be slower.
 */
@OptIn(ExperimentalForeignApi::class)
actual class AsyncTestSupport {
    /**
     * Waits until all pending async operations on the main run loop are complete.
     *
     * This drains the run loop by processing all pending events and sources.
     * May need a small delay to allow events to be queued before processing.
     */
    actual suspend fun waitUntilIdle() {
        // Process the run loop multiple times to ensure all pending work is done
        // Use a short timeout to avoid blocking indefinitely
        repeat(10) {
            NSRunLoop.currentRunLoop().runMode(
                mode = NSDefaultRunLoopMode,
                beforeDate = NSDate.dateWithTimeIntervalSinceNow(0.01)
            )
        }
    }

    /**
     * Waits until a condition becomes true or timeout is reached.
     *
     * Strategy:
     * 1. Check the condition immediately
     * 2. If false, process the run loop to handle pending work
     * 3. Check the condition again
     * 4. Repeat with polling interval until timeout
     * 5. Throw AssertionError if timeout is exceeded
     *
     * Note: Uses actual time delays since iOS can't advance virtual time.
     */
    actual suspend fun waitFor(timeout: Duration, condition: () -> Boolean) {
        val startTime = NSDate().timeIntervalSince1970
        val timeoutSeconds = timeout.inWholeMilliseconds / 1000.0
        val pollingIntervalMs = 50L // 50ms polling interval

        // First check - condition might already be true
        if (condition()) {
            return
        }

        // Process the run loop once to handle any pending work
        NSRunLoop.currentRunLoop().runMode(
            mode = NSDefaultRunLoopMode,
            beforeDate = NSDate.dateWithTimeIntervalSinceNow(0.01)
        )
        if (condition()) {
            return
        }

        // Poll the condition with run loop processing between checks
        while (true) {
            val elapsed = NSDate().timeIntervalSince1970 - startTime

            if (elapsed >= timeoutSeconds) {
                // Timeout - condition never became true
                throw AssertionError("Condition did not become true within $timeout")
            }

            // Delay to avoid busy-waiting and allow coroutines to run
            delay(pollingIntervalMs)

            // Process the run loop to handle any pending events
            NSRunLoop.currentRunLoop().runMode(
                mode = NSDefaultRunLoopMode,
                beforeDate = NSDate.dateWithTimeIntervalSinceNow(0.1)
            )

            // Check condition after processing
            if (condition()) {
                return
            }
        }
    }

    /**
     * Advances time by the specified duration.
     *
     * iOS Note: Unlike Android/Robolectric, iOS cannot advance virtual time.
     * This method uses actual delay via kotlinx.coroutines.delay().
     *
     * This means tests using advanceTimeBy() will actually wait for the specified duration,
     * making tests slower but more realistic.
     *
     * @param duration Amount of time to wait (actual real time)
     */
    actual suspend fun advanceTimeBy(duration: Duration) {
        delay(duration.inWholeMilliseconds)

        // Process the run loop after the delay to handle any work scheduled during the wait
        NSRunLoop.currentRunLoop().runMode(
            mode = NSDefaultRunLoopMode,
            beforeDate = NSDate.dateWithTimeIntervalSinceNow(0.01)
        )
    }
}
