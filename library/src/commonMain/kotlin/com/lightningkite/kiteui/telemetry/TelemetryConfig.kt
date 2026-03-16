package com.lightningkite.kiteui.telemetry

/**
 * Configures where and how telemetry data is exported.
 *
 * Minimal usage: just provide [endpoint] and [headers].
 * Everything else has sensible defaults following the KiteUI philosophy of
 * "make the correct decision the default."
 */
data class TelemetryConfig(
    /** OTLP HTTP endpoint, e.g. "https://otlp-gateway-prod-us-central-0.grafana.net/otlp" */
    val endpoint: String,
    /** HTTP headers for authentication, e.g. mapOf("Authorization" to "Basic ...") */
    val headers: Map<String, String> = emptyMap(),
    /** Identifies this application in the telemetry backend. */
    val serviceName: String = "kiteui-app",
    /** Application version string, attached to all exported data. */
    val serviceVersion: String = "",

    // Batching
    /** How often to flush buffered data, in milliseconds. */
    val flushIntervalMs: Long = 30_000L,
    /** Flush immediately when any buffer reaches this size. */
    val maxBatchSize: Int = 100,
    /** Drop oldest entries when a buffer exceeds this size. */
    val maxQueueSize: Int = 500,

    // Sampling
    /** Fraction of traces to export (0.0 to 1.0). Metrics are always exported. */
    val traceSamplingRate: Double = 1.0,

    // Logs
    /** Only ship log records at or above this severity. */
    val logMinSeverity: OtlpSeverity = OtlpSeverity.WARN,

    // Exception enrichment
    /** Called on every exception to provide extra attributes (e.g. user ID, feature flags). */
    val exceptionAttributes: () -> List<OtlpKeyValue> = { emptyList() },
)
