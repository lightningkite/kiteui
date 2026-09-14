package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.direct.WebView
import com.lightningkite.kiteui.views.direct.WebViewPermission
import com.lightningkite.kiteui.views.direct.WebViewSource
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.webView
import com.lightningkite.kiteui.views.native
import org.w3c.dom.HTMLIFrameElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * `WebView` on web, checked against a real mounted `<iframe>`.
 *
 * The whole capability model lands in two attributes here - `sandbox` and `allow` - so these tests
 * pin the exact token set for each permission rather than spot-checking membership. A test that only
 * asserted "allow-scripts is absent" would pass against an implementation that dropped the sandbox
 * attribute altogether, which is the single worst thing this code can do: an iframe with no sandbox
 * attribute is granted *everything*, top-level navigation included.
 */
class WebViewSandboxTest {

    private fun newWebView(): WebView {
        lateinit var element: WebView
        root(Theme(id = "unitTest")) {
            col { webView { element = this } }
        }
        return element
    }

    private val WebView.iframe: HTMLIFrameElement get() = native.element as HTMLIFrameElement

    private fun WebView.sandbox(): Set<String> =
        iframe.getAttribute("sandbox")?.split(' ')?.filter { it.isNotBlank() }?.toSet()
            ?: error("no sandbox attribute present - that grants the frame everything")

    private fun WebView.allow(): Set<String> =
        iframe.getAttribute("allow")?.split(';')?.map { it.trim() }?.filter { it.isNotBlank() }?.toSet()
            ?: error("no allow attribute present")

    @Test
    fun aWebViewThatWasNeverLoadedIsStillSandboxed() {
        // The constructor has to write the policy. A property initializer does not run a setter and
        // load() may never be called, so nothing else would.
        val view = newWebView()
        assertEquals(setOf("allow-forms"), view.sandbox())
        assertEquals(setOf("camera 'none'", "microphone 'none'", "autoplay 'none'"), view.allow())
        assertNull(view.source)
        assertTrue(view.permissions.isEmpty())
    }

    @Test
    fun loadingWithNoPermissionsGrantsNothingBeyondForms() {
        val view = newWebView()
        view.load(WebViewSource.Url("https://example.com"))

        // allow-same-origin comes from the source being a real site, not from any permission.
        assertEquals(setOf("allow-forms", "allow-same-origin"), view.sandbox())
        assertEquals(setOf("camera 'none'", "microphone 'none'", "autoplay 'none'"), view.allow())
        assertEquals("https://example.com", view.iframe.getAttribute("src"))
    }

    @Test
    fun eachSandboxPermissionAddsExactlyItsOwnToken() {
        val cases = mapOf(
            WebViewPermission.Scripts to "allow-scripts",
            WebViewPermission.Popups to "allow-popups",
            WebViewPermission.Modals to "allow-modals",
            WebViewPermission.Downloads to "allow-downloads",
        )
        for ((permission, token) in cases) {
            val view = newWebView()
            view.load(WebViewSource.Url("https://example.com"), setOf(permission))
            assertEquals(
                setOf("allow-forms", "allow-same-origin", token),
                view.sandbox(),
                "granting $permission should add $token and nothing else",
            )
        }
    }

    @Test
    fun eachPermissionsPolicyFeatureIsNamedExplicitlyInBothDirections() {
        // Omitting a feature is not the same as denying it - autoplay is allowed by default - so the
        // attribute always names all three.
        val view = newWebView()
        view.load(WebViewSource.Url("https://example.com"), setOf(WebViewPermission.Camera, WebViewPermission.Autoplay))
        assertEquals(setOf("camera 'src'", "microphone 'none'", "autoplay 'src'"), view.allow())
    }

    @Test
    fun inlineHtmlNeverGetsSameOriginEvenWithScripts() {
        // The load-bearing one. srcdoc content inherits the *embedding page's* origin, so
        // allow-same-origin together with allow-scripts would let inline HTML - the case where the
        // markup is most likely to be untrusted - reach parent.document and rewrite this attribute.
        val view = newWebView()
        view.load(WebViewSource.Html("<p>hi</p>"), WebViewPermission.trusted)

        assertTrue("allow-scripts" in view.sandbox(), "precondition: scripts were granted")
        assertTrue(
            "allow-same-origin" !in view.sandbox(),
            "inline HTML must never get an origin: ${view.sandbox()}",
        )
        assertEquals("<p>hi</p>", view.iframe.getAttribute("srcdoc"))
    }

    @Test
    fun topNavigationIsNeverGrantedByAnyPermission() {
        // No permission maps to it and no platform offers framed content the equivalent, so no
        // combination should produce it.
        val view = newWebView()
        view.load(WebViewSource.Url("https://example.com"), WebViewPermission.entries.toSet())
        assertTrue(
            view.sandbox().none { it.startsWith("allow-top-navigation") || it == "allow-popups-to-escape-sandbox" },
            "escape-hatch tokens leaked into the sandbox: ${view.sandbox()}",
        )
    }

    @Test
    fun switchingSourceKindClearsTheAttributeOfTheOther() {
        // Leaving the old one behind would let the previous content keep showing: src wins over
        // srcdoc in some browsers and loses in others.
        val view = newWebView()
        view.load(WebViewSource.Url("https://example.com"))
        view.load(WebViewSource.Html("<p>hi</p>"))
        assertNull(view.iframe.getAttribute("src"))

        view.load(WebViewSource.Url("https://example.com"))
        assertNull(view.iframe.getAttribute("srcdoc"))
    }

    @Test
    fun permissionsDoNotAccumulateAcrossLoads() {
        val view = newWebView()
        view.load(WebViewSource.Url("https://example.com"), setOf(WebViewPermission.Scripts))
        view.load(WebViewSource.Url("https://example.com"))

        assertTrue("allow-scripts" !in view.sandbox(), "a later load kept the earlier load's permissions")
        assertTrue(view.permissions.isEmpty())
    }

    @Test
    fun sourceAndPermissionsReportWhatWasLoaded() {
        val view = newWebView()
        val source = WebViewSource.Html("<p>hi</p>")
        view.load(source, setOf(WebViewPermission.Scripts))

        assertEquals(source, view.source)
        assertEquals(setOf(WebViewPermission.Scripts), view.permissions)
    }
}
