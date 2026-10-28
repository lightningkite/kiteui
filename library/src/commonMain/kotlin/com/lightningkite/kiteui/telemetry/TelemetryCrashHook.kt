package com.lightningkite.kiteui.telemetry

/** Installs a platform-specific hook to catch uncaught exceptions before process death. */
internal expect fun installCrashHook(onCrash: (Throwable) -> Unit)
