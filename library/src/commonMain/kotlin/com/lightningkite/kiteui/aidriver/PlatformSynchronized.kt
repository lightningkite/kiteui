// by Claude - KMP-safe synchronized block for thread safety in AI driver internals
package com.lightningkite.kiteui.aidriver

internal expect inline fun <T> platformSynchronized(lock: Any, block: () -> T): T
