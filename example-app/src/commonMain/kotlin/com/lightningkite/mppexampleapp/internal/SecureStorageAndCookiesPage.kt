package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class Check(val name: String, val outcome: Outcome, val detail: String)

enum class Outcome { Pass, Fail, NotYet }

/** Checks of SecurePlatformStorage, on the platforms that have it. Elsewhere, none. */
expect fun secureStorageChecks(): List<Check>

/**
 * Live checks of the native cookie jar (or, on the web, of `webIncludeCredentials`) and of SecurePlatformStorage,
 * against the `:test-server` echo server.
 */
@Routable("internal/secure-storage-and-cookies")
object SecureStorageAndCookiesPage : Page {
    override val title: Reactive<String> = Constant("Secure storage & cookies")

    private const val scopedCookie = "kiteui-scoped=present"

    // In plain storage: when this page last had the server set its cookie, so a later launch knows to expect it back.
    private const val cookieSetAtKey = "example.scopedCookieSetAt"

    // Read when the object is first touched, before this launch (page load, on the web) sets anything.
    private val cookieSetBeforeThisLaunch: Instant? = PlatformStorage.get(cookieSetAtKey)?.let { Instant.parse(it) }

    // Only the first request of a launch says anything about persistence; later ones see this launch's own cookie.
    private var cookieSentOnFirstRequest: Boolean? = null

    private suspend fun cookieChecks(server: String): List<Check> {
        val scoped = "$server/cookie/scoped"
        suspend fun carriesCookie(url: String, webIncludeCredentials: Boolean = true): Boolean =
            fetch(url, webIncludeCredentials = webIncludeCredentials).text().lines().any { it.startsWith("cookie:") && scopedCookie in it }

        val checks = mutableListOf<Check>()
        val sentOnFirstRequest = cookieSentOnFirstRequest ?: carriesCookie("$scoped/headers").also { cookieSentOnFirstRequest = it }
        val setAt = cookieSetBeforeThisLaunch
        checks += when {
            // The server's Max-Age is an hour.
            setAt == null || setAt < Clock.System.now() - 1.hours -> Check(
                "Cookie kept from an earlier launch", Outcome.NotYet,
                "No earlier launch set one in the last hour. Run the checks, quit the app completely (reload, on the web), and run them again."
            )
            sentOnFirstRequest -> Check("Cookie kept from an earlier launch", Outcome.Pass, "Set at $setAt by an earlier launch, and sent on this launch's first request.")
            else -> Check("Cookie kept from an earlier launch", Outcome.Fail, "Set at $setAt by an earlier launch, but this launch's first request went without it.")
        }

        fetch("$scoped/set", webIncludeCredentials = true)
        PlatformStorage.set(cookieSetAtKey, Clock.System.now().toString())
        checks += Check(
            "Cookie sent under its Path",
            if (carriesCookie("$scoped/headers")) Outcome.Pass else Outcome.Fail,
            "GET $scoped/headers after $scoped/set (Path=/cookie/scoped; Max-Age=3600; HttpOnly)"
        )
        checks += Check(
            "Cookie not sent outside its Path",
            if (carriesCookie("$server/headers")) Outcome.Fail else Outcome.Pass,
            "GET $server/headers"
        )
        if (Platform.current == Platform.Web) checks += Check(
            "No cookie without webIncludeCredentials",
            if (carriesCookie("$scoped/headers", webIncludeCredentials = false)) Outcome.Fail else Outcome.Pass,
            "A cross-origin request that does not opt in goes without cookies."
        )
        return checks
    }

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        // The host machine as each platform reaches it. The Android emulator aliases it as 10.0.2.2. iOS's App Transport
        // Security lets plain http through to an IP address but not to "localhost". A browser needs "localhost": the
        // page is served from there, and a cookie from 127.0.0.1 would be cross-site and withheld.
        val server = Signal(
            when (Platform.current) {
                Platform.Android -> "http://10.0.2.2:18787"
                Platform.iOS -> "http://127.0.0.1:18787"
                else -> "http://localhost:18787"
            }
        )
        val checks = Signal<List<Check>>(emptyList())

        scrolling.col {
            h1("Secure storage & cookies")
            text("Start the echo server on this machine with ./gradlew :test-server:run, then run the checks. To check what survives a restart, quit the app completely (on the web, reload the page) and run them again.")
            field("Test server") {
                textInput {
                    keyboardHints = KeyboardHints.id
                    content bind server
                }
            }
            row {
                important.button {
                    text("Run checks")
                    onClick {
                        val cookies = try {
                            cookieChecks(server.value)
                        } catch (e: FetchException) {
                            listOf(Check("Reach the test server", Outcome.Fail, "${server.value}: ${e.message}"))
                        }
                        checks.value = secureStorageChecks() + cookies
                    }
                }
                button {
                    text("Clear cookie")
                    onClick {
                        fetch("${server.value}/cookie/scoped/clear", webIncludeCredentials = true)
                        PlatformStorage.remove(cookieSetAtKey)
                        checks.value = emptyList()
                        context.toast("Cleared. The next launch has no cookie to find.")
                    }
                }
            }
            subtext("SecurePlatformStorage is checked on Android and iOS only. On the web, the cookie checks cover webIncludeCredentials instead.")
            colOf(checks) { check ->
                dynamicThemed {
                    when (check().outcome) {
                        Outcome.Pass -> AffirmativeSemantic
                        Outcome.Fail -> DangerSemantic
                        Outcome.NotYet -> CardSemantic
                    }
                }.col {
                    text { ::content { "${check().outcome.name.uppercase()}: ${check().name}" } }
                    subtext { ::content { check().detail } }
                }
            }
        }
    }
}
