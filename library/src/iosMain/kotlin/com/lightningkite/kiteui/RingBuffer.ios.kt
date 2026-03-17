package com.lightningkite.kiteui

import platform.Foundation.NSLock

private val nsLock = NSLock()

internal actual fun <T> platformSynchronized(lock: Any, block: () -> T): T {
    nsLock.lock()
    try {
        return block()
    } finally {
        nsLock.unlock()
    }
}
