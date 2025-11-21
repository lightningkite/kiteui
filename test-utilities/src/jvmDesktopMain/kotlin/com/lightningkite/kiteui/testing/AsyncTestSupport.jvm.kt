package com.lightningkite.kiteui.testing

import java.util.concurrent.TimeoutException
import javax.swing.SwingUtilities
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * JVM/Swing implementation of AsyncTestSupport.
 *
 * Uses Swing's Event Dispatch Thread (EDT) to manage async operations:
 * - SwingUtilities.invokeAndWait() flushes the EDT queue
 * - Thread.sleep() for polling delays
 * - System.currentTimeMillis() for timeout tracking
 *
 * Note: JVM desktop doesn't have virtual time advancement like Robolectric,
 * so advanceTimeBy() uses actual delays, similar to iOS.
 */
actual class AsyncTestSupport {
    /**
     * Waits until all pending async operations on the Swing EDT are complete.
     * This flushes the EDT queue by posting and waiting for a no-op task.
     *
     * If already on the EDT, processes events in-place. Otherwise, uses invokeAndWait.
     */
    actual suspend fun waitUntilIdle() {
        if (SwingUtilities.isEventDispatchThread()) {
            // Already on EDT - can't invokeAndWait from EDT as it would deadlock
            // Just return immediately; caller should handle this appropriately
            return
        }

        try {
            // Post a no-op task to EDT and wait for it to complete
            // This ensures all previously queued tasks have been processed
            SwingUtilities.invokeAndWait { /* no-op */ }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw AssertionError("Interrupted while waiting for EDT to idle", e)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw AssertionError("Exception occurred on EDT while waiting", e.cause)
        }
    }

    /**
     * Waits until a condition becomes true or timeout is reached.
     *
     * Strategy:
     * 1. Check the condition immediately
     * 2. If false, flush the EDT to process pending work
     * 3. Check the condition again
     * 4. Repeat with polling interval until timeout
     * 5. Throw TimeoutException if timeout is exceeded
     *
     * Note: Uses actual time delays since JVM desktop can't advance virtual time.
     */
    actual suspend fun waitFor(timeout: Duration, condition: () -> Boolean) {
        val startTime = System.currentTimeMillis()
        val timeoutMillis = timeout.inWholeMilliseconds
        val pollingInterval = 50.milliseconds

        // First check - condition might already be true
        if (condition()) {
            return
        }

        // Try flushing EDT to process pending work
        waitUntilIdle()
        if (condition()) {
            return
        }

        // Poll the condition with EDT flushing between checks
        while (true) {
            val elapsed = System.currentTimeMillis() - startTime

            if (elapsed >= timeoutMillis) {
                // Timeout - condition never became true
                throw TimeoutException("Condition did not become true within $timeout")
            }

            // Sleep briefly to avoid busy-waiting
            try {
                Thread.sleep(pollingInterval.inWholeMilliseconds)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw AssertionError("Interrupted while waiting for condition", e)
            }

            // Flush the EDT to handle any pending events
            waitUntilIdle()

            // Check condition after processing
            if (condition()) {
                return
            }
        }
    }

    /**
     * Advances time by the specified duration.
     *
     * JVM Note: Unlike Android/Robolectric, JVM Swing cannot advance virtual time.
     * This method uses actual delay via Thread.sleep().
     *
     * This means tests using advanceTimeBy() will actually wait for the specified duration,
     * making tests slower but more realistic.
     *
     * Alternative implementation: Could just flush EDT and return immediately for faster tests,
     * but this wouldn't test actual time-based behavior.
     *
     * @param duration Amount of time to wait (actual real time)
     */
    actual suspend fun advanceTimeBy(duration: Duration) {
        try {
            Thread.sleep(duration.inWholeMilliseconds)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw AssertionError("Interrupted while advancing time", e)
        }

        // Flush the EDT after the delay to handle any work scheduled during the wait
        waitUntilIdle()
    }
}
