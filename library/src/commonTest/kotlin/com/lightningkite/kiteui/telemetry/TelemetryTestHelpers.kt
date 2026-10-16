package com.lightningkite.kiteui.telemetry

import com.lightningkite.services.Namespaced
import com.lightningkite.services.SettingContext
import com.lightningkite.services.telemetry.RecordedLog
import com.lightningkite.services.telemetry.RecordedSpan
import com.lightningkite.services.telemetry.TelemetryBackend

internal fun testTelemetry(
    traceSamplingRate: Double = 1.0,
    tracePropagationHosts: List<String> = listOf("example.com"),
) = Telemetry(
    serviceName = "test",
    serviceVersion = "1",
    upload = { error("not uploading in this test") },
    traceSamplingRate = traceSamplingRate,
    tracePropagationHosts = tracePropagationHosts,
)

internal fun Telemetry.owner(name: String): Namespaced {
    val telemetry = this
    return object : Namespaced {
        override val name: String = name
        override val context: SettingContext = object : SettingContext by ClientContext {
            override val telemetryBackend: TelemetryBackend get() = telemetry
        }
    }
}

internal fun Telemetry.recordedSpans(): List<RecordedSpan> {
    outbox.persist()
    return outbox.batches.flatMap { it.spans }
}

internal fun Telemetry.recordedLogs(): List<RecordedLog> {
    outbox.persist()
    return outbox.batches.flatMap { it.logs }
}
