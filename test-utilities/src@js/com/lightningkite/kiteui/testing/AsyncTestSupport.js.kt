package com.lightningkite.kiteui.testing

import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.js.Date
import kotlin.time.Duration

/**
 * JS implementation of AsyncTestSupport.
 *
 * Uses JavaScript's event loop, microtasks, macrotasks, and requestAnimationFrame
 * to ensure async operations and UI rendering are complete.
 */
actual class AsyncTestSupport {
    /**
     * Waits until the JS event loop is idle.
     *
     * This performs three operations in sequence:
     * 1. Yields to flush microtasks (Promise callbacks, etc.)
     * 2. Posts to macrotask queue via delay(0) to ensure all pending tasks run
     * 3. Awaits animation frame to ensure DOM updates have been applied
     */
    actual suspend fun waitUntilIdle() {
        // Yield to flush microtasks queue
        kotlinx.coroutines.yield()
        
        // Post to macrotask queue to ensure all pending tasks run
        delay(0)
        
        // Await animation frame to ensure DOM rendering is complete
        awaitAnimationFrame()
    }

    /**
     * Waits for a condition to become true, checking periodically.
     *
     * @param timeout Maximum time to wait
     * @param condition The condition to wait for
     * @throws AssertionError if timeout is reached before condition becomes true
     */
    actual suspend fun waitFor(timeout: Duration, condition: () -> Boolean) {
        val startTime = Date.now()
        val timeoutMs = timeout.inWholeMilliseconds.toDouble()
        
        while (Date.now() - startTime < timeoutMs) {
            if (condition()) {
                return
            }
            
            // Delay to yield to event loop and prevent busy-waiting
            delay(50)
            
            // Await animation frame to ensure rendering has occurred
            awaitAnimationFrame()
        }
        
        // Final check before failing
        if (condition()) {
            return
        }
        
        throw AssertionError("Condition not met within timeout of $timeout")
    }

    /**
     * Advances time by the specified duration.
     *
     * Note: JavaScript doesn't have virtual time control in standard test environments,
     * so this uses an actual delay. For more sophisticated time control, consider
     * using a library like Sinon.js with fake timers.
     *
     * @param duration The amount of time to advance
     */
    actual suspend fun advanceTimeBy(duration: Duration) {
        delay(duration.inWholeMilliseconds)
    }

    /**
     * Suspends until the next animation frame.
     * This ensures that any pending DOM updates have been rendered.
     */
    private suspend fun awaitAnimationFrame() = suspendCoroutine<Unit> { continuation ->
        window.requestAnimationFrame {
            continuation.resume(Unit)
        }
    }
}
