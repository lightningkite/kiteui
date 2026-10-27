package com.lightningkite.kiteui.telemetry

import com.lightningkite.services.telemetry.TelemetryKeys

import com.lightningkite.kiteui.HttpMethod
import com.lightningkite.kiteui.HttpHeaders
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Regression tests for the userinfo-stripping fix in [Telemetry.instrumentFetch]: URLs with embedded
 * credentials (`https://user:pass@host/path`) must never have those credentials reach any recorded
 * telemetry attribute, while ordinary URLs must be recorded unchanged (over-redaction is also a bug).
 */
class TelemetryUrlRedactionTest {

    /** Runs instrumentFetch with a proceed that always fails, so the fetch span is still recorded. */
    private suspend fun fetch(t: Telemetry, url: String) {
        try {
            t.instrumentFetch(url, HttpMethod.GET, HttpHeaders(), null) { _, _, _, _ ->
                throw RuntimeException("mock failure")
            }
        } catch (_: RuntimeException) {
            // expected - the span is recorded on the failure path too
        }
    }

    @Test
    fun credentialsStrippedFromSpanHostAttribute() = runTest {
        val t = testTelemetry()
        fetch(t, "https://user:s3cr3t@example.com/api")

        val span = t.recordedSpans().single()
        val host = span.attributes[TelemetryKeys.Server.address]
        assertEquals("example.com", host, "host attribute should be stripped of credentials")

        val allValues = span.attributes.map.values.map { it.toString() }
        assertTrue(
            allValues.none { it.contains("s3cr3t") || it.contains("user:") },
            "credentials leaked into a span attribute: $allValues"
        )
    }

    @Test
    fun credentialsStrippedFromUrlPathAttributeToo() = runTest {
        val t = testTelemetry()
        fetch(t, "https://admin:hunter2@example.com/api/items?token=ignored")

        val urlPath = t.recordedSpans().single().attributes[TelemetryKeys.Url.path]
        assertFalse(urlPath!!.contains("hunter2"), "credentials leaked into url.path attribute: $urlPath")
    }

    @Test
    fun normalUrlWithoutCredentialsIsNotMangled() = runTest {
        val t = testTelemetry()
        fetch(t, "https://example.com/api/items")

        val span = t.recordedSpans().single()
        val host = span.attributes[TelemetryKeys.Server.address]
        assertEquals("example.com", host, "a plain host with no '@' must not be altered by the strip")
    }

    @Test
    fun normalUrlWithAtSignInPathIsNotMangled() = runTest {
        val t = testTelemetry()
        // '@' can legitimately appear in a path segment; only userinfo before the host should be stripped.
        fetch(t, "https://example.com/users/name@example.com/profile")

        val span = t.recordedSpans().single()
        val host = span.attributes[TelemetryKeys.Server.address]
        assertEquals("example.com", host, "host should be unaffected when '@' appears only in the path")
    }
}
