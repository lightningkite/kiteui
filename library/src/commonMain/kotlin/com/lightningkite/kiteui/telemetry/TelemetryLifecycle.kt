// by Claude - app lifecycle tracking for OpenTelemetry.
// Records cold start span, foreground session spans, connectivity issues.
// Flushes telemetry on app background (critical — app may be killed by OS).
package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.reactive.core.AppScope
import kotlinx.coroutines.launch

internal object TelemetryLifecycle {
    private val coldStartNanos = IdGenerator.nanosString()
    private var foregroundCleanup: (() -> Unit)? = null
    private var connectivityCleanup: (() -> Unit)? = null
    private var lastForegroundNanos: String = ""
    private var foregroundSpanId: String = ""

    fun install() {
        recordColdStart()
        installForegroundTracking()
        installConnectivityTracking()
    }

    private fun recordColdStart() {
        val nowNanos = IdGenerator.nanosString()
        Telemetry.exporter?.addSpan(
            OtlpSpan(
                traceId = Telemetry.currentTraceId,
                spanId = IdGenerator.spanId(),
                name = "app.cold_start",
                kind = 1, // SPAN_KIND_INTERNAL
                startTimeUnixNano = coldStartNanos,
                endTimeUnixNano = nowNanos,
                attributes = listOf(
                    OtlpKeyValue("session.id", OtlpAnyValue(stringValue = Telemetry.sessionId)),
                    OtlpKeyValue("os.type", OtlpAnyValue(stringValue = Platform.current.name.lowercase())),
                ),
            )
        )
    }

    private fun installForegroundTracking() {
        // Start as foreground (app is running when telemetry is configured)
        lastForegroundNanos = IdGenerator.nanosString()
        foregroundSpanId = IdGenerator.spanId()

        foregroundCleanup = AppState.inForeground.addListener {
            if (AppState.inForeground.value) {
                onForeground()
            } else {
                onBackground()
            }
        }
    }

    private fun onForeground() {
        lastForegroundNanos = IdGenerator.nanosString()
        foregroundSpanId = IdGenerator.spanId()
        Telemetry.exporter?.incrementCounter("app.foreground_count")
    }

    private fun onBackground() {
        if (lastForegroundNanos.isEmpty()) return

        val config = Telemetry.config
        if (config != null && kotlin.random.Random.nextDouble() <= config.traceSamplingRate) {
            Telemetry.exporter?.addSpan(
                OtlpSpan(
                    traceId = Telemetry.currentTraceId,
                    spanId = foregroundSpanId,
                    name = "app.foreground_session",
                    kind = 1, // SPAN_KIND_INTERNAL
                    startTimeUnixNano = lastForegroundNanos,
                    endTimeUnixNano = IdGenerator.nanosString(),
                    attributes = listOf(
                        OtlpKeyValue("session.id", OtlpAnyValue(stringValue = Telemetry.sessionId)),
                    ),
                )
            )
        }

        lastForegroundNanos = ""

        // by Claude - flush aggressively on background; the OS may kill the app
        AppScope.launch { Telemetry.flush() }
    }

    private fun installConnectivityTracking() {
        connectivityCleanup = Connectivity.lastConnectivityIssueCode.addListener {
            val code = Connectivity.lastConnectivityIssueCode.value
            if (code != 0.toShort()) {
                Telemetry.exporter?.incrementCounter(
                    "connectivity.issues",
                    attributes = listOf(
                        OtlpKeyValue("connectivity.issue_code", OtlpAnyValue(intValue = code.toLong()))
                    )
                )
                Telemetry.exporter?.addLog(
                    OtlpLogRecord(
                        timeUnixNano = IdGenerator.nanosString(),
                        severityNumber = OtlpSeverity.WARN.number,
                        severityText = OtlpSeverity.WARN.text,
                        body = OtlpAnyValue(stringValue = "Connectivity issue: status code $code"),
                        attributes = listOf(
                            OtlpKeyValue("connectivity.issue_code", OtlpAnyValue(intValue = code.toLong())),
                            OtlpKeyValue("session.id", OtlpAnyValue(stringValue = Telemetry.sessionId)),
                        ),
                    )
                )
            }
        }
    }
}
