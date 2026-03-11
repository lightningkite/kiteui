package com.lightningkite.kiteui.telemetry

import kotlin.test.*

class TelemetryExporterTest {

    private fun testConfig(
        maxQueueSize: Int = 500,
        maxBatchSize: Int = 10_000, // large to prevent auto-flush (which requires AppScope)
    ) = TelemetryConfig(
        endpoint = "http://localhost:0/otlp", // unreachable; export will fail silently
        maxQueueSize = maxQueueSize,
        maxBatchSize = maxBatchSize,
    )

    private fun testSpan(name: String = "test.span") = OtlpSpan(
        traceId = Telemetry.traceId(),
        spanId = Telemetry.spanId(),
        name = name,
        kind = 1,
        startTimeUnixNano = Telemetry.nanosString(),
        endTimeUnixNano = Telemetry.nanosString(),
    )

    private fun testLog(body: String = "test log") = OtlpLogRecord(
        timeUnixNano = Telemetry.nanosString(),
        severityNumber = OtlpSeverity.WARN.number,
        severityText = OtlpSeverity.WARN.text,
        body = OtlpAnyValue(stringValue = body),
    )

    // --- Span buffer ---

    @Test
    fun addSpanBuffersSpan() {
        val exporter = TelemetryExporter(testConfig())
        exporter.addSpan(testSpan("first"))
        assertEquals(1, exporter.spanBuffer.size)
        assertEquals("first", exporter.spanBuffer.first().name)
    }

    @Test
    fun addSpanOverflowDropsOldest() {
        val exporter = TelemetryExporter(testConfig(maxQueueSize = 3))
        exporter.addSpan(testSpan("a"))
        exporter.addSpan(testSpan("b"))
        exporter.addSpan(testSpan("c"))
        exporter.addSpan(testSpan("d"))
        exporter.addSpan(testSpan("e"))
        assertEquals(3, exporter.spanBuffer.size, "Buffer should be capped at maxQueueSize")
        assertEquals("c", exporter.spanBuffer[0].name, "Oldest should be dropped")
        assertEquals("d", exporter.spanBuffer[1].name)
        assertEquals("e", exporter.spanBuffer[2].name)
    }

    // --- Log buffer ---

    @Test
    fun addLogBuffersLog() {
        val exporter = TelemetryExporter(testConfig())
        exporter.addLog(testLog("hello"))
        assertEquals(1, exporter.logBuffer.size)
        assertEquals("hello", exporter.logBuffer.first().body?.stringValue)
    }

    @Test
    fun addLogOverflowDropsOldest() {
        val exporter = TelemetryExporter(testConfig(maxQueueSize = 2))
        exporter.addLog(testLog("first"))
        exporter.addLog(testLog("second"))
        exporter.addLog(testLog("third"))
        assertEquals(2, exporter.logBuffer.size, "Buffer should be capped at maxQueueSize")
        assertEquals("second", exporter.logBuffer[0].body?.stringValue)
        assertEquals("third", exporter.logBuffer[1].body?.stringValue)
    }

    // --- Counter delegation ---

    @Test
    fun incrementCounterCreatesAggregator() {
        val exporter = TelemetryExporter(testConfig())
        exporter.incrementCounter("test.count")
        assertEquals(1, exporter.counters.size)
    }

    @Test
    fun incrementCounterAccumulates() {
        val exporter = TelemetryExporter(testConfig())
        exporter.incrementCounter("test.count", 3)
        exporter.incrementCounter("test.count", 7)
        val counter = exporter.counters.values.single()
        val snapshot = counter.snapshot(Telemetry.nanosString())
        assertNotNull(snapshot)
        assertEquals(10L, snapshot.sum!!.dataPoints.single().asInt)
    }

    @Test
    fun counterKeyDiffersByAttributes() {
        val exporter = TelemetryExporter(testConfig())
        exporter.incrementCounter("http.requests", attributes = listOf(
            OtlpKeyValue("method", OtlpAnyValue(stringValue = "GET"))
        ))
        exporter.incrementCounter("http.requests", attributes = listOf(
            OtlpKeyValue("method", OtlpAnyValue(stringValue = "POST"))
        ))
        assertEquals(2, exporter.counters.size, "Different attributes should create separate aggregators")
    }

    @Test
    fun counterKeyMatchesSameAttributes() {
        val exporter = TelemetryExporter(testConfig())
        val attrs = listOf(OtlpKeyValue("method", OtlpAnyValue(stringValue = "GET")))
        exporter.incrementCounter("test", attributes = attrs)
        exporter.incrementCounter("test", attributes = attrs)
        assertEquals(1, exporter.counters.size, "Same name+attributes should reuse aggregator")
        val snapshot = exporter.counters.values.single().snapshot(Telemetry.nanosString())
        assertEquals(2L, snapshot!!.sum!!.dataPoints.single().asInt)
    }

    // --- Histogram delegation ---

    @Test
    fun recordHistogramCreatesAggregator() {
        val exporter = TelemetryExporter(testConfig())
        exporter.recordHistogram("latency", 42.0, "ms")
        assertEquals(1, exporter.histograms.size)
    }

    @Test
    fun recordHistogramAccumulates() {
        val exporter = TelemetryExporter(testConfig())
        exporter.recordHistogram("latency", 10.0)
        exporter.recordHistogram("latency", 20.0)
        val hist = exporter.histograms.values.single()
        val snapshot = hist.snapshot(Telemetry.nanosString())!!
        val dp = snapshot.histogram!!.dataPoints.single()
        assertEquals(2L, dp.count)
        assertEquals(30.0, dp.sum)
    }

    @Test
    fun histogramKeyDiffersByAttributes() {
        val exporter = TelemetryExporter(testConfig())
        exporter.recordHistogram("latency", 10.0, attributes = listOf(
            OtlpKeyValue("host", OtlpAnyValue(stringValue = "a.com"))
        ))
        exporter.recordHistogram("latency", 20.0, attributes = listOf(
            OtlpKeyValue("host", OtlpAnyValue(stringValue = "b.com"))
        ))
        assertEquals(2, exporter.histograms.size)
    }

    // --- Empty buffer behavior ---

    @Test
    fun emptyExporterHasNoBufferedData() {
        val exporter = TelemetryExporter(testConfig())
        assertTrue(exporter.spanBuffer.isEmpty())
        assertTrue(exporter.logBuffer.isEmpty())
        assertTrue(exporter.counters.isEmpty())
        assertTrue(exporter.histograms.isEmpty())
    }
}
