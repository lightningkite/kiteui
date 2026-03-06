// by Claude - tests for TelemetryContext coroutine context element and fallback behavior
package com.lightningkite.kiteui.telemetry

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.test.*

class TelemetryContextTest {

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
    fun fallsBackToGlobalTraceId() = runTest {
        // No TelemetryContext in coroutine context — should fall back to Telemetry.currentTraceId
        val result = coroutineContext.traceId()
        assertEquals(Telemetry.currentTraceId, result)
    }

    @Test
    fun fallsBackToGlobalSpanId() = runTest {
        val savedSpanId = Telemetry.currentSpanId
        Telemetry.currentSpanId = "fallback12345678"
        try {
            val result = coroutineContext.spanId()
            assertEquals("fallback12345678", result)
        } finally {
            Telemetry.currentSpanId = savedSpanId
        }
    }

    @Test
    fun contextElementOverridesGlobal() = runTest {
        val savedSpanId = Telemetry.currentSpanId
        Telemetry.currentSpanId = "global_span_0000"
        try {
            val ctx = TelemetryContext(traceId = "context_trace_override00000000", spanId = "context_span_ovr")
            withContext(ctx) {
                assertEquals("context_trace_override00000000", coroutineContext.traceId())
                assertEquals("context_span_ovr", coroutineContext.spanId())
            }
        } finally {
            Telemetry.currentSpanId = savedSpanId
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
