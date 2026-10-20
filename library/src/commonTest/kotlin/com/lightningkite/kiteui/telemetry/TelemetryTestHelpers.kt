package com.lightningkite.kiteui.telemetry

import com.lightningkite.services.telemetry.RecordedLog
import com.lightningkite.services.telemetry.RecordedSpan

internal fun testTelemetry(
    traceSamplingRate: Double = 1.0,
    tracePropagationHosts: List<String> = listOf("example.com"),
) = Telemetry(
    serviceVersion = "1",
    upload = { error("not uploading in this test") },
    traceSamplingRate = traceSamplingRate,
    tracePropagationHosts = tracePropagationHosts,
)

internal fun Telemetry.recordedSpans(): List<RecordedSpan> {
    outbox.persist()
    return outbox.batches.flatMap { it.spans }
}

internal fun Telemetry.recordedLogs(): List<RecordedLog> {
    outbox.persist()
    return outbox.batches.flatMap { it.logs }
}
