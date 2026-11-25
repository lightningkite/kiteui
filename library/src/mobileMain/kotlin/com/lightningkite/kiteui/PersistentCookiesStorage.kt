package com.lightningkite.kiteui

import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.fillDefaults
import io.ktor.client.plugins.cookies.matches
import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.http.parseServerSetCookieHeader
import io.ktor.http.renderSetCookieHeader
import io.ktor.util.date.GMTDate
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/** The cookie jar for the native HTTP clients, which, like a browser's, keeps cookies through restarts. */
internal val nativeCookies: PersistentCookiesStorage = PersistentCookiesStorage(
    read = { SecurePlatformStorage.get(nativeCookiesKey) },
    // Usually a rotated refresh token: losing the write leaves only a revoked one.
    write = { SecurePlatformStorage.set(nativeCookiesKey, it, ensureOnDisk = true) },
)
internal const val nativeCookiesKey: String = "KiteUI.cookies"

/**
 * Ktor's [io.ktor.client.plugins.cookies.AcceptAllCookiesStorage], but written out through [write] on every change
 * and read back through [read] on first use.
 *
 * Cookies without an expiry are not written, so like a browser's session cookies they last until the app exits.
 */
internal class PersistentCookiesStorage(
    private val read: () -> String?,
    private val write: (String) -> Unit,
    private val clock: () -> Long = { getTimeMillis() },
) : CookiesStorage {
    private val mutex = Mutex()
    private var loaded: MutableList<Cookie>? = null

    // Stored as Set-Cookie headers, Ktor's own serialized form of a cookie (see its CookieJvmSerializer).
    private fun cookies(): MutableList<Cookie> = loaded ?: read()
        .let { stored -> if (stored == null) emptyList() else Json.decodeFromString<List<String>>(stored) }
        .map { parseServerSetCookieHeader(it) }
        .toMutableList()
        .also { loaded = it }

    override suspend fun get(requestUrl: Url): List<Cookie> = mutex.withLock {
        val now = clock()
        cookies().filter { !it.isExpired(now) && it.matches(requestUrl) }
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        if (cookie.name.isBlank()) return
        mutex.withLock {
            val now = clock()
            val cookies = cookies()
            cookies.removeAll { it.name == cookie.name && it.matches(requestUrl) }
            // Max-Age counts from now, so it is turned into a date that still means the same thing after a restart.
            val filled = cookie.fillDefaults(requestUrl)
            val maxAge = filled.maxAge
            cookies.add(if (maxAge == null) filled else filled.copy(maxAge = null, expires = GMTDate(now + maxAge * 1000L)))
            cookies.removeAll { it.isExpired(now) }
            write(Json.encodeToString(cookies.filter { it.expires != null }.map { renderSetCookieHeader(it) }))
        }
    }

    override fun close() {}

    private fun Cookie.isExpired(now: Long): Boolean = expires.let { it != null && it.timestamp <= now }
}
