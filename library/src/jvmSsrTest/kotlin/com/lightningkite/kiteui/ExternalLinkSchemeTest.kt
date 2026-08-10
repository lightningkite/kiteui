@file:OptIn(ExperimentalCoroutinesApi::class)

package com.lightningkite.kiteui

import com.lightningkite.kiteui.markdown.MarkdownConfig
import com.lightningkite.kiteui.markdown.markdown
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.Frame
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.externalLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * Scheme validation on `ExternalLink.to`, the sink that hands a URL to the platform.
 *
 * This is the security boundary: every caller gets the protection without opting in, which is why
 * the checks that used to sit in the markdown renderer were removed as duplicates. That removal is
 * only safe if the sink really does validate, so the markdown cases at the bottom exercise the
 * whole path rather than trusting the reasoning.
 *
 * Both directions matter. Rejecting an unsafe scheme is the point, but over-rejecting would
 * silently strip the destination from every ordinary link in an application - a worse outcome in
 * practice, because nothing errors and nobody notices until a user reports a dead link.
 */
class ExternalLinkSchemeTest {
    init {
        Dispatchers.setMain(Dispatchers.IO)
    }

    private fun <T> build(block: ViewWriter.() -> T): T {
        val writer = Frame(ElementContext("/"))
        var result: T? = null
        with(writer) { col { result = block() } }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    private fun linkTargetFor(url: String): String? = build {
        var target: String? = null
        externalLink {
            to = url
            target = to
        }
        target
    }

    private fun renderedHtml(block: ViewWriter.() -> Unit): String {
        val writer = Frame(ElementContext("/"))
        with(writer) { col { block() } }
        return buildString { writer.children[0].native.render(this) }
    }

    @Test
    fun scriptBearingSchemesAreRejected() {
        // Each of these reaches a different capability: script execution, an attacker-authored
        // document, local storage, and another installed application.
        assertNull(linkTargetFor("javascript:alert(1)"), "javascript: must not survive")
        assertNull(linkTargetFor("data:text/html,<script>alert(1)</script>"), "data: must not survive")
        assertNull(linkTargetFor("file:///etc/passwd"), "file: must not survive")
        assertNull(linkTargetFor("intent://scan/#Intent;scheme=zxing;end"), "intent: must not survive")
    }

    @Test
    fun schemeMatchingIgnoresCaseAndPadding() {
        // Browsers strip ASCII whitespace and control characters before resolving a scheme, so
        // these all reach the same handler as a plain javascript: URL.
        assertNull(linkTargetFor("JaVaScRiPt:alert(1)"), "scheme matching must be case-insensitive")
        assertNull(linkTargetFor("  javascript:alert(1)"), "leading whitespace must not defeat the check")
        assertNull(linkTargetFor("java\tscript:alert(1)"), "an embedded tab must not defeat the check")
    }

    @Test
    fun ordinaryLinksArePreservedExactly() {
        // Over-rejection is the quieter failure: no error, just links that go nowhere.
        for (url in listOf(
            "https://example.com/path?q=1&r=2#frag",
            "http://example.com",
            "mailto:someone@example.com",
            "tel:+15551234567",
            "sms:+15551234567",
            "/about",
            "docs/getting-started",
            "?tab=profile",
            "#section",
        )) {
            assertEquals(url, linkTargetFor(url), "safe URL must be preserved unchanged: $url")
        }
    }

    @Test
    fun aColonInAPathIsNotMistakenForAScheme() {
        // "/a:b" has a colon but no scheme - the delimiter comes first. Treating it as a scheme
        // would reject legitimate paths.
        assertEquals("/a:b", linkTargetFor("/a:b"))
        assertEquals("?x=1:2", linkTargetFor("?x=1:2"))
    }

    @Test
    fun markdownLinksGoThroughTheSinkAndCannotCarryScript() {
        // The renderer no longer checks schemes itself; it relies entirely on this sink. If that
        // judgement was wrong, this is where it shows up.
        val html = renderedHtml {
            markdown("[click me](javascript:alert(1))", config = MarkdownConfig.Default)
        }
        assertFalse(html.contains("javascript:"), "markdown link smuggled a javascript: target into the DOM: $html")
    }

    @Test
    fun markdownStillRendersOrdinaryLinks() {
        val html = renderedHtml {
            markdown("[docs](https://example.com/docs)", config = MarkdownConfig.Default)
        }
        assertFalse(
            !html.contains("https://example.com/docs"),
            "an ordinary markdown link lost its destination: $html"
        )
    }
}
