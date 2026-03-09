// by Claude - iOS synchronization using Foundation NSRecursiveLock
package com.lightningkite.kiteui.aidriver

import platform.Foundation.NSRecursiveLock

private val globalAiDriverLock = NSRecursiveLock()

internal actual inline fun <T> platformSynchronized(lock: Any, block: () -> T): T {
    globalAiDriverLock.lock()
    try {
        return block()
    } finally {
        globalAiDriverLock.unlock()
    }
}
