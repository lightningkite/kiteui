package com.lightningkite.kiteui

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/**
 * The one failure a browser reports differently from every other platform.
 *
 * A browser refuses to say whether a request failed because the network is down or because the
 * server's CORS policy rejected the page's origin — both surface as a status of zero with no
 * detail — so the JS implementation works the difference out by probing afterwards. Getting it wrong
 * costs a user either a retry loop that can never succeed or a misreported outage, and no other
 * target can exercise the code that decides.
 *
 * The test server runs a second copy of itself without CORS headers for exactly this.
 */
class BrowserRequestBlockedTest {

    @Test
    fun aCorsRefusalIsNotRetryable() = networkTest {
        val thrown = assertFailsWith<FetchException> { fetch("${TestServer.httpWithoutCors}/hello") }
        assertIs<RequestBlockedException>(
            thrown,
            "a reachable server that refuses this origin will refuse it identically forever",
        )
    }
}
