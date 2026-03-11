// by Claude - tests for Telemetry singleton lifecycle, exception capture, verbose logging, and noop behavior.
// Uses configureForTesting() to bypass AppScope/Dispatchers.Main which aren't available in unit tests.
package com.lightningkite.kiteui.telemetry

import kotlin.test.*

class TelemetryConfigureTest {

    @BeforeTest
    fun reset() {
        Telemetry.resetForTesting()
    }

    @AfterTest
    fun cleanup() {
        Telemetry.resetForTesting()
    }

    // --- Configure lifecycle ---

    @Test
    fun isInactiveBeforeConfigure() {
        assertFalse(Telemetry.isActive)
        assertNull(Telemetry.config)
        assertNull(Telemetry.exporter)
    }

    @Test
    fun configureForTestingActivatesTelemetry() {
        Telemetry.configureForTesting(TelemetryConfig(
            endpoint = "http://localhost:0/otlp",
        ))
        assertTrue(Telemetry.isActive)
        assertNotNull(Telemetry.config)
        assertNotNull(Telemetry.exporter)
    }

    @Test
    fun configureStoresConfigValues() {
        Telemetry.configureForTesting(TelemetryConfig(
            endpoint = "http://example.com/otlp",
            serviceName = "my-test-app",
            serviceVersion = "2.0.0",
            traceSamplingRate = 0.5,
        ))
        val config = Telemetry.config!!
        assertEquals("http://example.com/otlp", config.endpoint)
        assertEquals("my-test-app", config.serviceName)
        assertEquals("2.0.0", config.serviceVersion)
        assertEquals(0.5, config.traceSamplingRate)
    }

    @Test
    fun doubleConfigureForTestingResets() {
        Telemetry.configureForTesting(TelemetryConfig(
            endpoint = "http://first.com/otlp",
            serviceName = "first",
        ))
        Telemetry.configureForTesting(TelemetryConfig(
            endpoint = "http://second.com/otlp",
            serviceName = "second",
        ))
        // configureForTesting resets and reconfigures
        assertEquals("second", Telemetry.config!!.serviceName)
    }

    // --- Verbose logging ---

    @Test
    fun setVerboseLoggingChangesMinSeverity() {
        Telemetry.configureForTesting(TelemetryConfig(
            endpoint = "http://localhost:0/otlp",
            logMinSeverity = OtlpSeverity.WARN,
        ))
        assertEquals(OtlpSeverity.WARN, Telemetry.config!!.logMinSeverity)

        Telemetry.setVerboseLogging(true)
        assertEquals(OtlpSeverity.DEBUG, Telemetry.config!!.logMinSeverity)

        Telemetry.setVerboseLogging(false)
        assertEquals(OtlpSeverity.WARN, Telemetry.config!!.logMinSeverity)
    }

    @Test
    fun setVerboseLoggingIsNoopWhenNotConfigured() {
        // Should not crash or have any effect
        Telemetry.setVerboseLogging(true)
        assertFalse(Telemetry.isActive)
    }

    // --- Counter and histogram delegation ---

    @Test
    fun counterDelegatesToExporter() {
        Telemetry.configureForTesting(TelemetryConfig(
            endpoint = "http://localhost:0/otlp",
        ))
        Telemetry.counter("custom.count", 5)
        assertEquals(1, Telemetry.exporter!!.counters.size)
        val snapshot = Telemetry.exporter!!.counters.values.single().snapshot(IdGenerator.nanosString())
        assertEquals(5L, snapshot!!.sum!!.dataPoints.single().asInt)
    }

    @Test
    fun histogramDelegatesToExporter() {
        Telemetry.configureForTesting(TelemetryConfig(
            endpoint = "http://localhost:0/otlp",
        ))
        Telemetry.histogram("custom.latency", 42.0)
        assertEquals(1, Telemetry.exporter!!.histograms.size)
    }

    @Test
    fun counterIsNoopWhenNotConfigured() {
        // Should not crash
        Telemetry.counter("noop.counter")
        assertFalse(Telemetry.isActive)
    }

    @Test
    fun histogramIsNoopWhenNotConfigured() {
        // Should not crash
        Telemetry.histogram("noop.latency", 99.0)
        assertFalse(Telemetry.isActive)
    }

    // --- Exception capture ---

    @Test
    fun recordExceptionCreatesLogRecord() {
        Telemetry.configureForTesting(TelemetryConfig(
            endpoint = "http://localhost:0/otlp",
        ))
        val exporter = Telemetry.exporter!!

        Telemetry.recordException(IllegalStateException("test error"), "TestContext")

        val record = exporter.logBuffer.single()
        assertEquals(OtlpSeverity.ERROR.number, record.severityNumber)
        assertTrue(record.body?.stringValue?.contains("test error") == true,
            "Body should contain exception message")
    }

    @Test
    fun recordExceptionIncludesAttributes() {
        Telemetry.configureForTesting(TelemetryConfig(
            endpoint = "http://localhost:0/otlp",
        ))
        val exporter = Telemetry.exporter!!

        Telemetry.recordException(IllegalArgumentException("bad arg"), "MyScreen")

        val record = exporter.logBuffer.single()
        val attrs = record.attributes!!
        val typeAttr = attrs.find { it.key == "exception.type" }
        assertNotNull(typeAttr)
        assertEquals("IllegalArgumentException", typeAttr.value.stringValue)

        val msgAttr = attrs.find { it.key == "exception.message" }
        assertNotNull(msgAttr)
        assertEquals("bad arg", msgAttr.value.stringValue)

        val ctxAttr = attrs.find { it.key == "exception.context" }
        assertNotNull(ctxAttr)
        assertEquals("MyScreen", ctxAttr.value.stringValue)
    }

    @Test
    fun recordExceptionOmitsContextWhenEmpty() {
        Telemetry.configureForTesting(TelemetryConfig(
            endpoint = "http://localhost:0/otlp",
        ))
        val exporter = Telemetry.exporter!!

        Telemetry.recordException(RuntimeException("oops"), "")

        val record = exporter.logBuffer.single()
        val ctxAttr = record.attributes?.find { it.key == "exception.context" }
        assertNull(ctxAttr, "Empty context should not produce exception.context attribute")
    }

    @Test
    fun recordExceptionIsNoopWhenNotConfigured() {
        // Should not crash
        Telemetry.recordException(RuntimeException("noop"), "test")
    }

    @Test
    fun recordExceptionIncludesTraceContext() {
        Telemetry.configureForTesting(TelemetryConfig(
            endpoint = "http://localhost:0/otlp",
        ))
        Telemetry.recordException(RuntimeException("traced"), "ctx")

        val record = Telemetry.exporter!!.logBuffer.single()
        assertEquals(Telemetry.currentTraceId, record.traceId)
    }

    @Test
    fun recordExceptionIncludesSessionId() {
        Telemetry.configureForTesting(TelemetryConfig(
            endpoint = "http://localhost:0/otlp",
        ))
        Telemetry.recordException(RuntimeException("session"), "ctx")

        val record = Telemetry.exporter!!.logBuffer.single()
        val sessionAttr = record.attributes?.find { it.key == "session.id" }
        assertNotNull(sessionAttr)
        assertEquals(Telemetry.sessionId, sessionAttr.value.stringValue)
    }

    // --- Reset ---

    @Test
    fun resetForTestingClearsState() {
        Telemetry.configureForTesting(TelemetryConfig(
            endpoint = "http://localhost:0/otlp",
        ))
        assertTrue(Telemetry.isActive)
        Telemetry.resetForTesting()
        assertFalse(Telemetry.isActive)
        assertNull(Telemetry.config)
        assertNull(Telemetry.exporter)
    }
}
