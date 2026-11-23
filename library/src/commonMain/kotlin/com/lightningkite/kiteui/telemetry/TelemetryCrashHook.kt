package com.lightningkite.kiteui.telemetry

/**
 * Installs a platform-specific hook to catch uncaught exceptions before process death.
 *
 * Whatever [onCrash] throws, even an [Error], is swallowed, so the handler that was there before always runs.
 */
internal expect fun installCrashHook(onCrash: (Throwable) -> Unit)
