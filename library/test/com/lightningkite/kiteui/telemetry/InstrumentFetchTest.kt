package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.HttpMethod
import com.lightningkite.kiteui.httpHeaders
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.*

class InstrumentFetchTest {

    private fun testConfig(
        traceSamplingRate: Double = 1.0,
        tracePropagationHosts: List<String> = listOf("example.com"),
    ) = TelemetryConfig(
        endpoint = "http://localhost:0/otlp",
        traceSamplingRate = traceSamplingRate,
        tracePropagationHosts = tracePropagationHosts,
    )

    private class ProceedCapture(val headers: com.lightningkite.kiteui.HttpHeaders) : Exception("mock proceed")

    /** Calls instrumentFetch, captures the headers received by proceed, then throws. */
    private suspend fun captureHeaders(
        t: Telemetry,
        url: String = "http://example.com/api",
        method: HttpMethod = HttpMethod.GET,
    ): com.lightningkite.kiteui.HttpHeaders {
        try {
            t.instrumentFetch(url, method, httpHeaders(), null) { _, _, h, _ ->
                throw ProceedCapture(h)
            }
        } catch (e: ProceedCapture) {
            return e.headers
        }
        error("unreachable")
    }

    // --- Traceparent injection ---

    @Test
    fun instrumentFetchInjectsTraceparentHeader() = runTest {
        val t = Telemetry(testConfig(traceSamplingRate = 1.0))
        val captured = captureHeaders(t)

        assertTrue(captured.has("traceparent"), "traceparent header should be set")
        val tp = captured.get("traceparent")!!
        assertTrue(tp.startsWith("00-"), "Should start with version 00: $tp")
    }

    @Test
    fun traceparentNotInjectedByDefault() = runTest {
        val t = Telemetry(testConfig(tracePropagationHosts = emptyList()))
        val captured = captureHeaders(t)

        assertFalse(captured.has("traceparent"), "traceparent should NOT be set when propagation hosts is empty")
    }

    @Test
    fun traceparentNotInjectedForUnlistedHost() = runTest {
        val t = Telemetry(testConfig(tracePropagationHosts = listOf("other.com")))
        val captured = captureHeaders(t)

        assertFalse(captured.has("traceparent"), "traceparent should NOT be set for unlisted host")
    }

    @Test
    fun callerHeadersNotMutated() = runTest {
        val t = Telemetry(testConfig())
        val callerHeaders = httpHeaders()
        try {
            t.instrumentFetch("http://example.com/api", HttpMethod.GET, callerHeaders, null) { _, _, _, _ ->
                throw ProceedCapture(httpHeaders())
            }
        } catch (_: ProceedCapture) {}

        assertFalse(callerHeaders.has("traceparent"), "caller's headers should not be mutated")
    }

    @Test
    fun traceparentHasSampledFlagWhenSampled() = runTest {
        val t = Telemetry(testConfig(traceSamplingRate = 1.0))
        val captured = captureHeaders(t)

        val tp = captured.get("traceparent")!!
        assertTrue(tp.endsWith("-01"), "Sampled session should end with -01: $tp")
    }

    @Test
    fun traceparentHasUnsampledFlagWhenNotSampled() = runTest {
        val t = Telemetry(testConfig(traceSamplingRate = 0.0))
        val captured = captureHeaders(t)

        val tp = captured.get("traceparent")!!
        assertTrue(tp.endsWith("-00"), "Unsampled session should end with -00: $tp")
    }

    @Test
    fun traceparentUsesW3CFormat() = runTest {
        val t = Telemetry(testConfig())
        val captured = captureHeaders(t)

        val tp = captured.get("traceparent")!!
        val parts = tp.split("-")
        assertEquals(4, parts.size, "traceparent should have 4 parts: $tp")
        assertEquals("00", parts[0], "Version should be 00")
        assertEquals(32, parts[1].length, "Trace ID should be 32 hex chars: ${parts[1]}")
        assertEquals(16, parts[2].length, "Span ID should be 16 hex chars: ${parts[2]}")
        assertTrue(parts[3] == "00" || parts[3] == "01", "Flags should be 00 or 01: ${parts[3]}")
        assertTrue(Regex("^[0-9a-f]+$").matches(parts[1]), "Trace ID should be hex: ${parts[1]}")
        assertTrue(Regex("^[0-9a-f]+$").matches(parts[2]), "Span ID should be hex: ${parts[2]}")
    }

    // --- Coroutine context propagation ---

    @Test
    fun traceparentUsesTraceIdFromCoroutineContext() = runTest {
        val t = Telemetry(testConfig())
        val contextTraceId = "aaaa1111bbbb2222cccc3333dddd4444"
        val ctx = TelemetryContext(traceId = contextTraceId, spanId = "1234567890abcdef")

        val captured: com.lightningkite.kiteui.HttpHeaders
        withContext(ctx) {
            captured = captureHeaders(t)
        }

        val tp = captured.get("traceparent")!!
        val parts = tp.split("-")
        assertEquals(contextTraceId, parts[1], "Should use trace ID from coroutine context")
    }

    @Test
    fun traceparentUsesSpanIdFromCoroutineContextAsParent() = runTest {
        val t = Telemetry(testConfig())
        val contextSpanId = "1234567890abcdef"
        val ctx = TelemetryContext(traceId = "aaaa1111bbbb2222cccc3333dddd4444", spanId = contextSpanId)

        val captured: com.lightningkite.kiteui.HttpHeaders
        withContext(ctx) {
            captured = captureHeaders(t)
        }

        val tp = captured.get("traceparent")!!
        val fetchSpanId = tp.split("-")[2]
        assertNotEquals(contextSpanId, fetchSpanId,
            "Fetch span ID should be a new ID, not the parent span ID")
    }

    @Test
    fun traceparentFallsBackToTelemetryTraceId() = runTest {
        val t = Telemetry(testConfig())
        val captured = captureHeaders(t)

        val tp = captured.get("traceparent")!!
        val traceId = tp.split("-")[1]
        assertEquals(t.currentTraceId, traceId,
            "Should fall back to Telemetry.currentTraceId when no coroutine context")
    }

    @Test
    fun eachFetchGetsUniqueSpanId() = runTest {
        val t = Telemetry(testConfig())
        val spanIds = mutableSetOf<String>()

        repeat(5) {
            val captured = captureHeaders(t)
            val spanId = captured.get("traceparent")!!.split("-")[2]
            spanIds.add(spanId)
        }

        assertEquals(5, spanIds.size, "Each fetch should get a unique span ID")
    }

    @Test
    fun proceedReceivesSameUrlAndMethod() = runTest {
        val t = Telemetry(testConfig())
        var capturedUrl = ""
        var capturedMethod = HttpMethod.GET

        try {
            t.instrumentFetch("http://example.com/api/items", HttpMethod.POST, httpHeaders(), null) { u, m, _, _ ->
                capturedUrl = u
                capturedMethod = m
                throw ProceedCapture(httpHeaders())
            }
        } catch (_: ProceedCapture) {}

        assertEquals("http://example.com/api/items", capturedUrl)
        assertEquals(HttpMethod.POST, capturedMethod)
    }
}
