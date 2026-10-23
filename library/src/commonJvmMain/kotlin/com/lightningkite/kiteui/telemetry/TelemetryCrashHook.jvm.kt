package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.PlatformStorage

internal actual fun installCrashHook(onCrash: (Throwable) -> Unit) {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        try {
            onCrash(throwable)
        } catch (_: Exception) {}
        previous?.uncaughtException(thread, throwable)
    }
}

internal actual fun writeDurably(key: String, value: String) = PlatformStorage.set(key, value)
