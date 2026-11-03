package com.lightningkite.kiteui.telemetry

import com.lightningkite.kiteui.HttpMethod
import com.lightningkite.kiteui.httpHeaders
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Regression tests for the userinfo-stripping fix in [Telemetry.instrumentFetch]: URLs with embedded
 * credentials (`https://user:pass@host/path`) must never have those credentials reach any recorded
 * telemetry attribute, while ordinary URLs must be recorded unchanged (over-redaction is also a bug).
 */
class TelemetryUrlRedactionTest {

    private fun testConfig(traceSamplingRate: Double = 1.0) = TelemetryConfig(
        endpoint = "http://localhost:0/otlp",
        traceSamplingRate = traceSamplingRate,
    )

    /** Runs instrumentFetch with a proceed that always fails, so the fetch span is still recorded. */
    private suspend fun fetch(t: Telemetry, url: String) {
        try {
            t.instrumentFetch(url, HttpMethod.GET, httpHeaders(), null) { _, _, _, _ ->
                throw RuntimeException("mock failure")
            }
        } catch (_: RuntimeException) {
            // expected - recordFetchTelemetry runs on the failure path too
        }
    }

    @Test
    fun credentialsStrippedFromSpanHostAttribute() = runTest {
        val t = Telemetry(testConfig())
        fetch(t, "https://user:s3cr3t@example.com/api")

        val span = t.exporter.spanBuffer.single()
        val host = span.attributes.single { it.key == "server.address" }.value.stringValue
        assertEquals("example.com", host, "host attribute should be stripped of credentials")

        val allValues = span.attributes.mapNotNull { it.value.stringValue }
        assertTrue(
            allValues.none { it.contains("s3cr3t") || it.contains("user:") },
            "credentials leaked into a span attribute: $allValues"
        )
    }

    @Test
    fun credentialsStrippedFromUrlPathAttributeToo() = runTest {
        val t = Telemetry(testConfig())
        fetch(t, "https://admin:hunter2@example.com/api/items?token=ignored")

        val span = t.exporter.spanBuffer.single()
        val urlPath = span.attributes.single { it.key == "url.path" }.value.stringValue
        assertFalse(urlPath!!.contains("hunter2"), "credentials leaked into url.path attribute: $urlPath")
    }

    @Test
    fun normalUrlWithoutCredentialsIsNotMangled() = runTest {
        val t = Telemetry(testConfig())
        fetch(t, "https://example.com/api/items")

        val span = t.exporter.spanBuffer.single()
        val host = span.attributes.single { it.key == "server.address" }.value.stringValue
        assertEquals("example.com", host, "a plain host with no '@' must not be altered by the strip")
    }

    @Test
    fun normalUrlWithAtSignInPathIsNotMangled() = runTest {
        val t = Telemetry(testConfig())
        // '@' can legitimately appear in a path segment; only userinfo before the host should be stripped.
        fetch(t, "https://example.com/users/name@example.com/profile")

        val span = t.exporter.spanBuffer.single()
        val host = span.attributes.single { it.key == "server.address" }.value.stringValue
        assertEquals("example.com", host, "host should be unaffected when '@' appears only in the path")
    }
}
