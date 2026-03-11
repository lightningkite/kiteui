package com.lightningkite.kiteui.telemetry

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.test.*

class TelemetryContextTest {

    private fun testConfig() = TelemetryConfig(endpoint = "http://localhost:0/otlp")

    @AfterTest
    fun cleanup() {
        activeTelemetry = null
    }

    @Test
    fun traceIdFromContextElement() = runTest {
        val ctx = TelemetryContext(traceId = "aaaa1111bbbb2222cccc3333dddd4444", spanId = "1234567890abcdef")
        withContext(ctx) {
            assertEquals("aaaa1111bbbb2222cccc3333dddd4444", coroutineContext.traceId())
        }
    }

    @Test
    fun spanIdFromContextElement() = runTest {
        val ctx = TelemetryContext(traceId = "aaaa1111bbbb2222cccc3333dddd4444", spanId = "1234567890abcdef")
        withContext(ctx) {
            assertEquals("1234567890abcdef", coroutineContext.spanId())
        }
    }

    @Test
    fun fallsBackToActiveTelemetryTraceId() = runTest {
        val t = Telemetry(testConfig())
        activeTelemetry = t
        val result = coroutineContext.traceId()
        assertEquals(t.currentTraceId, result)
    }

    @Test
    fun fallsBackToActiveTelemetrySpanId() = runTest {
        val t = Telemetry(testConfig())
        t.currentSpanId = "fallback12345678"
        activeTelemetry = t
        val result = coroutineContext.spanId()
        assertEquals("fallback12345678", result)
    }

    @Test
    fun fallsBackToEmptyWhenNoTelemetry() = runTest {
        activeTelemetry = null
        assertEquals("", coroutineContext.traceId())
        assertEquals("", coroutineContext.spanId())
    }

    @Test
    fun contextElementOverridesGlobal() = runTest {
        val t = Telemetry(testConfig())
        t.currentSpanId = "global_span_0000"
        activeTelemetry = t
        val ctx = TelemetryContext(traceId = "context_trace_override00000000", spanId = "context_span_ovr")
        withContext(ctx) {
            assertEquals("context_trace_override00000000", coroutineContext.traceId())
            assertEquals("context_span_ovr", coroutineContext.spanId())
        }
    }

    @Test
    fun contextElementInstallsAndRetrievesCorrectly() = runTest {
        val ctx = TelemetryContext(traceId = "aabb", spanId = "ccdd")
        withContext(ctx) {
            val retrieved = coroutineContext[TelemetryContext]
            assertNotNull(retrieved)
            assertEquals("aabb", retrieved.traceId)
            assertEquals("ccdd", retrieved.spanId)
        }
    }

    @Test
    fun emptyContextHasNoElement() {
        val element = EmptyCoroutineContext[TelemetryContext]
        assertNull(element)
    }

    // --- TelemetryContext.current() factory ---

    @Test
    fun currentCapturesFromExistingContext() = runTest {
        val parent = TelemetryContext(traceId = "aabb1122ccdd3344eeff5566aabb7788", spanId = "1122334455667788")
        withContext(parent) {
            val captured = TelemetryContext.current()
            assertEquals("aabb1122ccdd3344eeff5566aabb7788", captured.traceId)
            assertEquals("1122334455667788", captured.spanId)
        }
    }

    @Test
    fun currentFallsBackToActiveTelemetry() = runTest {
        val t = Telemetry(testConfig())
        activeTelemetry = t
        val captured = TelemetryContext.current()
        assertEquals(t.currentTraceId, captured.traceId)
        assertEquals(t.currentSpanId, captured.spanId)
    }

    @Test
    fun childCoroutineInheritsContext() = runTest {
        val ctx = TelemetryContext(traceId = "parent_trace_id_00000000000000", spanId = "parent_span_1234")
        withContext(ctx) {
            // Launch a child — it should inherit the TelemetryContext
            withContext(EmptyCoroutineContext) {
                // EmptyCoroutineContext is merged with parent, so TelemetryContext survives
                assertEquals("parent_trace_id_00000000000000", coroutineContext.traceId())
                assertEquals("parent_span_1234", coroutineContext.spanId())
            }
        }
    }
}
