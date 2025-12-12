package com.lightningkite.kiteui.testing

import kotlinx.coroutines.delay

/**
 * Waits for a condition to become true, checking periodically.
 * Useful for waiting for async operations like data loading.
 *
 * @param timeoutMs Maximum time to wait in milliseconds
 * @param checkIntervalMs How often to check the condition
 * @param condition The condition to wait for
 * @return true if condition became true, false if timeout
 */
suspend fun waitFor(
    timeoutMs: Long = 10000,
    checkIntervalMs: Long = 100,
    condition: () -> Boolean
): Boolean {
    val startTime = currentTimeMillis()
    while (currentTimeMillis() - startTime < timeoutMs) {
        if (condition()) return true
        delay(checkIntervalMs)
    }
    return false
}

/**
 * Waits for a specified duration.
 * Useful when you need to wait for animations, loading, etc.
 */
suspend fun waitForDuration(durationMs: Long) {
    delay(durationMs)
}

/**
 * Blocks the current thread for the specified duration.
 * Use this in non-suspend test contexts when you need to wait.
 */
expect fun blockingWait(durationMs: Long)

expect fun currentTimeMillis(): Long

/**
 * Waits for reactive updates to process.
 * Call this after changing reactive values to ensure UI updates have been applied.
 *
 * Example:
 * ```
 * enabled.value = false
 * waitForReactiveUpdates()
 * button.assertDisabled()
 * ```
 */
fun waitForReactiveUpdates() {
    // Give the reactive system time to process updates
    // This is a simple approach - just wait a small amount of time
    blockingWait(100)
}
