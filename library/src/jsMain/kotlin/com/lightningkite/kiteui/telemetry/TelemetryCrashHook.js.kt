package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.toBlob
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

internal actual fun blockingFlush(exporter: TelemetryExporter) {
    // Use navigator.sendBeacon for best-effort delivery during page unload/error.
    // Unlike AppScope.launch, sendBeacon survives page navigation and unload.
    val navigator = js("navigator")
    val payloads = exporter.drainToPayloads()
    for ((url, body) in payloads) {
        try {
            val blob = body.toBlob("application/json")
            navigator.sendBeacon(url, blob)
        } catch (_: dynamic) {
            // Best effort — if sendBeacon is unavailable, data is lost
        }
    }
}
