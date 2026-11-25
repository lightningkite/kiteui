package com.lightningkite.kiteui

import androidx.test.platform.app.InstrumentationRegistry
import com.lightningkite.kiteui.views.AndroidAppContext
import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** The app's real client and cookie jar against the echo server the build starts on the host. */
class NativeCookiesDeviceTest {
    // The emulator's address for the host's loopback, where the echo server listens.
    private val server = "http://10.0.2.2:$TEST_SERVER_PORT"
    private val scoped = "$server/cookie/scoped"
    private val scopedCookie = "kiteui-scoped=present"

    @BeforeTest
    fun setUp() {
        AndroidAppContext.applicationCtx = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    }

    private suspend fun headersAt(url: String): List<String> = fetch(url).text().lines()

    /** A new jar over what this one wrote to storage, which is what the next launch starts from. */
    private suspend fun cookiesNextLaunchSendsTo(url: String): List<String> =
        PersistentCookiesStorage(read = { SecurePlatformStorage.get(nativeCookiesKey) }, write = {})
            .get(Url(url)).map { "${it.name}=${it.value}" }

    @Test
    fun aPathScopedCookieIsSentOnlyUnderItsPathAndOutlivesTheApp() = runBlocking {
        fetch("$scoped/clear")
        fetch("$scoped/set")

        assertContains(headersAt("$scoped/headers"), "cookie: $scopedCookie")
        assertFalse(headersAt("$server/headers").any { scopedCookie in it }, "outside the cookie's Path")
        assertEquals(listOf(scopedCookie), cookiesNextLaunchSendsTo("$scoped/headers"))
    }

    @Test
    fun aClearedCookieIsGoneNowAndAfterARestart() = runBlocking {
        fetch("$scoped/set")
        fetch("$scoped/clear")

        assertFalse(headersAt("$scoped/headers").any { scopedCookie in it })
        assertEquals(emptyList(), cookiesNextLaunchSendsTo("$scoped/headers"))
    }
}
