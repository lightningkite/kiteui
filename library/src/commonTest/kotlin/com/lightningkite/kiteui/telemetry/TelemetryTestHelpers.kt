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

/** Forgets batches earlier tests persisted. In the browser they'd otherwise load into the next outbox from localStorage. */
internal fun clearStoredTelemetry() = PlatformStorage.remove(TelemetryOutbox.STORAGE_KEY)

internal fun Telemetry.recordedSpans(): List<RecordedSpan> {
    outbox.persist()
    return outbox.batches.flatMap { it.spans }
}

internal fun Telemetry.recordedLogs(): List<RecordedLog> {
    outbox.persist()
    return outbox.batches.flatMap { it.logs }
}
