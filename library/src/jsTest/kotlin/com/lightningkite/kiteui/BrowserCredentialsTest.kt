package com.lightningkite.kiteui

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class BrowserCredentialsTest {
    // The Karma page is on localhost, so the server must be addressed as localhost too: a different port is a
    // different origin but the same site, which is the case this option exists for. As 127.0.0.1 it would be
    // cross-site, and the browser would withhold the cookie for SameSite reasons regardless of credentials.
    private val sameSiteServer = TestServer.http.replace("127.0.0.1", "localhost")

    @Test
    fun cookiesTravelOnlyWhenRequested() = networkTest {
        fetch("$sameSiteServer/cookie/set", webIncludeCredentials = true)

        val credentialed = fetch("$sameSiteServer/headers", webIncludeCredentials = true).text().lines()
        assertContains(credentialed, "cookie: ${TestServer.testCookie}")

        val plain = fetch("$sameSiteServer/headers").text().lines()
        assertFalse(plain.any { it.startsWith("cookie:") }, "a request that did not opt in must not carry cookies")
    }
}
