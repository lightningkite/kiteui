package com.lightningkite.kiteui

import platform.Foundation.NSLock

private val mapLock = NSLock()
private val locks = HashMap<Any, NSLock>()

internal actual fun <T> platformSynchronized(lock: Any, block: () -> T): T {
    mapLock.lock()
    val nsLock = locks.getOrPut(lock) { NSLock() }
    nsLock.lock()
    mapLock.unlock()
    try {
        return block()
    } finally {
        nsLock.unlock()
    }
}
