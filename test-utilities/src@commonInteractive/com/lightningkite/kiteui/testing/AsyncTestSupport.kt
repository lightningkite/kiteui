package com.lightningkite.kiteui.testing

import kotlin.time.Duration

/**
 * Platform-specific support for async testing operations.
 *
 * Provides utilities for waiting, advancing time, and ensuring UI updates are complete
 * in tests. Each platform implements these differently based on their async model:
 * - Android: Uses Espresso's IdlingRegistry and shadow looper
 * - iOS: Uses XCTest expectations and RunLoop
 * - JS: Uses event loop, microtasks, and requestAnimationFrame
 */
expect class AsyncTestSupport() {
    /**
     * Waits until the platform's async queue is idle.
     * This ensures all pending UI updates, animations, and async operations have completed.
     *
     * Platform implementations:
     * - Android: Waits for Espresso IdlingResources and flushes the main looper
     * - iOS: Runs the RunLoop until no tasks remain
     * - JS: Yields to flush microtasks, posts to macrotask queue, awaits animation frame
     */
    suspend fun waitUntilIdle()

    /**
     * Waits for a condition to become true, checking periodically.
     * Ensures UI rendering happens between checks.
     *
     * @param timeout Maximum time to wait
     * @param condition The condition to wait for
     * @throws AssertionError if timeout is reached before condition becomes true
     */
    suspend fun waitFor(timeout: Duration, condition: () -> Boolean)

    /**
     * Advances time in the test environment.
     *
     * Platform implementations:
     * - Android: Advances Robolectric's shadow looper by the specified duration
     * - iOS: Advances the test scheduler/RunLoop
     * - JS: Uses actual delay (no virtual time control available)
     *
     * @param duration The amount of time to advance
     */
    suspend fun advanceTimeBy(duration: Duration)
}
