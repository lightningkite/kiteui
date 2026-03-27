package com.lightningkite.kiteui.telemetry

import kotlin.test.Test
import kotlin.test.assertTrue

class TelemetryTest {

    private fun testConfig() = TelemetryConfig(endpoint = "http://localhost:0/otlp")

    @Test
    fun sessionIdIsGenerated() {
        val telemetry = Telemetry(testConfig())
        val sid = telemetry.sessionId
        assertTrue(sid.length == 16, "Session ID should be 16 chars: $sid")
        assertTrue(Regex("^[0-9a-f]+$").matches(sid), "Session ID should be hex: $sid")
    }

    @Test
    fun traceIdIsGenerated() {
        val telemetry = Telemetry(testConfig())
        val tid = telemetry.currentTraceId
        assertTrue(tid.length == 32, "Trace ID should be 32 chars: $tid")
        assertTrue(Regex("^[0-9a-f]+$").matches(tid), "Trace ID should be hex: $tid")
    }
}
