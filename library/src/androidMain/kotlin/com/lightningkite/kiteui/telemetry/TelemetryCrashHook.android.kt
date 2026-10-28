package com.lightningkite.kiteui.telemetry

internal actual fun installCrashHook(onCrash: (Throwable) -> Unit) {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        try {
            onCrash(throwable)
        } catch (_: Exception) {}
        previous?.uncaughtException(thread, throwable)
    }
}
