package com.lightningkite.kiteui.telemetry

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OtlpModelsTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    @Test
    fun traceRequestRoundtrip() {
        val request = OtlpExportTraceRequest(
            resourceSpans = listOf(
                OtlpResourceSpans(
                    resource = OtlpResource(
                        attributes = listOf(
                            OtlpKeyValue("service.name", OtlpAnyValue(stringValue = "test-app"))
                        )
                    ),
                    scopeSpans = listOf(
                        OtlpScopeSpans(
                            scope = OtlpInstrumentationScope(name = "com.lightningkite.kiteui", version = "1.0"),
                            spans = listOf(
                                OtlpSpan(
                                    traceId = "0123456789abcdef0123456789abcdef",
                                    spanId = "0123456789abcdef",
                                    name = "HTTP GET",
                                    kind = 3,
                                    startTimeUnixNano = "1000000000000000000",
                                    endTimeUnixNano = "1000000001000000000",
                                    attributes = listOf(
                                        OtlpKeyValue("http.request.method", OtlpAnyValue(stringValue = "GET"))
                                    ),
                                )
                            )
                        )
                    )
                )
            )
        )
        val encoded = json.encodeToString(OtlpExportTraceRequest.serializer(), request)
        val decoded = json.decodeFromString(OtlpExportTraceRequest.serializer(), encoded)
        assertEquals(request, decoded)
    }

    @Test
    fun metricsRequestRoundtrip() {
        val request = OtlpExportMetricsRequest(
            resourceMetrics = listOf(
                OtlpResourceMetrics(
                    resource = OtlpResource(),
                    scopeMetrics = listOf(
                        OtlpScopeMetrics(
                            scope = OtlpInstrumentationScope(),
                            metrics = listOf(
                                OtlpMetric(
                                    name = "http.client.request.duration",
                                    unit = "ms",
                                    histogram = OtlpHistogram(
                                        dataPoints = listOf(
                                            OtlpHistogramDataPoint(
                                                startTimeUnixNano = "1000",
                                                timeUnixNano = "2000",
                                                count = 5,
                                                sum = 250.0,
                                                min = 10.0,
                                                max = 100.0,
                                                bucketCounts = listOf(1, 2, 2),
                                                explicitBounds = listOf(25.0, 50.0),
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
        val encoded = json.encodeToString(OtlpExportMetricsRequest.serializer(), request)
        val decoded = json.decodeFromString(OtlpExportMetricsRequest.serializer(), encoded)
        assertEquals(request, decoded)
    }

    @Test
    fun logsRequestRoundtrip() {
        val request = OtlpExportLogsRequest(
            resourceLogs = listOf(
                OtlpResourceLogs(
                    resource = OtlpResource(),
                    scopeLogs = listOf(
                        OtlpScopeLogs(
                            scope = OtlpInstrumentationScope(),
                            logRecords = listOf(
                                OtlpLogRecord(
                                    timeUnixNano = "1000000000000000000",
                                    severityNumber = OtlpSeverity.ERROR.number,
                                    severityText = OtlpSeverity.ERROR.text,
                                    body = OtlpAnyValue(stringValue = "Something failed"),
                                    attributes = listOf(
                                        OtlpKeyValue("exception.type", OtlpAnyValue(stringValue = "RuntimeException"))
                                    ),
                                    traceId = "0123456789abcdef0123456789abcdef",
                                    spanId = "0123456789abcdef",
                                )
                            )
                        )
                    )
                )
            )
        )
        val encoded = json.encodeToString(OtlpExportLogsRequest.serializer(), request)
        val decoded = json.decodeFromString(OtlpExportLogsRequest.serializer(), encoded)
        assertEquals(request, decoded)
    }

    @Test
    fun traceJsonContainsExpectedFields() {
        val span = OtlpSpan(
            traceId = "abcdef0123456789abcdef0123456789",
            spanId = "1234567890abcdef",
            name = "test-span",
            startTimeUnixNano = "1000",
            endTimeUnixNano = "2000",
        )
        val encoded = json.encodeToString(OtlpSpan.serializer(), span)
        assertTrue("\"traceId\"" in encoded, "Should contain traceId field")
        assertTrue("\"spanId\"" in encoded, "Should contain spanId field")
        assertTrue("\"startTimeUnixNano\"" in encoded, "Should contain startTimeUnixNano")
        assertTrue("\"abcdef0123456789abcdef0123456789\"" in encoded, "traceId value should be hex string")
    }

    @Test
    fun severityEnumValues() {
        assertEquals(1, OtlpSeverity.TRACE.number)
        assertEquals(5, OtlpSeverity.DEBUG.number)
        assertEquals(9, OtlpSeverity.INFO.number)
        assertEquals(13, OtlpSeverity.WARN.number)
        assertEquals(17, OtlpSeverity.ERROR.number)
        assertEquals(21, OtlpSeverity.FATAL.number)
    }

    @Test
    fun anyValueVariants() {
        val stringVal = OtlpAnyValue(stringValue = "hello")
        val intVal = OtlpAnyValue(intValue = 42)
        val doubleVal = OtlpAnyValue(doubleValue = 3.14)
        val boolVal = OtlpAnyValue(boolValue = true)

        // Roundtrip each
        for (v in listOf(stringVal, intVal, doubleVal, boolVal)) {
            val encoded = json.encodeToString(OtlpAnyValue.serializer(), v)
            val decoded = json.decodeFromString(OtlpAnyValue.serializer(), encoded)
            assertEquals(v, decoded)
        }
    }
}
