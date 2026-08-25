package com.lightningkite.kiteui

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * What [platformFetch] must do identically on every platform, checked against a real server.
 *
 * Each target implements this over a different HTTP client, so agreement is a claim about four
 * separate pieces of code and cannot be established by testing one of them.
 */
class HttpFetchTest {

    @Test
    fun readsBodyAndStatus() = networkTest {
        val response = fetch("${TestServer.http}/hello")
        assertEquals(200, response.status.toInt())
        assertTrue(response.ok)
        assertEquals(TestServer.helloBody, response.text())
    }

    @Test
    fun sendsBodyAndMethod() = networkTest {
        val payload = "round trip é ✓"
        val response = fetch(
            url = "${TestServer.http}/echo",
            method = HttpMethod.PUT,
            body = RequestBodyText(payload, "text/plain; charset=utf-8"),
        )
        assertEquals(payload, response.text())
        assertEquals("PUT", response.headers.get(TestServer.echoMethodHeader))
    }

    @Test
    fun sendsRequestHeaders() = networkTest {
        val response = fetch(
            url = "${TestServer.http}/headers",
            headers = httpHeaders("X-Kiteui-Test" to "present"),
        )
        assertContains(response.text().lines(), "x-kiteui-test: present")
    }

    @Test
    fun roundTripsBinaryBodies() = networkTest {
        val sent = TestServer.deterministicBytes(4096)
        val response = fetch(
            url = "${TestServer.http}/echo",
            method = HttpMethod.POST,
            body = RequestBodyBlob(sent.toBlob("application/octet-stream")),
        )
        assertEquals(sent.toList(), response.blob().toByteArray().toList())
    }

    @Test
    fun downloadsKnownLengthBody() = networkTest {
        val response = fetch("${TestServer.http}/bytes/$downloadSize")
        val received = response.blob().toByteArray()
        assertEquals(downloadSize, received.size)
        assertEquals(TestServer.deterministicBytes(downloadSize).toList(), received.toList())
    }

    /**
     * Progress reporting is the part of [fetch] an app shows to a user, and each platform wires it to
     * a different callback. Only the final figures are asserted: how many intermediate events arrive
     * depends on buffering, but the last one has to describe a finished transfer.
     */
    @Test
    fun reportsDownloadProgress() = networkTest {
        var lastComplete = -1L
        var lastExpected = -1L
        val response = fetch(
            url = "${TestServer.http}/bytes/$downloadSize",
            onDownloadProgress = { complete, expected ->
                lastComplete = complete
                lastExpected = expected
            },
        )
        response.blob()
        assertEquals(downloadSize.toLong(), lastComplete, "no completed download was reported")
        assertEquals(downloadSize.toLong(), lastExpected)
    }

    /**
     * A status the server chose is an answer, not a failure: the caller decides what a 404 means, so
     * [fetch] must hand it back rather than throw.
     */
    @Test
    fun reportsErrorStatusWithoutThrowing() = networkTest {
        val response = fetch("${TestServer.http}/status/404")
        assertEquals(404, response.status.toInt())
        assertTrue(!response.ok)
        assertEquals("status 404", response.text())
    }

    /**
     * Nothing is listening, so every platform must reach the same verdict: retryable, and not the
     * [RequestBlockedException] that means "stop asking". A browser is the interesting case — it
     * refuses to say why a request failed, and the JS implementation has to work it out by probing.
     */
    @Test
    fun refusedConnectionIsRetryable() = networkTest {
        val thrown = assertFailsWith<FetchException> { fetch(TestServer.refused) }
        assertIs<ConnectionException>(thrown, "a refused connection should be worth retrying")
    }

    /**
     * The two status codes [ConnectivityGate] schedules differently, read from a real response.
     * Reaching `Retry-After` at all is the substance of this one in a browser, where a header is
     * unreadable unless the server has listed it in `Access-Control-Expose-Headers`.
     */
    @Test
    fun classifiesRateLimitedResponses() = networkTest {
        val response = fetch("${TestServer.http}/status/429")
        val failure = response.connectivityFailure()
        assertIs<RateLimitedException>(failure)
        assertEquals(TestServer.retryAfterSeconds.seconds, failure.retryAfter)
    }

    @Test
    fun classifiesUnavailableResponses() = networkTest {
        val response = fetch("${TestServer.http}/status/503")
        assertIs<ServerUnavailableException>(response.connectivityFailure())
    }

    /** A request that succeeds must pass through the gate untouched and leave it open. */
    @Test
    fun gateForwardsSuccessfulRequests() = networkTest {
        val gate = ConnectivityGate()
        val response = gate.runRequest("GET /hello") { fetch("${TestServer.http}/hello") }
        assertEquals(TestServer.helloBody, response.text())
        assertTrue(gate.gate.permit, "a successful request should leave the gate open")
    }

    /** Large enough that a client has to deliver the body in pieces, small enough to stay quick. */
    private val downloadSize = 512 * 1024
}
