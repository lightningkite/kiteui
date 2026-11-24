package com.lightningkite.kiteui

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Runs Ktor's own cookie plugin over [PersistentCookiesStorage], as the native clients do. */
class PersistentCookiesStorageTest {
    private var saved: String? = null
    private var now = 1_000_000_000_000L

    /** A fresh storage reading what earlier ones wrote, which is what an app restart amounts to. */
    private fun launchApp() = PersistentCookiesStorage(read = { saved }, write = { saved = it }, clock = { now })

    /** Requests [url] and returns the Cookie header it carried; [setCookie] is what the server answers with. */
    private fun PersistentCookiesStorage.request(url: String, setCookie: String? = null): String? = runBlocking {
        var sent: String? = null
        val client = HttpClient(MockEngine { request ->
            sent = request.headers[HttpHeaders.Cookie]
            respond("", headers = if (setCookie == null) headersOf() else headersOf(HttpHeaders.SetCookie, setCookie))
        }) {
            install(HttpCookies) { storage = this@request }
        }
        client.get(url)
        client.close()
        sent
    }

    private val refreshCookie = "refresh=abc; Path=/auth/refresh; Max-Age=3600; Secure; HttpOnly; SameSite=Strict"

    @Test
    fun aPathScopedCookieSurvivesARestartAndGoesOnlyWhereItBelongs() {
        launchApp().request("https://api.example.com/auth/login", setCookie = refreshCookie)

        val app = launchApp()
        assertEquals("refresh=abc", app.request("https://api.example.com/auth/refresh"))
        assertNull(app.request("https://api.example.com/users"), "outside the cookie's Path")
        assertNull(app.request("http://api.example.com/auth/refresh"), "a Secure cookie over plain http")
        assertNull(app.request("https://other.example.com/auth/refresh"), "another host")
    }

    @Test
    fun maxAgeStillCountsFromWhenTheCookieWasSetAfterARestart() {
        launchApp().request("https://api.example.com/auth/login", setCookie = refreshCookie)
        now += 3_599_000
        assertEquals("refresh=abc", launchApp().request("https://api.example.com/auth/refresh"))
        now += 2_000
        assertNull(launchApp().request("https://api.example.com/auth/refresh"))
    }

    @Test
    fun aServerCanDeleteACookie() {
        val app = launchApp()
        app.request("https://api.example.com/auth/login", setCookie = refreshCookie)
        app.request("https://api.example.com/auth/refresh", setCookie = "refresh=; Path=/auth/refresh; Max-Age=0")
        assertNull(app.request("https://api.example.com/auth/refresh"))
        assertNull(launchApp().request("https://api.example.com/auth/refresh"))
    }

    @Test
    fun aSessionCookieLastsOnlyUntilTheAppExits() {
        val app = launchApp()
        app.request("https://api.example.com/", setCookie = "session=1; Path=/")
        assertEquals("session=1", app.request("https://api.example.com/"))
        assertNull(launchApp().request("https://api.example.com/"))
    }
}
