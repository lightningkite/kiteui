package com.lightningkite.kiteui.telemetry

import kotlin.test.*

class TelemetryConfigureTest {

    private fun testConfig(
        endpoint: String = "http://localhost:0/otlp",
        serviceName: String = "kiteui-app",
        serviceVersion: String = "",
        traceSamplingRate: Double = 1.0,
        logMinSeverity: OtlpSeverity = OtlpSeverity.WARN,
    ) = TelemetryConfig(
        endpoint = endpoint,
        serviceName = serviceName,
        serviceVersion = serviceVersion,
        traceSamplingRate = traceSamplingRate,
        logMinSeverity = logMinSeverity,
    )

    // --- Constructor ---

    @Test
    fun constructorCreatesExporter() {
        val t = Telemetry(testConfig())
        assertNotNull(t.exporter)
    }

    @Test
    fun constructorStoresConfigValues() {
        val t = Telemetry(testConfig(
            endpoint = "http://example.com/otlp",
            serviceName = "my-test-app",
            serviceVersion = "2.0.0",
            traceSamplingRate = 0.5,
        ))
        assertEquals("http://example.com/otlp", t.config.endpoint)
        assertEquals("my-test-app", t.config.serviceName)
        assertEquals("2.0.0", t.config.serviceVersion)
        assertEquals(0.5, t.config.traceSamplingRate)
    }

    // --- Verbose logging ---

    @Test
    fun setVerboseLoggingChangesMinSeverity() {
        val t = Telemetry(testConfig(logMinSeverity = OtlpSeverity.WARN))
        assertEquals(OtlpSeverity.WARN, t.logMinSeverity)

        t.setVerboseLogging(true)
        assertEquals(OtlpSeverity.DEBUG, t.logMinSeverity)

        t.setVerboseLogging(false)
        assertEquals(OtlpSeverity.WARN, t.logMinSeverity)
    }

    // --- Counter and histogram delegation ---

    @Test
    fun counterDelegatesToExporter() {
        val t = Telemetry(testConfig())
        t.counter("custom.count", 5)
        assertEquals(1, t.exporter.counters.size)
        val snapshot = t.exporter.counters.values.single().snapshot(Telemetry.nanosString())
        assertEquals(5L, snapshot!!.sum!!.dataPoints.single().asInt)
    }

    @Test
    fun histogramDelegatesToExporter() {
        val t = Telemetry(testConfig())
        t.histogram("custom.latency", 42.0)
        assertEquals(1, t.exporter.histograms.size)
    }

    // --- Exception capture ---

    @Test
    fun recordExceptionCreatesLogRecord() {
        val t = Telemetry(testConfig())
        t.recordException(IllegalStateException("test error"), "TestContext")

        val record = t.exporter.logBuffer.single()
        assertEquals(OtlpSeverity.ERROR.number, record.severityNumber)
        assertTrue(record.body?.stringValue?.contains("test error") == true,
            "Body should contain exception message")
    }

    @Test
    fun recordExceptionIncludesAttributes() {
        val t = Telemetry(testConfig())
        t.recordException(IllegalArgumentException("bad arg"), "MyScreen")

        val record = t.exporter.logBuffer.single()
        val attrs = record.attributes
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
        val t = Telemetry(testConfig())
        t.recordException(RuntimeException("oops"), "")

        val record = t.exporter.logBuffer.single()
        val ctxAttr = record.attributes.find { it.key == "exception.context" }
        assertNull(ctxAttr, "Empty context should not produce exception.context attribute")
    }

    @Test
    fun recordExceptionIncludesTraceContext() {
        val t = Telemetry(testConfig())
        t.recordException(RuntimeException("traced"), "ctx")

        val record = t.exporter.logBuffer.single()
        assertEquals(t.currentTraceId, record.traceId)
    }

    @Test
    fun recordExceptionIncludesSessionId() {
        val t = Telemetry(testConfig())
        t.recordException(RuntimeException("session"), "ctx")

        val record = t.exporter.logBuffer.single()
        val sessionAttr = record.attributes.find { it.key == "session.id" }
        assertNotNull(sessionAttr)
        assertEquals(t.sessionId, sessionAttr.value.stringValue)
    }

    // --- Crash fingerprint and severity ---

    @Test
    fun recordExceptionDefaultsToErrorSeverity() {
        val t = Telemetry(testConfig())
        t.recordException(RuntimeException("err"), "ctx")

        val record = t.exporter.logBuffer.single()
        assertEquals(OtlpSeverity.ERROR.number, record.severityNumber)
        assertEquals(OtlpSeverity.ERROR.text, record.severityText)
    }

    @Test
    fun recordExceptionAcceptsFatalSeverity() {
        val t = Telemetry(testConfig())
        t.recordException(RuntimeException("crash"), "uncaught", OtlpSeverity.FATAL)

        val record = t.exporter.logBuffer.single()
        assertEquals(OtlpSeverity.FATAL.number, record.severityNumber)
        assertEquals(OtlpSeverity.FATAL.text, record.severityText)
    }

    @Test
    fun recordExceptionIncludesCrashFingerprint() {
        val t = Telemetry(testConfig())
        val ex = IllegalStateException("fingerprint test")
        t.recordException(ex, "ctx")

        val record = t.exporter.logBuffer.single()
        val fpAttr = record.attributes.find { it.key == "crash.fingerprint" }
        assertNotNull(fpAttr, "Should have crash.fingerprint attribute")

        val fp = fpAttr.value.stringValue!!
        assertEquals(16, fp.length, "Fingerprint should be 16 hex chars: $fp")
        assertTrue(Regex("^[0-9a-f]+$").matches(fp), "Fingerprint should be hex: $fp")
        assertEquals(CrashFingerprint.generate(ex), fp, "Fingerprint should match CrashFingerprint.generate()")
    }

    @Test
    fun recordExceptionIncludesStacktraceAttribute() {
        val t = Telemetry(testConfig())
        val ex = RuntimeException("stacktrace test")
        t.recordException(ex, "ctx")

        val record = t.exporter.logBuffer.single()
        val stAttr = record.attributes.find { it.key == "exception.stacktrace" }
        assertNotNull(stAttr, "Should have exception.stacktrace attribute")
        assertTrue(stAttr.value.stringValue!!.contains("stacktrace test"),
            "Stacktrace attribute should contain exception message")
    }

    @Test
    fun recordExceptionIncludesOsType() {
        val t = Telemetry(testConfig())
        t.recordException(RuntimeException("os"), "ctx")

        val record = t.exporter.logBuffer.single()
        val osAttr = record.attributes.find { it.key == "os.type" }
        assertNotNull(osAttr, "Should have os.type attribute")
        assertTrue(osAttr.value.stringValue!!.isNotEmpty(), "os.type should not be empty")
    }

    @Test
    fun customExceptionAttributesAreIncluded() {
        val t = Telemetry(testConfig().copy(
            exceptionAttributes = {
                listOf(
                    OtlpKeyValue("user.id", OtlpAnyValue(stringValue = "user-42")),
                    OtlpKeyValue("feature.flag", OtlpAnyValue(stringValue = "dark-mode")),
                )
            }
        ))
        t.recordException(RuntimeException("custom"), "ctx")

        val record = t.exporter.logBuffer.single()
        val userAttr = record.attributes.find { it.key == "user.id" }
        assertNotNull(userAttr, "Should have custom user.id attribute")
        assertEquals("user-42", userAttr.value.stringValue)

        val flagAttr = record.attributes.find { it.key == "feature.flag" }
        assertNotNull(flagAttr, "Should have custom feature.flag attribute")
        assertEquals("dark-mode", flagAttr.value.stringValue)
    }

    @Test
    fun customExceptionAttributesErrorDoesNotBreakRecording() {
        val t = Telemetry(testConfig().copy(
            exceptionAttributes = { error("kaboom") }
        ))
        t.recordException(RuntimeException("still works"), "ctx")

        val record = t.exporter.logBuffer.single()
        assertEquals(OtlpSeverity.ERROR.number, record.severityNumber)
    }

    @Test
    fun sameExceptionProducesSameFingerprintAcrossCalls() {
        val t = Telemetry(testConfig())
        val ex = IllegalArgumentException("consistent")
        t.recordException(ex, "call1")
        t.recordException(ex, "call2")

        val records = t.exporter.logBuffer
        assertEquals(2, records.size)
        val fp1 = records[0].attributes.find { it.key == "crash.fingerprint" }!!.value.stringValue
        val fp2 = records[1].attributes.find { it.key == "crash.fingerprint" }!!.value.stringValue
        assertEquals(fp1, fp2, "Same exception should produce same fingerprint")
    }
}
