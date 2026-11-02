package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.HttpFetcher
import com.lightningkite.kiteui.HttpHeaders
import com.lightningkite.kiteui.HttpMethod
import com.lightningkite.kiteui.TestServer
import com.lightningkite.kiteui.networkTest
import com.lightningkite.services.telemetry.RecordedSpan
import com.lightningkite.services.telemetry.TelemetryKeys
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.*

class InstrumentFetchTest {
    private class ProceedCapture(val headers: HttpHeaders) : Exception("mock proceed")

    /** Calls instrumentFetch, captures the headers received by proceed, then throws. */
    private suspend fun captureHeaders(t: Telemetry, url: String = "http://example.com/api"): HttpHeaders {
        try {
            t.instrumentFetch(url, HttpMethod.GET, HttpHeaders(), null) { _, _, h, _ -> throw ProceedCapture(h) }
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
    fun idsEmailsAndTokensInThePathAreReplaced() = runTest {
        val t = testTelemetry()
        captureHeaders(t, "https://example.com/v1/users/42/name@example.com/550e8400-e29b-41d4-a716-446655440000/verify/eyJhbGciOiJIUzI1NiJ9abc/a%40b.com/my-article/release-notes-for-2024")
        assertEquals("/v1/users/{id}/{id}/{id}/verify/{id}/{id}/my-article/release-notes-for-2024", t.recordedSpans().single().attributes[TelemetryKeys.Url.path])
    }

    @Test
    fun hostsMatchIgnoringCase() = runTest {
        val t = testTelemetry(tracePropagationHosts = listOf("Example.com"))
        assertTrue(captureHeaders(t, "https://API.EXAMPLE.COM/api").has("traceparent"))
        assertEquals("api.example.com", t.recordedSpans().single().attributes[TelemetryKeys.Server.address])
    }

    @Test
    fun ipv6HostKeepsItsBrackets() = runTest {
        val t = testTelemetry()
        captureHeaders(t, "http://[::1]:8080/api")
        assertEquals("[::1]", t.recordedSpans().single().attributes[TelemetryKeys.Server.address])
    }

    @Test
    fun relativeUrlsGoToTheOwnOriginAndArePropagatedTo() = runTest {
        val t = testTelemetry(tracePropagationHosts = listOf("example.com"))
        assertTrue(captureHeaders(t, "/api/items?token=secret").has("traceparent"))
        val span = t.recordedSpans().single()
        assertEquals("/api/items", span.attributes[TelemetryKeys.Url.path])
        assertNull(span.attributes[TelemetryKeys.Server.address])
        assertFalse(captureHeaders(testTelemetry(tracePropagationHosts = emptyList()), "/api/items").has("traceparent"))
    }

    @Test
    fun traceparentHasUnsampledFlagWhenNotSampled() = runTest {
        val t = testTelemetry(traceSamplingRate = 0.0)
        assertTrue(captureHeaders(t).get("traceparent")!!.endsWith("-00"))
    }

    @Test
    fun callerHeadersNotMutated() = runTest {
        val t = testTelemetry()
        val callerHeaders = HttpHeaders()
        try {
            t.instrumentFetch("http://example.com/api", HttpMethod.GET, callerHeaders, null) { _, _, _, _ -> throw ProceedCapture(HttpHeaders()) }
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
    fun errorStatusIsAnErrorSpan() = networkTest {
        val t = testTelemetry()
        for (path in listOf("hello", "status/500")) {
            t.instrumentFetch("${TestServer.http}/$path", HttpMethod.GET, HttpHeaders(), null) { u, m, h, b -> HttpFetcher.Platform.fetch(u, m, h, b) }
        }
        val (ok, failed) = t.recordedSpans()
        assertEquals(RecordedSpan.Outcome.Ok, ok.outcome)
        assertEquals(RecordedSpan.Outcome.Error, failed.outcome)
        assertEquals(500L, failed.attributes[TelemetryKeys.Http.responseStatusCode])
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
            t.instrumentFetch("http://example.com/api/items", HttpMethod.POST, HttpHeaders(), null) { u, m, _, _ ->
                capturedUrl = u
                capturedMethod = m
                throw ProceedCapture(HttpHeaders())
            }
        } catch (_: ProceedCapture) {}

        assertEquals("http://example.com/api/items", capturedUrl)
        assertEquals(HttpMethod.POST, capturedMethod)
    }
}
