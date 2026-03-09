// by Claude
package com.lightningkite.kiteui.aidriver

internal actual inline fun <T> platformSynchronized(lock: Any, block: () -> T): T = synchronized(lock, block)
