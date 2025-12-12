package com.lightningkite.kiteui.testing

import kotlin.js.Date

actual fun blockingWait(durationMs: Long) {
    // JavaScript can't truly block, so we use a busy-wait loop
    // This is not ideal but works for testing purposes
    val endTime = Date.now() + durationMs
    while (Date.now() < endTime) {
        // Busy wait
    }
}

actual fun currentTimeMillis(): Long = Date.now().toLong()
