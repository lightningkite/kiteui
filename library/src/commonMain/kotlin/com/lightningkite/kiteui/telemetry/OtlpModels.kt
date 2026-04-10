// OTLP JSON wire format models for traces, metrics, and logs.
// Follows the proto3 JSON mapping: https://opentelemetry.io/docs/specs/otlp/
//
// Serialization strategy: We use encodeDefaults = false (in TelemetryExporter) to match the
// proto3 JSON spec, which omits fields at their default/zero value. For fields where our Kotlin
// constructor default differs from the proto3 zero, we annotate with @EncodeDefault to force
// serialization. Without this, a receiver would interpret the missing field as the proto3 zero
// (e.g., SPAN_KIND_UNSPECIFIED instead of SPAN_KIND_INTERNAL), silently corrupting semantics.
//
// Fields using @EncodeDefault and why:
//   OtlpSpan.kind:                     Kotlin default 1 (INTERNAL), proto3 zero 0 (UNSPECIFIED)
//   OtlpSum.aggregationTemporality:    Kotlin default 1 (DELTA), proto3 zero 0 (UNSPECIFIED)
//   OtlpSum.isMonotonic:               Kotlin default true, proto3 zero false
//   OtlpHistogram.aggregationTemporality: same as OtlpSum
//   OtlpInstrumentationScope.name:     Kotlin default non-empty, proto3 zero ""
package com.lightningkite.kiteui.telemetry

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

// ===== Shared =====

@Serializable
data class OtlpResource(
    val attributes: List<OtlpKeyValue> = emptyList()
)

@Serializable
data class OtlpKeyValue(
    val key: String,
    val value: OtlpAnyValue
)

@Serializable
data class OtlpAnyValue(
    val stringValue: String? = null,
    val intValue: Long? = null,
    val doubleValue: Double? = null,
    val boolValue: Boolean? = null,
) {
    /** Deterministic string representation used for aggregator keys and debugging. */
    override fun toString(): String = when {
        stringValue != null -> stringValue
        intValue != null -> intValue.toString()
        doubleValue != null -> doubleValue.toString()
        boolValue != null -> boolValue.toString()
        else -> ""
    }
}

@Serializable
data class OtlpInstrumentationScope(
    // @EncodeDefault: Kotlin default is non-empty but proto3 zero is "". If omitted, the
    // receiver would see "" instead of our SDK name, breaking scope-based filtering/grouping.
    @OptIn(ExperimentalSerializationApi::class) @EncodeDefault val name: String = "com.lightningkite.kiteui",
    val version: String = ""
)

// ===== Traces =====

@Serializable
data class OtlpExportTraceRequest(
    val resourceSpans: List<OtlpResourceSpans>
)

@Serializable
data class OtlpResourceSpans(
    val resource: OtlpResource,
    val scopeSpans: List<OtlpScopeSpans>
)

@Serializable
data class OtlpScopeSpans(
    val scope: OtlpInstrumentationScope,
    val spans: List<OtlpSpan>
)

@Serializable
data class OtlpSpan(
    val traceId: String,               // 32 hex chars
    val spanId: String,                 // 16 hex chars
    val parentSpanId: String = "",
    val name: String,
    // @EncodeDefault: proto3 zero is 0 (UNSPECIFIED), our default is 1 (INTERNAL)
    @OptIn(ExperimentalSerializationApi::class) @EncodeDefault val kind: Int = 1, // SPAN_KIND_INTERNAL=1, CLIENT=3
    val startTimeUnixNano: String,      // nanoseconds as string
    val endTimeUnixNano: String,
    val attributes: List<OtlpKeyValue> = emptyList(),
    val status: OtlpSpanStatus? = null,
)

@Serializable
data class OtlpSpanStatus(
    val code: Int = 0,                  // 0=UNSET, 1=OK, 2=ERROR
    val message: String = ""
)

// ===== Metrics =====

@Serializable
data class OtlpExportMetricsRequest(
    val resourceMetrics: List<OtlpResourceMetrics>
)

@Serializable
data class OtlpResourceMetrics(
    val resource: OtlpResource,
    val scopeMetrics: List<OtlpScopeMetrics>
)

@Serializable
data class OtlpScopeMetrics(
    val scope: OtlpInstrumentationScope,
    val metrics: List<OtlpMetric>
)

@Serializable
data class OtlpMetric(
    val name: String,
    val description: String = "",
    val unit: String = "",
    val sum: OtlpSum? = null,
    val histogram: OtlpHistogram? = null,
)

@Serializable
data class OtlpSum(
    val dataPoints: List<OtlpNumberDataPoint>,
    // @EncodeDefault: proto3 zero is 0 (UNSPECIFIED), our default is 1 (DELTA)
    @OptIn(ExperimentalSerializationApi::class) @EncodeDefault val aggregationTemporality: Int = 1, // DELTA=1, CUMULATIVE=2
    // @EncodeDefault: proto3 zero is false, our default is true (counters are monotonic)
    @OptIn(ExperimentalSerializationApi::class) @EncodeDefault val isMonotonic: Boolean = true,
)

@Serializable
data class OtlpNumberDataPoint(
    val startTimeUnixNano: String,
    val timeUnixNano: String,
    val asInt: Long? = null,
    val asDouble: Double? = null,
    val attributes: List<OtlpKeyValue> = emptyList(),
)

@Serializable
data class OtlpHistogram(
    val dataPoints: List<OtlpHistogramDataPoint>,
    // @EncodeDefault: proto3 zero is 0 (UNSPECIFIED), our default is 1 (DELTA)
    @OptIn(ExperimentalSerializationApi::class) @EncodeDefault val aggregationTemporality: Int = 1, // DELTA
)

@Serializable
data class OtlpHistogramDataPoint(
    val startTimeUnixNano: String,
    val timeUnixNano: String,
    val count: Long,
    val sum: Double,
    val min: Double,
    val max: Double,
    val bucketCounts: List<Long>,
    val explicitBounds: List<Double>,
    val attributes: List<OtlpKeyValue> = emptyList(),
)

// ===== Logs =====

@Serializable
data class OtlpExportLogsRequest(
    val resourceLogs: List<OtlpResourceLogs>
)

@Serializable
data class OtlpResourceLogs(
    val resource: OtlpResource,
    val scopeLogs: List<OtlpScopeLogs>
)

@Serializable
data class OtlpScopeLogs(
    val scope: OtlpInstrumentationScope,
    val logRecords: List<OtlpLogRecord>
)

@Serializable
data class OtlpLogRecord(
    val timeUnixNano: String,
    val severityNumber: Int,           // OtlpSeverity.number
    val severityText: String,
    val body: OtlpAnyValue? = null,
    val attributes: List<OtlpKeyValue> = emptyList(),
    val traceId: String = "",
    val spanId: String = "",
)

enum class OtlpSeverity(val number: Int, val text: String) {
    TRACE(1, "TRACE"),
    DEBUG(5, "DEBUG"),
    INFO(9, "INFO"),
    WARN(13, "WARN"),
    ERROR(17, "ERROR"),
    FATAL(21, "FATAL"),
}
