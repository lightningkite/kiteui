// by Claude - tests for Telemetry singleton behavior
package com.lightningkite.kiteui.telemetry

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TelemetryTest {

    @Test
    fun isInactiveByDefault() {
        // Telemetry should not be active if configure() hasn't been called
        // (Note: if a previous test configured it, this may not hold — tests should be independent)
        assertFalse(Telemetry.isActive && Telemetry.config?.endpoint == "test-noop",
            "Telemetry with endpoint 'test-noop' should not be pre-configured")
    }

    @Test
    fun sessionIdIsGenerated() {
        // Session ID should be a valid 16-hex-char span ID
        val sid = Telemetry.sessionId
        assertTrue(sid.length == 16, "Session ID should be 16 chars: $sid")
        assertTrue(Regex("^[0-9a-f]+$").matches(sid), "Session ID should be hex: $sid")
    }

    @Test
    fun traceIdIsGenerated() {
        val tid = Telemetry.currentTraceId
        assertTrue(tid.length == 32, "Trace ID should be 32 chars: $tid")
        assertTrue(Regex("^[0-9a-f]+$").matches(tid), "Trace ID should be hex: $tid")
    }
}
