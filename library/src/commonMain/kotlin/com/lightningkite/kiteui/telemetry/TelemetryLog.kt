package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.LogInterceptor
import com.lightningkite.kiteui.LogLevel

internal class TelemetryLogInterceptor(private val telemetry: Telemetry) : LogInterceptor {
    override fun intercept(level: LogLevel, tag: String, entries: Array<out Any?>) {
        val severity = when (level) {
            LogLevel.LOG -> OtlpSeverity.DEBUG
            LogLevel.INFO -> OtlpSeverity.INFO
            LogLevel.WARN -> OtlpSeverity.WARN
            LogLevel.ERROR -> OtlpSeverity.ERROR
        }
        if (severity.number < telemetry.logMinSeverity.number) return
        val msg = entries.joinToString(" ") { it.toString() }
        telemetry.exporter.addLog(
            OtlpLogRecord(
                timeUnixNano = Telemetry.nanosString(),
                severityNumber = severity.number,
                severityText = severity.text,
                body = OtlpAnyValue(stringValue = msg),
                attributes = buildList {
                    if (tag.isNotEmpty()) add(OtlpKeyValue("log.tag", OtlpAnyValue(stringValue = tag)))
                    add(OtlpKeyValue("session.id", OtlpAnyValue(stringValue = telemetry.sessionId)))
                },
                traceId = activeTraceId.ifEmpty { telemetry.currentTraceId },
                spanId = activeSpanId.ifEmpty { telemetry.currentSpanId },
            )
        )
    }
}
