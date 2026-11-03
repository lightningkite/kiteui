package com.lightningkite.kiteui

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

/**
 * The decorators an app builds its networking out of, exercised over a real request rather than a
 * stub — the point of [HttpFetcher] is that these compose around whatever the platform does, so a
 * fake at the bottom would leave the interesting half untested.
 */
class HttpFetcherChainTest {

    @Test
    fun addedHeadersReachTheServer() = networkTest {
        val fetcher = HttpFetcher.platform.withHeaders { listOf("X-Kiteui-Auth" to "token-1") }
        val body = fetcher.fetch("${TestServer.http}/headers").text()
        assertContains(body.lines(), "x-kiteui-auth: token-1")
    }

    /**
     * The reason [HttpFetcher.withHeaders] takes a function: a token can expire while a caller holds
     * the fetcher, so every request has to ask again rather than replay what was computed once.
     */
    @Test
    fun headersAreRecomputedPerRequest() = networkTest {
        var issued = 0
        val fetcher = HttpFetcher.platform.withHeaders { listOf("X-Kiteui-Auth" to "token-${++issued}") }
        assertContains(fetcher.fetch("${TestServer.http}/headers").text().lines(), "x-kiteui-auth: token-1")
        assertContains(fetcher.fetch("${TestServer.http}/headers").text().lines(), "x-kiteui-auth: token-2")
    }

    /** A header the caller supplied is the one an added header of the same name replaces. */
    @Test
    fun addedHeadersOverrideTheCallers() = networkTest {
        val fetcher = HttpFetcher.platform.withHeaders { listOf("X-Kiteui-Auth" to "from-chain") }
        val body = fetcher.fetch(
            url = "${TestServer.http}/headers",
            headers = httpHeaders("X-Kiteui-Auth" to "from-caller"),
        ).text()
        assertContains(body.lines(), "x-kiteui-auth: from-chain")
    }

    /** Interceptors wrap in the order given, and each one really does see the request go by. */
    @Test
    fun interceptorsRunOutermostFirst() = networkTest {
        val entered = mutableListOf<String>()
        val fetcher = HttpFetcher.platform.intercepted(
            { url, method, headers, body, proceed -> entered += "outer"; proceed(url, method, headers, body) },
            { url, method, headers, body, proceed -> entered += "inner"; proceed(url, method, headers, body) },
        )
        assertEquals(TestServer.helloBody, fetcher.fetch("${TestServer.http}/hello").text())
        assertEquals(listOf("outer", "inner"), entered)
    }

    /** An interceptor may rewrite the request; the server is what proves the rewrite took effect. */
    @Test
    fun interceptorsCanRewriteTheRequest() = networkTest {
        val fetcher = HttpFetcher.platform.intercepted({ _, method, headers, body, proceed ->
            proceed("${TestServer.http}/hello", method, headers, body)
        })
        assertEquals(TestServer.helloBody, fetcher.fetch("${TestServer.http}/status/500").text())
    }
}
