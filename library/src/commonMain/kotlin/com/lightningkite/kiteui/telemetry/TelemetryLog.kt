// by Claude - Log interceptor that ships log records via OpenTelemetry.
// Filters by severity, converts to OtlpLogRecord, and delegates to the previous interceptor/LogRoot.
package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.Log
import com.lightningkite.kiteui.LogRoot
import com.lightningkite.kiteui.logInterceptor

internal object TelemetryLog : Log {
    private var delegate: Log = LogRoot

    /** Installs this interceptor into the log chain. Captures the current interceptor as delegate. */
    fun install() {
        delegate = logInterceptor ?: LogRoot
        logInterceptor = this
    }

    private fun maybeRecord(severity: OtlpSeverity, tag: String, entries: Array<out Any?>) {
        val config = Telemetry.config ?: return
        if (severity.number < config.logMinSeverity.number) return
        val exporter = Telemetry.exporter ?: return
        val msg = entries.joinToString(" ") { it.toString() }
        exporter.addLog(
            OtlpLogRecord(
                timeUnixNano = IdGenerator.nanosString(),
                severityNumber = severity.number,
                severityText = severity.text,
                body = OtlpAnyValue(stringValue = msg),
                attributes = buildList {
                    if (tag.isNotEmpty()) add(OtlpKeyValue("log.tag", OtlpAnyValue(stringValue = tag)))
                    add(OtlpKeyValue("session.id", OtlpAnyValue(stringValue = Telemetry.sessionId)))
                },
                traceId = Telemetry.currentTraceId,
                spanId = Telemetry.currentSpanId,
            )
        )
    }

    override fun tag(tag: String): Log = TelemetryTaggedLog(tag, delegate.tag(tag))
    override fun log(vararg entries: Any?) { maybeRecord(OtlpSeverity.DEBUG, "", entries); delegate.log(*entries) }
    override fun info(vararg entries: Any?) { maybeRecord(OtlpSeverity.INFO, "", entries); delegate.info(*entries) }
    override fun warn(vararg entries: Any?) { maybeRecord(OtlpSeverity.WARN, "", entries); delegate.warn(*entries) }
    override fun error(vararg entries: Any?) { maybeRecord(OtlpSeverity.ERROR, "", entries); delegate.error(*entries) }

    private class TelemetryTaggedLog(val tag: String, val delegate: Log) : Log {
        override fun tag(tag: String) = TelemetryTaggedLog("${this.tag}/$tag", delegate.tag(tag))
        override fun log(vararg entries: Any?) { maybeRecord(OtlpSeverity.DEBUG, tag, entries); delegate.log(*entries) }
        override fun info(vararg entries: Any?) { maybeRecord(OtlpSeverity.INFO, tag, entries); delegate.info(*entries) }
        override fun warn(vararg entries: Any?) { maybeRecord(OtlpSeverity.WARN, tag, entries); delegate.warn(*entries) }
        override fun error(vararg entries: Any?) { maybeRecord(OtlpSeverity.ERROR, tag, entries); delegate.error(*entries) }
    }
}
