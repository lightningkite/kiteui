package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.PlatformStorage
import com.lightningkite.services.telemetry.RecordedLog
import com.lightningkite.services.telemetry.RecordedSpan

internal fun testTelemetry(
    traceSamplingRate: Double = 1.0,
    tracePropagationHosts: List<String> = listOf("example.com"),
): Telemetry {
    clearStoredTelemetry()
    return Telemetry(
        serviceVersion = "1",
        upload = { error("not uploading in this test") },
        traceSamplingRate = traceSamplingRate,
        tracePropagationHosts = tracePropagationHosts,
    )
}

// Claude: PlatformStorage throws in Android unit tests not run under Robolectric, and the SSR target's keeps nothing.
// The outbox survives both, so these helpers do too, rather than depending on which test ran first.

/** Forgets batches earlier tests persisted. In the browser they'd otherwise load into the next outbox from localStorage. */
internal fun clearStoredTelemetry() = try { PlatformStorage.remove(TelemetryOutbox.STORAGE_KEY) } catch (_: Throwable) {}

/** Returns whether [PlatformStorage] keeps what's written to it on this platform. */
internal fun storageKeepsValues(): Boolean = try {
    PlatformStorage.set("kiteui.telemetry.probe", "1")
    (PlatformStorage.get("kiteui.telemetry.probe") != null).also { PlatformStorage.remove("kiteui.telemetry.probe") }
} catch (_: Throwable) {
    false
}

internal fun Telemetry.recordedSpans(): List<RecordedSpan> {
    outbox.persist()
    return outbox.batches.flatMap { it.spans }
}

internal fun Telemetry.recordedLogs(): List<RecordedLog> {
    outbox.persist()
    return outbox.batches.flatMap { it.logs }
}
