// by Claude - public API for KiteUI OpenTelemetry integration.
// Call Telemetry.configure() once at app startup; everything else is automatic.
package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.LogRoot
import com.lightningkite.kiteui.Throwable_report

/**
 * KiteUI's built-in OpenTelemetry integration.
 *
 * Minimal usage:
 * ```kotlin
 * Telemetry.configure(
 *     endpoint = "https://otlp-gateway-prod-us-central-0.grafana.net/otlp",
 *     headers = mapOf("Authorization" to "Basic $base64Token")
 * )
 * ```
 *
 * When configured, the following are auto-instrumented:
 * - HTTP request spans and latency histograms
 * - Page navigation spans and view counters
 * - App lifecycle (cold start, foreground sessions)
 * - Connectivity issues
 * - Warn/Error log records and exception reports
 *
 * When not configured, overhead is a single boolean check per instrumentation point.
 */
object Telemetry {
    private var _config: TelemetryConfig? = null

    /** Current configuration, or null if not yet configured. */
    val config: TelemetryConfig? get() = _config

    internal var exporter: TelemetryExporter? = null
        private set

    /** Whether telemetry has been configured and is active. */
    val isActive: Boolean get() = _config != null

    // by Claude - session ID generated once per app launch, attached to all exported data
    internal val sessionId: String = IdGenerator.spanId()

    // by Claude - current trace context for correlating spans and logs
    internal var currentTraceId: String = IdGenerator.traceId()
    internal var currentSpanId: String = ""

    private val log = LogRoot.tag("Telemetry")

    /**
     * Configure and activate telemetry export with minimal parameters.
     * All auto-instrumentation hooks are installed automatically.
     *
     * @param endpoint OTLP HTTP endpoint (e.g. "https://otlp-gateway-prod-us-central-0.grafana.net/otlp")
     * @param headers HTTP headers for authentication (e.g. mapOf("Authorization" to "Basic ..."))
     * @param serviceName Identifies this app in the telemetry backend
     */
    fun configure(
        endpoint: String,
        headers: Map<String, String> = emptyMap(),
        serviceName: String = "kiteui-app",
    ) {
        configure(
            TelemetryConfig(
                endpoint = endpoint,
                headers = headers,
                serviceName = serviceName,
            )
        )
    }

    /**
     * Configure and activate telemetry export with full control.
     * All auto-instrumentation hooks are installed automatically.
     */
    fun configure(config: TelemetryConfig) {
        if (_config != null) {
            log.warn("Telemetry already configured; ignoring reconfiguration")
            return
        }
        _config = config
        exporter = TelemetryExporter(config)

        // Install auto-instrumentation hooks
        TelemetryLog.install()
        installExceptionCapture()
        TelemetryLifecycle.install()
        // Fetch instrumentation is inline in connectivityFetch() — checks Telemetry.isActive
        // Navigation instrumentation is auto-bound in appBase() — checks Telemetry.isActive

        // Start the background flush loop
        exporter?.startFlushLoop()

        log.info("Telemetry configured: endpoint=${config.endpoint}, service=${config.serviceName}")
    }

    /**
     * Temporarily enable verbose (DEBUG+) log shipping for investigation.
     * Call with `false` to restore the default (WARN+).
     */
    fun setVerboseLogging(enabled: Boolean) {
        val config = _config ?: return
        _config = config.copy(
            logMinSeverity = if (enabled) OtlpSeverity.DEBUG else OtlpSeverity.WARN
        )
    }

    /** Record a custom counter metric. */
    fun counter(name: String, value: Long = 1, attributes: List<OtlpKeyValue> = emptyList()) {
        exporter?.incrementCounter(name, value, attributes)
    }

    /** Record a custom histogram metric value. */
    fun histogram(name: String, value: Double, unit: String = "ms", attributes: List<OtlpKeyValue> = emptyList()) {
        exporter?.recordHistogram(name, value, unit, attributes)
    }

    /** Flush all buffered data immediately. Call before app termination or on background. */
    suspend fun flush() {
        exporter?.flushAll()
    }

    /** Shut down telemetry, flush remaining data, and remove hooks. */
    suspend fun shutdown() {
        exporter?.flushAll()
        exporter?.stop()
        _config = null
        exporter = null
    }

    // by Claude - resets state without network calls, for test isolation only
    internal fun resetForTesting() {
        exporter?.stop()
        _config = null
        exporter = null
    }

    // by Claude - configure without starting flush loop or lifecycle hooks, for test isolation only.
    // Tests can't use the real configure() because AppScope/Dispatchers.Main aren't available.
    internal fun configureForTesting(config: TelemetryConfig) {
        resetForTesting()
        _config = config
        exporter = TelemetryExporter(config)
    }

    // by Claude - chains into the existing Throwable_report global to capture exceptions as OTel log records
    private fun installExceptionCapture() {
        val previousHandler = Throwable_report
        Throwable_report = { throwable, context ->
            previousHandler(throwable, context)
            recordException(throwable, context)
        }
    }

    internal fun recordException(throwable: Throwable, context: String) {
        val exporter = exporter ?: return
        exporter.addLog(
            OtlpLogRecord(
                timeUnixNano = IdGenerator.nanosString(),
                severityNumber = OtlpSeverity.ERROR.number,
                severityText = OtlpSeverity.ERROR.text,
                body = OtlpAnyValue(stringValue = throwable.stackTraceToString()),
                attributes = buildList {
                    add(OtlpKeyValue("exception.type", OtlpAnyValue(stringValue = throwable::class.simpleName ?: "Unknown")))
                    add(OtlpKeyValue("exception.message", OtlpAnyValue(stringValue = throwable.message ?: "")))
                    if (context.isNotEmpty()) {
                        add(OtlpKeyValue("exception.context", OtlpAnyValue(stringValue = context)))
                    }
                    add(OtlpKeyValue("session.id", OtlpAnyValue(stringValue = sessionId)))
                },
                traceId = currentTraceId,
                spanId = currentSpanId,
            )
        )
    }
}
