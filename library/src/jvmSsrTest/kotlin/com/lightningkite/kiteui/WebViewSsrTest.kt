@file:OptIn(ExperimentalCoroutinesApi::class)

package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.Frame
import com.lightningkite.kiteui.views.direct.WebViewPermission
import com.lightningkite.kiteui.views.direct.webViewAttributes
import com.lightningkite.kiteui.views.direct.WebViewSource
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.webView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The web view's capability model, checked in the markup server-side rendering emits.
 *
 * Worth testing separately from the browser tests even though both come from the one
 * `commonHtmlMain` implementation: this markup is what a browser enforces the policy from, and it
 * reaches the page through a different attribute writer - one that has to escape values, where the
 * DOM path does not. `srcdoc` is HTML nested inside an HTML attribute, so a gap in that escaping
 * would let framed content close the attribute and write markup into the embedding page itself.
 */
class WebViewSsrTest {
    init {
        Dispatchers.setMain(Dispatchers.IO)
    }

    private fun renderHtml(build: ViewWriter.() -> Unit): String {
        val writer = Frame(ElementContext("/"))
        with(writer) { col { build() } }
        return buildString { writer.children[0].native.render(this) }
    }

    @Test
    fun inlineHtmlIsEscapedIntoTheSrcdocAttribute() {
        val html = renderHtml {
            webView { load(WebViewSource.Html("""<p class="x">hi</p>"""), setOf(WebViewPermission.Scripts)) }
        }

        assertTrue("srcdoc=" in html, "no srcdoc attribute was emitted: $html")
        // The raw markup must not survive unescaped: its quotes would terminate the attribute and
        // everything after them would be parsed as part of the embedding page.
        assertFalse(
            """<p class="x">""" in html,
            "srcdoc content was emitted unescaped, so it can break out of the attribute: $html",
        )
        assertTrue("&lt;p" in html, "the escaped markup should still be there: $html")
    }

    @Test
    fun theSandboxAttributeSurvivesIntoTheRenderedMarkup() {
        val html = renderHtml { webView { load(WebViewSource.Url("https://example.com")) } }

        assertTrue("sandbox=" in html, "no sandbox attribute in the rendered markup: $html")
        assertFalse("allow-scripts" in html, "scripts were not granted, so the token must be absent: $html")
    }

    @Test
    fun inlineHtmlDoesNotGetSameOriginInRenderedMarkup() {
        val html = renderHtml {
            webView { load(WebViewSource.Html("<p>hi</p>"), WebViewPermission.trusted) }
        }

        assertTrue("allow-scripts" in html, "precondition: scripts were granted")
        assertFalse(
            "allow-same-origin" in html,
            "inline HTML must never be given an origin, server-side either: $html",
        )
    }
}

/**
 * The order [webViewAttributes] returns its attributes in.
 *
 * Separate from the rendered-markup tests above because they cannot see this: every one of them
 * inspects final attribute *values*, which are order-independent. Writing `src`/`srcdoc` before
 * `sandbox`/`allow` would leave all of them green while starting each load under the previous load's
 * policy - and for a Url followed by an Html, "the previous policy" is the one with
 * `allow-same-origin`, handing untrusted markup the app's own origin.
 */
class WebViewAttributeOrderTest {

    private fun names(source: WebViewSource?, permissions: Set<WebViewPermission> = emptySet()) =
        webViewAttributes(source, permissions).map { it.first }

    @Test
    fun policyIsWrittenBeforeTheContentItGoverns() {
        for (source in listOf(WebViewSource.Url("https://example.com"), WebViewSource.Html("<p>hi</p>"))) {
            val order = names(source, WebViewPermission.trusted)
            val lastPolicy = maxOf(order.indexOf("sandbox"), order.indexOf("allow"))
            val firstContent = order.indexOfFirst { it == "src" || it == "srcdoc" }

            assertTrue(lastPolicy >= 0, "sandbox and allow must always be written: $order")
            assertTrue(firstContent >= 0, "$source should write a content attribute: $order")
            assertTrue(
                lastPolicy < firstContent,
                "policy must be written before content or the load runs under the old policy: $order",
            )
        }
    }

    @Test
    fun theOtherKindsContentAttributeIsAlwaysCleared() {
        assertEquals(listOf("sandbox", "allow", "srcdoc", "src"), names(WebViewSource.Url("https://example.com")))
        assertEquals(listOf("sandbox", "allow", "src", "srcdoc"), names(WebViewSource.Html("<p>hi</p>")))
    }

    @Test
    fun anUnloadedViewStillWritesAPolicy() {
        assertEquals(listOf("sandbox", "allow"), names(null))
    }
}
