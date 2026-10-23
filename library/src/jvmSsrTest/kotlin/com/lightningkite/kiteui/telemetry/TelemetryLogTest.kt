package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.Log
import com.lightningkite.kiteui.LogInterceptor
import com.lightningkite.services.telemetry.LogLevel
import com.lightningkite.services.telemetry.TelemetryKey
import com.lightningkite.services.telemetry.TelemetryKeys
import com.lightningkite.services.telemetry.telemetryAttributesOf
import kotlin.test.*

class TelemetryLogTest {
    private var savedInterceptors: List<LogInterceptor> = emptyList()
    private val telemetry = testTelemetry()
    private val logTag = TelemetryKey.OfString("log.tag")

    @BeforeTest
    fun saveState() {
        savedInterceptors = Log.interceptors.toList()
        Log.interceptors.clear()
        Log.interceptors.add(telemetry.logInterceptor)
    }

    @AfterTest
    fun restoreState() {
        Log.interceptors.clear()
        Log.interceptors.addAll(savedInterceptors)
    }

    @Test
    fun onlyLogsAtTheMinimumLevelAndAboveAreRecorded() {
        Log.log("debug msg")
        Log.info("info msg")
        Log.warn("warn msg")
        Log.error("error msg")

        val logs = telemetry.recordedLogs()
        assertEquals(listOf(LogLevel.Warn, LogLevel.Error), logs.map { it.level })
        assertEquals(listOf("warn msg", "error msg"), logs.map { it.message })
    }

    @Test
    fun logsCarryTheCurrentScreenAndNoTraceIds() {
        telemetry.currentScreen = telemetryAttributesOf(TelemetryKeys.App.screenName to "HomePage")
        Log.warn("something went wrong")

        val log = telemetry.recordedLogs().single()
        assertEquals("HomePage", log.attributes[TelemetryKeys.App.screenName])
        assertNull(log.traceId)
        assertNull(log.spanId)
    }

    @Test
    fun nestedTagsConcatenate() {
        Log.tag("Parent").tag("Child").warn("nested")
        assertEquals("Parent/Child", telemetry.recordedLogs().single().attributes[logTag])
    }

    @Test
    fun untaggedLogHasNoTagAttribute() {
        Log.error("no tag")
        assertNull(telemetry.recordedLogs().single().attributes[logTag])
    }
}
