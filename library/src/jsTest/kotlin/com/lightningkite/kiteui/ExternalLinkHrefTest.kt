package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.direct.ExternalLink
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.externalLink
import org.w3c.dom.HTMLAnchorElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * What a rejected `ExternalLink.to` leaves in the real DOM.
 *
 * `ExternalLinkSchemeTest` already covers which URLs `to` rejects, but it runs on SSR, where
 * attributes are a plain map and a null simply is not written. The browser is a different sink: the
 * generated `attributes.href` accessor assigns the DOM *property*, and `href` is a non-nullable
 * `USVString` in the IDL, so assigning null stringified it to the literal `href="null"`. The link
 * stayed live and navigated to `/null` - which on any site with a catch-all route is a real page.
 *
 * That is why these assertions read `getAttribute` rather than the `href` property: the property
 * resolves against the document base and reports an absolute URL either way, so it cannot tell an
 * absent attribute from a present nonsense one. Only the attribute distinguishes them.
 */
class ExternalLinkHrefTest {

    private fun mount(setup: ExternalLink.() -> Unit): HTMLAnchorElement {
        lateinit var element: ExternalLink
        root(Theme(id = "unitTest")) {
            col {
                externalLink { element = this; setup() }
            }
        }
        return element.native.element as HTMLAnchorElement
    }

    @Test
    fun aRejectedTargetLeavesNoHrefAtAll() {
        val el = mount { to = "javascript:alert(1)" }

        assertNull(
            el.getAttribute("href"),
            "a rejected URL must remove the attribute outright, not write a placeholder",
        )
        // The precise regression: href="null" resolves against the base URL, so the anchor stayed
        // clickable and went somewhere real.
        assertFalse(
            el.href.endsWith("/null"),
            "a rejected link navigated to '${el.href}' instead of going nowhere",
        )
    }

    @Test
    fun everyRejectedSchemeIsRemovedNotStringified() {
        for (url in listOf(
            "javascript:alert(1)",
            "data:text/html,<script>alert(1)</script>",
            "file:///etc/passwd",
            "intent://scan/#Intent;scheme=zxing;end",
        )) {
            val el = mount { to = url }
            assertNull(el.getAttribute("href"), "$url should have left no href")
        }
    }

    @Test
    fun anAcceptedTargetStillReachesTheDom() {
        // The mirror of the above: over-removal would be the quieter failure, since nothing errors
        // and every ordinary link in an application would simply stop working.
        val el = mount { to = "https://example.com/docs" }

        assertEquals(
            "https://example.com/docs",
            el.getAttribute("href"),
            "a safe URL must be written through unchanged",
        )
    }

    @Test
    fun clearingTheTargetRemovesTheAttribute() {
        lateinit var element: ExternalLink
        root(Theme(id = "unitTest")) {
            col { externalLink { element = this; to = "https://example.com" } }
        }
        val el = element.native.element as HTMLAnchorElement

        element.to = null

        assertNull(el.getAttribute("href"), "clearing the target must remove the attribute")
    }

    @Test
    fun sameTabLinksCarryNoRelAttribute() {
        // Same null-stringification trap, one property over: this used to emit rel="null" on every
        // link that was not opened in a new tab.
        val el = mount { to = "https://example.com"; newTab = false }

        assertNull(el.getAttribute("rel"), "a same-tab link must not carry a rel attribute")
        assertEquals("_self", el.getAttribute("target"))
    }

    @Test
    fun newTabLinksStillCarryTheProtectiveRel() {
        val el = mount { to = "https://example.com"; newTab = true }

        assertEquals("noopener noreferrer", el.getAttribute("rel"))
        assertEquals("_blank", el.getAttribute("target"))
    }
}
