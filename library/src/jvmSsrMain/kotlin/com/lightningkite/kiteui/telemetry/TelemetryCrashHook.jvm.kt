package com.lightningkite.kiteui.telemetry

import kotlinx.coroutines.runBlocking

internal actual fun installCrashHook(onCrash: (Throwable) -> Unit) {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        try {
            onCrash(throwable)
        } catch (_: Exception) {}
        previous?.uncaughtException(thread, throwable)
    }
}

internal actual fun blockingFlush(exporter: TelemetryExporter) {
    runBlocking { exporter.flushAll() }
}
