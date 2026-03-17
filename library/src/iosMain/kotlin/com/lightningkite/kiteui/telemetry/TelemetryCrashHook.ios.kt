package com.lightningkite.kiteui.telemetry

import kotlinx.coroutines.runBlocking

// Only catches Kotlin/Native exceptions, not ObjC/Swift crashes.
// setUnhandledExceptionHook replaces the default termination behavior,
// so we must explicitly terminate after flushing.
@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
internal actual fun installCrashHook(onCrash: (Throwable) -> Unit) {
    val previous = kotlin.native.getUnhandledExceptionHook()
    kotlin.native.setUnhandledExceptionHook { throwable ->
        try {
            onCrash(throwable)
        } catch (_: Exception) {}
        if (previous != null) {
            previous(throwable)
        } else {
            throwable.printStackTrace()
            kotlin.system.exitProcess(1)
        }
    }
}

internal actual fun blockingFlush(exporter: TelemetryExporter) {
    runBlocking { exporter.flushAll() }
}
