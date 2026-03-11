package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.Log
import com.lightningkite.kiteui.LogRoot
import com.lightningkite.kiteui.logInterceptor

internal class TelemetryLog(private val telemetry: Telemetry) : Log {
    private var delegate: Log = LogRoot

    /** Installs this interceptor into the log chain. Captures the current interceptor as delegate. */
    fun install() {
        delegate = logInterceptor ?: LogRoot
        logInterceptor = this
    }

    private fun maybeRecord(severity: OtlpSeverity, tag: String, entries: Array<out Any?>) {
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
                traceId = telemetry.currentTraceId,
                spanId = telemetry.currentSpanId,
            )
        )
    }

    override fun tag(tag: String): Log = TelemetryTaggedLog(tag, delegate.tag(tag))
    override fun log(vararg entries: Any?) { maybeRecord(OtlpSeverity.DEBUG, "", entries); delegate.log(*entries) }
    override fun info(vararg entries: Any?) { maybeRecord(OtlpSeverity.INFO, "", entries); delegate.info(*entries) }
    override fun warn(vararg entries: Any?) { maybeRecord(OtlpSeverity.WARN, "", entries); delegate.warn(*entries) }
    override fun error(vararg entries: Any?) { maybeRecord(OtlpSeverity.ERROR, "", entries); delegate.error(*entries) }

    private inner class TelemetryTaggedLog(val tag: String, val delegate: Log) : Log {
        override fun tag(tag: String) = TelemetryTaggedLog("${this.tag}/$tag", delegate.tag(tag))
        override fun log(vararg entries: Any?) { maybeRecord(OtlpSeverity.DEBUG, tag, entries); delegate.log(*entries) }
        override fun info(vararg entries: Any?) { maybeRecord(OtlpSeverity.INFO, tag, entries); delegate.info(*entries) }
        override fun warn(vararg entries: Any?) { maybeRecord(OtlpSeverity.WARN, tag, entries); delegate.warn(*entries) }
        override fun error(vararg entries: Any?) { maybeRecord(OtlpSeverity.ERROR, tag, entries); delegate.error(*entries) }
    }
}
