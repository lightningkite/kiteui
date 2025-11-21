package com.lightningkite.kiteui.testing

actual fun blockingWait(durationMs: Long) {
    Thread.sleep(durationMs)
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()
