package com.lightningkite.kiteui.testing

import platform.Foundation.NSDate
import platform.Foundation.NSThread
import platform.Foundation.timeIntervalSince1970

actual fun blockingWait(durationMs: Long) {
    NSThread.sleepForTimeInterval(durationMs / 1000.0)
}

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
