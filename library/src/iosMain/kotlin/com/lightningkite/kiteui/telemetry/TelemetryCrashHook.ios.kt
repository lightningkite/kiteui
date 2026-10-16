package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.PlatformStorage
import platform.Foundation.NSUserDefaults

// Only catches Kotlin/Native exceptions, not ObjC/Swift crashes.
// setUnhandledExceptionHook replaces the default termination behavior,
// so we must explicitly terminate after recording.
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

internal actual fun writeDurably(key: String, value: String) {
    PlatformStorage.set(key, value)
    NSUserDefaults.standardUserDefaults.synchronize()
}
