package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.HttpHeaders
import com.lightningkite.kiteui.HttpMethod
import com.lightningkite.kiteui.httpHeaders
import com.lightningkite.services.telemetry.TelemetryKeys
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.*

class InstrumentFetchTest {
    private class ProceedCapture(val headers: HttpHeaders) : Exception("mock proceed")

    /** Calls instrumentFetch, captures the headers received by proceed, then throws. */
    private suspend fun captureHeaders(t: Telemetry, url: String = "http://example.com/api"): HttpHeaders {
        try {
            t.instrumentFetch(url, HttpMethod.GET, httpHeaders(), null) { _, _, h, _ -> throw ProceedCapture(h) }
        } catch (e: ProceedCapture) {
            return e.headers
        }
        error("unreachable")
    }

    @Test
    fun traceparentIsTheRequestSpan() = runTest {
        val t = testTelemetry()
        val tp = captureHeaders(t).get("traceparent")!!

        val span = t.recordedSpans().single()
        assertEquals("00-${span.traceId}-${span.spanId}-01", tp)
        assertEquals("http", span.owner)
        assertEquals("GET", span.operation)
    }

    @Test
    fun traceparentNotInjectedByDefault() = runTest {
        val t = testTelemetry(tracePropagationHosts = emptyList())
        assertFalse(captureHeaders(t).has("traceparent"))
    }

    @Test
    fun traceparentNotInjectedForUnlistedHost() = runTest {
        val t = testTelemetry(tracePropagationHosts = listOf("other.com"))
        assertFalse(captureHeaders(t).has("traceparent"))
    }

    @Test
    fun propagationMatchesHostsAndSubdomainsIgnoringPort() = runTest {
        val t = testTelemetry(tracePropagationHosts = listOf("example.com"))
        assertTrue(captureHeaders(t, "http://example.com:8080/api").has("traceparent"))
        assertTrue(captureHeaders(t, "https://api.example.com/api").has("traceparent"))
        assertFalse(captureHeaders(t, "https://evilexample.com/api").has("traceparent"))
    }

    @Test
    fun urlPathIsOnlyThePath() = runTest {
        val t = testTelemetry()
        captureHeaders(t, "https://example.com:8443/api/items?token=secret#frag")
        val span = t.recordedSpans().single()
        assertEquals("/api/items", span.attributes[TelemetryKeys.Url.path])
        assertEquals("example.com", span.attributes[TelemetryKeys.Server.address])
    }

    @Test
    fun traceparentHasUnsampledFlagWhenNotSampled() = runTest {
        val t = testTelemetry(traceSamplingRate = 0.0)
        assertTrue(captureHeaders(t).get("traceparent")!!.endsWith("-00"))
    }

    @Test
    fun callerHeadersNotMutated() = runTest {
        val t = testTelemetry()
        val callerHeaders = httpHeaders()
        try {
            t.instrumentFetch("http://example.com/api", HttpMethod.GET, callerHeaders, null) { _, _, _, _ -> throw ProceedCapture(httpHeaders()) }
        } catch (_: ProceedCapture) {}
        assertFalse(callerHeaders.has("traceparent"))
    }

    @Test
    fun requestInsideASpanIsItsChild() = runTest {
        val t = testTelemetry()
        val tp = t.span("checkout", "pay") { captureHeaders(t) }.get("traceparent")!!

        val parent = t.recordedSpans().single { it.owner == "checkout" }
        assertEquals(parent.traceId, tp.split("-")[1])
        assertEquals(parent.spanId, t.recordedSpans().single { it.owner == "http" }.parentSpanId)
    }

    @Test
    fun requestsOutsideAnySpanAreSeparateTraces() = runTest {
        val t = testTelemetry()
        val first = captureHeaders(t).get("traceparent")!!.split("-")[1]
        val second = captureHeaders(t).get("traceparent")!!.split("-")[1]
        assertNotEquals(first, second)
    }

    @Test
    fun failedRequestRecordsAnErrorSpan() = runTest {
        val t = testTelemetry()
        captureHeaders(t)
        val span = t.recordedSpans().single()
        assertEquals("ProceedCapture", span.error?.type)
        assertEquals("example.com", span.attributes[TelemetryKeys.Server.address])
    }

    @Test
    fun telemetrysOwnUploadIsNotRecorded() = runTest {
        val t = testTelemetry()
        withContext(TelemetryUploading) { captureHeaders(t) }
        assertTrue(t.recordedSpans().isEmpty())
    }

    @Test
    fun proceedReceivesSameUrlAndMethod() = runTest {
        val t = testTelemetry()
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
