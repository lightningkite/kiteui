package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.PlatformStorage
import kotlinx.browser.window
import org.w3c.dom.events.Event

internal actual fun installCrashHook(onCrash: (Throwable) -> Unit) {
    window.addEventListener("error", { event: Event ->
        val error = event.asDynamic().error
        val throwable = if (error is Throwable) error
            else Exception(event.asDynamic().message?.toString() ?: "Unknown error")
        try { onCrash(throwable) } catch (_: Exception) {}
    })
    window.addEventListener("unhandledrejection", { event: Event ->
        val reason = event.asDynamic().reason
        val throwable = if (reason is Throwable) reason
            else Exception(reason?.toString() ?: "Unhandled promise rejection")
        try { onCrash(throwable) } catch (_: Exception) {}
    })
}

// localStorage writes are synchronous.
internal actual fun writeDurably(key: String, value: String) = PlatformStorage.set(key, value)
