package com.lightningkite.kiteui

import io.ktor.http.Url
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** The app's real client and cookie jar, kept in the Keychain, against the echo server. */
class NativeCookiesTest {
    private val scoped = "${TestServer.http}${TestServer.scopedCookiePath}"

    private suspend fun headersAt(url: String): List<String> = fetch(url).text().lines()

    /** A new jar over what this one wrote to storage, which is what the next launch starts from. */
    private suspend fun cookiesNextLaunchSendsTo(url: String): List<String> =
        PersistentCookiesStorage(read = { SecurePlatformStorage.get(nativeCookiesKey) }, write = {})
            .get(Url(url)).map { "${it.name}=${it.value}" }

    @Test
    fun aPathScopedCookieIsSentOnlyUnderItsPathAndOutlivesTheApp() = networkTest {
        fetch("$scoped/clear")
        fetch("$scoped/set")

        assertContains(headersAt("$scoped/headers"), "cookie: ${TestServer.scopedCookie}")
        assertFalse(headersAt("${TestServer.http}/headers").any { TestServer.scopedCookie in it }, "outside the cookie's Path")
        assertEquals(listOf(TestServer.scopedCookie), cookiesNextLaunchSendsTo("$scoped/headers"))
    }

    @Test
    fun aClearedCookieIsGoneNowAndAfterARestart() = networkTest {
        fetch("$scoped/set")
        fetch("$scoped/clear")

        assertFalse(headersAt("$scoped/headers").any { TestServer.scopedCookie in it })
        assertEquals(emptyList(), cookiesNextLaunchSendsTo("$scoped/headers"))
    }
}
