package com.lightningkite.kiteui.telemetry

/** Installs a platform-specific hook to catch uncaught exceptions before process death. */
internal expect fun installCrashHook(onCrash: (Throwable) -> Unit)

/** Best-effort synchronous flush of buffered telemetry. Called from crash hooks. */
internal expect fun blockingFlush(exporter: TelemetryExporter)
