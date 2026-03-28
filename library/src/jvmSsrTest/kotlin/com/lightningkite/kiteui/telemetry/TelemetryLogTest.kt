package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.Log
import com.lightningkite.kiteui.LogInterceptor
import com.lightningkite.kiteui.LogLevel
import com.lightningkite.kiteui.logInterceptors
import kotlin.test.*

class TelemetryLogTest {

    /** Records all log calls for verification. */
    private class RecordingInterceptor : LogInterceptor {
        data class Entry(val level: String, val tag: String, val message: String)
        val entries = mutableListOf<Entry>()
        override fun intercept(level: LogLevel, tag: String, entries: Array<out Any?>) {
            this.entries.add(Entry(level.name, tag, entries.joinToString(" ")))
        }
    }

    private var savedInterceptors: List<LogInterceptor> = emptyList()
    private lateinit var telemetry: Telemetry

    @BeforeTest
    fun saveState() {
        savedInterceptors = logInterceptors.toList()
        logInterceptors.clear()
        telemetry = Telemetry(TelemetryConfig(
            endpoint = "http://localhost:0/otlp",
            logMinSeverity = OtlpSeverity.WARN,
            maxBatchSize = 10_000, // prevent auto-flush
        ))
    }

    @AfterTest
    fun restoreState() {
        logInterceptors.clear()
        logInterceptors.addAll(savedInterceptors)
    }

    // --- List-based interceptor preservation ---

    @Test
    fun interceptorAddedToList() {
        val recording = RecordingInterceptor()
        logInterceptors.add(recording)
        val logInterceptor = TelemetryLogInterceptor(telemetry)
        logInterceptors.add(logInterceptor)
        assertEquals(2, logInterceptors.size)
        assertIs<TelemetryLogInterceptor>(logInterceptors.last())
    }

    @Test
    fun allInterceptorsReceiveCalls() {
        val recording = RecordingInterceptor()
        logInterceptors.add(recording)
        logInterceptors.add(TelemetryLogInterceptor(telemetry))

        Log.log("debug msg")
        Log.info("info msg")
        Log.warn("warn msg")
        Log.error("error msg")

        assertEquals(4, recording.entries.size, "All 4 calls should reach recording interceptor")
        assertEquals("LOG", recording.entries[0].level)
        assertEquals("INFO", recording.entries[1].level)
        assertEquals("WARN", recording.entries[2].level)
        assertEquals("ERROR", recording.entries[3].level)
    }

    // --- Severity filtering ---

    @Test
    fun severityFilteringDropsBelowMinimum() {
        val recording = RecordingInterceptor()
        logInterceptors.add(recording)
        logInterceptors.add(TelemetryLogInterceptor(telemetry))
        val exporter = telemetry.exporter

        // Default min severity is WARN, so log() and info() should NOT be recorded as OTel logs
        Log.log("debug message")
        Log.info("info message")
        Log.warn("warn message")
        Log.error("error message")

        // Recording interceptor still gets all 4 calls
        assertEquals(4, recording.entries.size)

        // But only WARN and ERROR should be in the exporter's log buffer
        val otlpLogs = exporter.logBuffer.toList()
        assertEquals(2, otlpLogs.size, "Only WARN and ERROR should be exported")
        assertEquals(OtlpSeverity.WARN.number, otlpLogs[0].severityNumber)
        assertEquals(OtlpSeverity.ERROR.number, otlpLogs[1].severityNumber)
    }

    @Test
    fun verboseLoggingShipsDEBUG() {
        logInterceptors.add(TelemetryLogInterceptor(telemetry))
        val exporter = telemetry.exporter

        telemetry.setVerboseLogging(true)
        Log.log("debug now visible")
        Log.info("info now visible")

        val otlpLogs = exporter.logBuffer.toList()
        assertEquals(2, otlpLogs.size, "Verbose mode should ship DEBUG and INFO")
        assertEquals(OtlpSeverity.DEBUG.number, otlpLogs[0].severityNumber)
        assertEquals(OtlpSeverity.INFO.number, otlpLogs[1].severityNumber)
    }

    // --- Log record content ---

    @Test
    fun logRecordContainsBody() {
        logInterceptors.add(TelemetryLogInterceptor(telemetry))

        Log.warn("something", "went", "wrong")

        val record = telemetry.exporter.logBuffer.single()
        assertEquals("something went wrong", record.body?.stringValue)
    }

    @Test
    fun logRecordContainsSessionId() {
        logInterceptors.add(TelemetryLogInterceptor(telemetry))

        Log.error("test")

        val record = telemetry.exporter.logBuffer.single()
        val sessionAttr = record.attributes.find { it.key == "session.id" }
        assertNotNull(sessionAttr, "Log record should have session.id attribute")
        assertEquals(telemetry.sessionId, sessionAttr.value.stringValue)
    }

    @Test
    fun logRecordContainsTraceContext() {
        logInterceptors.add(TelemetryLogInterceptor(telemetry))

        Log.error("test")

        val record = telemetry.exporter.logBuffer.single()
        assertEquals(telemetry.currentTraceId, record.traceId)
    }

    // --- Tag propagation ---

    @Test
    fun taggedLogIncludesTagAttribute() {
        val recording = RecordingInterceptor()
        logInterceptors.add(recording)
        logInterceptors.add(TelemetryLogInterceptor(telemetry))

        Log.tag("MyComponent").warn("tagged warning")

        // Recording interceptor should receive the tagged call
        val delegateEntry = recording.entries.single()
        assertEquals("WARN", delegateEntry.level)
        assertEquals("MyComponent", delegateEntry.tag)

        // OTel record should include tag
        val record = telemetry.exporter.logBuffer.single()
        val tagAttr = record.attributes.find { it.key == "log.tag" }
        assertNotNull(tagAttr, "Tagged log should have log.tag attribute")
        assertEquals("MyComponent", tagAttr.value.stringValue)
    }

    @Test
    fun nestedTagsConcatenate() {
        logInterceptors.add(TelemetryLogInterceptor(telemetry))

        Log.tag("Parent").tag("Child").warn("nested")

        val record = telemetry.exporter.logBuffer.single()
        val tagAttr = record.attributes.find { it.key == "log.tag" }
        assertNotNull(tagAttr)
        assertEquals("Parent/Child", tagAttr.value.stringValue)
    }

    @Test
    fun untaggedLogHasNoTagAttribute() {
        logInterceptors.add(TelemetryLogInterceptor(telemetry))

        Log.error("no tag")

        val record = telemetry.exporter.logBuffer.single()
        val tagAttr = record.attributes.find { it.key == "log.tag" }
        assertNull(tagAttr, "Untagged log should not have log.tag attribute")
    }
}
