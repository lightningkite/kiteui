package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.direct.WebView
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.webView
import com.lightningkite.kiteui.views.native
import org.w3c.dom.HTMLIFrameElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * `WebView.permitJs` on web, checked against a real mounted `<iframe>`.
 *
 * Before the fix, `permitJs` was a bare property with no custom setter at all: writing to it never
 * touched the DOM, so the iframe was never actually sandboxed regardless of the value. The fix
 * gives it a setter that applies (or removes) the `sandbox` attribute via `setAttribute`. These
 * tests confirm the attribute actually lands on a real, already-mounted `<iframe>` in both
 * directions.
 *
 * (Note: the production code's comment says direct property assignment, i.e.
 * `native.attributes.sandbox = value`, would throw because `HTMLIFrameElement.sandbox` is a
 * read-only `DOMTokenList`. A real-browser check of that specific claim did not hold up - direct
 * assignment succeeded rather than throwing, since the IDL attribute is `[PutForwards=value]` and
 * forwards to the token list's `.value` setter. That doesn't change the correctness of the actual
 * fix - `setAttribute` is a perfectly good and arguably more direct way to set the attribute - so
 * no test here asserts the throwing behavior.)
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

    @Test
    fun defaultPermitJsLeavesTheIframeUnsandboxed() {
        val view = newWebView()
        assertNull(view.iframe.getAttribute("sandbox"), "permitJs defaults to true, so no sandbox attribute should be present")
    }

    @Test
    fun disablingJsAfterMountSandboxesTheIframe() {
        // Setting permitJs on an already-mounted element (rather than during initial setup, before
        // a real DOM node exists) is the scenario the old bare-property implementation could never
        // satisfy, since it never wrote to the DOM at all.
        val view = newWebView()

        view.permitJs = false

        assertEquals(
            "allow-same-origin allow-forms allow-popups allow-modals",
            view.iframe.getAttribute("sandbox"),
            "disabling JS must sandbox the iframe but omit allow-scripts"
        )
    }

    @Test
    fun reEnablingJsAfterMountRemovesTheSandboxAttribute() {
        val view = newWebView()
        view.permitJs = false

        view.permitJs = true

        assertNull(view.iframe.getAttribute("sandbox"), "re-enabling JS must drop the sandbox attribute entirely")
    }

}
