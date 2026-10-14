package com.lightningkite.kiteui.telemetry

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

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
    try {
        runBlocking { withTimeout(3_000) { exporter.flushAll() } }
    } catch (_: Exception) {
        // Best effort — if network is unreachable or times out, data is lost.
    }
}
