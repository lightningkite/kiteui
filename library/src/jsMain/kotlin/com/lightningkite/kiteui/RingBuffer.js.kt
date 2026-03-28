package com.lightningkite.kiteui

internal actual fun <T> platformSynchronized(lock: Any, block: () -> T): T = block()
