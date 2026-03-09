// by Claude - JVM actual for platformSynchronized (Swing target)
package com.lightningkite.kiteui.aidriver

internal actual inline fun <T> platformSynchronized(lock: Any, block: () -> T): T = synchronized(lock, block)
