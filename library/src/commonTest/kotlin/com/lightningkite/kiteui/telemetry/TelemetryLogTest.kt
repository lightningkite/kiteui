package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.Log
import com.lightningkite.kiteui.logInterceptor
import kotlin.test.*

class TelemetryLogTest {

    /** Records all log calls for verification. */
    private class RecordingLog : Log {
        data class Entry(val level: String, val tag: String, val message: String)
        val entries = mutableListOf<Entry>()
        override fun tag(tag: String): Log = TaggedRecordingLog(tag, this)
        override fun log(vararg entries: Any?) { this.entries.add(Entry("log", "", entries.joinToString(" "))) }
        override fun info(vararg entries: Any?) { this.entries.add(Entry("info", "", entries.joinToString(" "))) }
        override fun warn(vararg entries: Any?) { this.entries.add(Entry("warn", "", entries.joinToString(" "))) }
        override fun error(vararg entries: Any?) { this.entries.add(Entry("error", "", entries.joinToString(" "))) }
    }

    private class TaggedRecordingLog(val tag: String, val parent: RecordingLog) : Log {
        override fun tag(tag: String): Log = TaggedRecordingLog("${this.tag}/$tag", parent)
        override fun log(vararg entries: Any?) { parent.entries.add(RecordingLog.Entry("log", tag, entries.joinToString(" "))) }
        override fun info(vararg entries: Any?) { parent.entries.add(RecordingLog.Entry("info", tag, entries.joinToString(" "))) }
        override fun warn(vararg entries: Any?) { parent.entries.add(RecordingLog.Entry("warn", tag, entries.joinToString(" "))) }
        override fun error(vararg entries: Any?) { parent.entries.add(RecordingLog.Entry("error", tag, entries.joinToString(" "))) }
    }

    private var savedInterceptor: Log? = null
    private lateinit var telemetry: Telemetry

    @BeforeTest
    fun saveState() {
        savedInterceptor = logInterceptor
        telemetry = Telemetry(TelemetryConfig(
            endpoint = "http://localhost:0/otlp",
            logMinSeverity = OtlpSeverity.WARN,
            maxBatchSize = 10_000, // prevent auto-flush
        ))
    }

    @AfterTest
    fun restoreState() {
        logInterceptor = savedInterceptor
    }

    // --- Delegate chain preservation ---

    @Test
    fun installPreservesExistingInterceptor() {
        val recording = RecordingLog()
        logInterceptor = recording
        TelemetryLog(telemetry).install()
        // TelemetryLog should now be the interceptor, with recording as delegate
        assertIs<TelemetryLog>(logInterceptor)
    }

    @Test
    fun delegateReceivesAllCalls() {
        val recording = RecordingLog()
        logInterceptor = recording
        TelemetryLog(telemetry).install()

        Log.log("debug msg")
        Log.info("info msg")
        Log.warn("warn msg")
        Log.error("error msg")

        assertEquals(4, recording.entries.size, "All 4 calls should reach delegate")
        assertEquals("log", recording.entries[0].level)
        assertEquals("info", recording.entries[1].level)
        assertEquals("warn", recording.entries[2].level)
        assertEquals("error", recording.entries[3].level)
    }

    // --- Severity filtering ---

    @Test
    fun severityFilteringDropsBelowMinimum() {
        val recording = RecordingLog()
        logInterceptor = recording
        TelemetryLog(telemetry).install()
        val exporter = telemetry.exporter

        // Default min severity is WARN, so log() and info() should NOT be recorded as OTel logs
        Log.log("debug message")
        Log.info("info message")
        Log.warn("warn message")
        Log.error("error message")

        // Delegate still gets all 4 calls
        assertEquals(4, recording.entries.size)

        // But only WARN and ERROR should be in the exporter's log buffer
        val otlpLogs = exporter.logBuffer.toList()
        assertEquals(2, otlpLogs.size, "Only WARN and ERROR should be exported")
        assertEquals(OtlpSeverity.WARN.number, otlpLogs[0].severityNumber)
        assertEquals(OtlpSeverity.ERROR.number, otlpLogs[1].severityNumber)
    }

    @Test
    fun verboseLoggingShipsDEBUG() {
        val recording = RecordingLog()
        logInterceptor = recording
        TelemetryLog(telemetry).install()
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
        val recording = RecordingLog()
        logInterceptor = recording
        TelemetryLog(telemetry).install()

        Log.warn("something", "went", "wrong")

        val record = telemetry.exporter.logBuffer.single()
        assertEquals("something went wrong", record.body?.stringValue)
    }

    @Test
    fun logRecordContainsSessionId() {
        val recording = RecordingLog()
        logInterceptor = recording
        TelemetryLog(telemetry).install()

        Log.error("test")

        val record = telemetry.exporter.logBuffer.single()
        val sessionAttr = record.attributes.find { it.key == "session.id" }
        assertNotNull(sessionAttr, "Log record should have session.id attribute")
        assertEquals(telemetry.sessionId, sessionAttr.value.stringValue)
    }

    @Test
    fun logRecordContainsTraceContext() {
        val recording = RecordingLog()
        logInterceptor = recording
        TelemetryLog(telemetry).install()

        Log.error("test")

        val record = telemetry.exporter.logBuffer.single()
        assertEquals(telemetry.currentTraceId, record.traceId)
    }

    // --- Tag propagation ---

    @Test
    fun taggedLogIncludesTagAttribute() {
        val recording = RecordingLog()
        logInterceptor = recording
        TelemetryLog(telemetry).install()

        Log.tag("MyComponent").warn("tagged warning")

        // Delegate should receive the tagged call
        val delegateEntry = recording.entries.single()
        assertEquals("warn", delegateEntry.level)
        assertEquals("MyComponent", delegateEntry.tag)

        // OTel record should include tag
        val record = telemetry.exporter.logBuffer.single()
        val tagAttr = record.attributes.find { it.key == "log.tag" }
        assertNotNull(tagAttr, "Tagged log should have log.tag attribute")
        assertEquals("MyComponent", tagAttr.value.stringValue)
    }

    @Test
    fun nestedTagsConcatenate() {
        val recording = RecordingLog()
        logInterceptor = recording
        TelemetryLog(telemetry).install()

        Log.tag("Parent").tag("Child").warn("nested")

        val record = telemetry.exporter.logBuffer.single()
        val tagAttr = record.attributes.find { it.key == "log.tag" }
        assertNotNull(tagAttr)
        assertEquals("Parent/Child", tagAttr.value.stringValue)
    }

    @Test
    fun untaggedLogHasNoTagAttribute() {
        val recording = RecordingLog()
        logInterceptor = recording
        TelemetryLog(telemetry).install()

        Log.error("no tag")

        val record = telemetry.exporter.logBuffer.single()
        val tagAttr = record.attributes.find { it.key == "log.tag" }
        assertNull(tagAttr, "Untagged log should not have log.tag attribute")
    }
}
