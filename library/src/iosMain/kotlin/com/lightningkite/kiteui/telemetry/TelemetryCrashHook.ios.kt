package com.lightningkite.kiteui.telemetry

import kotlinx.coroutines.runBlocking

// Only catches Kotlin/Native exceptions, not ObjC/Swift crashes.
@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
internal actual fun installCrashHook(onCrash: (Throwable) -> Unit) {
    kotlin.native.setUnhandledExceptionHook { throwable ->
        try {
            onCrash(throwable)
        } catch (_: Exception) {}
        throwable.printStackTrace()
    }
}

internal actual fun blockingFlush(exporter: TelemetryExporter) {
    runBlocking { exporter.flushAll() }
}
