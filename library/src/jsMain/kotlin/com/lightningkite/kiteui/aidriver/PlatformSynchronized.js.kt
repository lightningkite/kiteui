// by Claude - JS is single-threaded, no synchronization needed
package com.lightningkite.kiteui.aidriver

internal actual inline fun <T> platformSynchronized(lock: Any, block: () -> T): T = block()
