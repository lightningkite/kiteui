package com.lightningkite.kiteui.telemetry

/** Installs a platform-specific hook to catch uncaught exceptions before process death. */
internal expect fun installCrashHook(onCrash: (Throwable) -> Unit)

/** Like [com.lightningkite.kiteui.PlatformStorage.set], but on disk before it returns, for the crash path. */
internal expect fun writeDurably(key: String, value: String)
