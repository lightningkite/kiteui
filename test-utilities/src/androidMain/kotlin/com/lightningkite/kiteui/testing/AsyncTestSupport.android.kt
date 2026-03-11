package com.lightningkite.kiteui.testing

import android.os.Looper
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Android/Robolectric implementation of AsyncTestSupport.
 *
 * Uses Robolectric's scheduler control to manage async operations:
 * - shadowOf(Looper.getMainLooper()) provides access to the main thread's scheduler
 * - idle() processes all pending tasks until none remain
 * - idleFor(duration) advances time and processes tasks scheduled within that time
 */
actual class AsyncTestSupport {
    private val mainLooper: ShadowLooper
        get() = shadowOf(Looper.getMainLooper())

    /**
     * Waits until all pending async operations on the main looper are complete.
     * This includes posted runnables, delayed messages, and pending handlers.
     */
    actual suspend fun waitUntilIdle() {
        mainLooper.idle()
    }

    /**
     * Waits until a condition becomes true or timeout is reached.
     *
     * Strategy:
     * 1. Check the condition immediately
     * 2. If false, idle the main looper to process pending work
     * 3. Check the condition again
     * 4. Repeat with incremental time advancement if needed
     * 5. Throw AssertionError if timeout is exceeded
     */
    actual suspend fun waitFor(timeout: Duration, condition: () -> Boolean) {
        val startTime = System.currentTimeMillis()
        val timeoutMillis = timeout.inWholeMilliseconds

        // First check - condition might already be true
        if (condition()) {
            return
        }

        // Try idling to process pending work
        mainLooper.idle()
        if (condition()) {
            return
        }

        // If still not true, try advancing time incrementally
        // This helps with delayed tasks that haven't been processed yet
        val checkInterval = 100.milliseconds
        var elapsed = 0L

        while (elapsed < timeoutMillis) {
            // Advance time by a small increment
            mainLooper.idleFor(checkInterval.inWholeMilliseconds, java.util.concurrent.TimeUnit.MILLISECONDS)

            // Check condition after processing work
            if (condition()) {
                return
            }

            elapsed = System.currentTimeMillis() - startTime
        }

        // Timeout - condition never became true
        throw AssertionError("Condition did not become true within $timeout")
    }

    /**
     * Advances virtual time by the specified duration.
     *
     * This processes all tasks scheduled to run within the duration,
     * but doesn't actually pause/sleep the test thread.
     */
    actual suspend fun advanceTimeBy(duration: Duration) {
        mainLooper.idleFor(duration.inWholeMilliseconds, java.util.concurrent.TimeUnit.MILLISECONDS)
    }
}
