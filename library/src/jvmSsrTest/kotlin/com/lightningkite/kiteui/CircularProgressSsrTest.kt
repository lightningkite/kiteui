@file:OptIn(ExperimentalCoroutinesApi::class)

package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.Frame
import com.lightningkite.kiteui.views.direct.circularProgress
import com.lightningkite.kiteui.views.direct.col
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Server-side rendering of [com.lightningkite.kiteui.views.direct.CircularProgress].
 *
 * A progress ring is a static value at render time, so it belongs in the initial HTML rather than
 * waiting for script. Previously the getter threw, the setter did nothing, and no markup was
 * emitted at all. Because the ring is painted from CSS, one implementation serves both the browser
 * and the server, so what the server emits here is also what the browser builds.
 *
 * These assertions run against the real HTML serializer rather than the view-tree snapshot, since
 * the point of the component is the markup it produces.
 */
class CircularProgressSsrTest {
    init {
        Dispatchers.setMain(Dispatchers.IO)
    }


    /** The serializer quotes attributes with `'`, but the browser DOM uses `"`; accept either. */
    private fun String.hasAttribute(name: String, value: String): Boolean =
        contains("$name='$value'") || contains("$name=\"$value\"")

    private fun renderHtml(build: ViewWriter.() -> Unit): String {
        val writer = Frame(ElementContext("/"))
        with(writer) { col { build() } }
        return buildString { writer.children[0].native.render(this) }
    }

    @Test
    fun ratioRoundTripsThroughTheProperty() {
        renderHtml {
            circularProgress {
                ratio = 0.25f
                assertEquals(0.25f, ratio, "ratio getter did not return what was set")
            }
        }
    }

    @Test
    fun ratioIsClampedToZeroThroughOne() {
        renderHtml {
            circularProgress {
                ratio = 5f
                assertEquals(1f, ratio, "ratio above 1 should clamp to 1")
                ratio = -3f
                assertEquals(0f, ratio, "ratio below 0 should clamp to 0")
            }
        }
    }

    @Test
    fun rendersAProgressElementCarryingTheValue() {
        // A <progress> is what makes the value legible to assistive technology without a
        // hand-maintained role/aria-valuenow pair, so the tag itself is part of the contract.
        val html = renderHtml { circularProgress { ratio = 0.5f } }
        assertTrue(html.contains("<progress"), "expected a <progress> element, got: $html")
        assertTrue(html.hasAttribute("value", "0.5"), "expected the ratio in the value attribute: $html")
        assertTrue(html.hasAttribute("max", "1"), "expected an explicit max so value reads as a fraction: $html")
    }

    @Test
    fun theFilledArcIsDescribedInTheMarkup() {
        // The ring is painted by CSS from this custom property. If it were missing, an SSR page
        // would show an empty ring until script ran - the exact gap this component used to have.
        val html = renderHtml { circularProgress { ratio = 0.75f } }
        assertTrue(html.contains("--kiteui-progress"), "ring fill property missing from SSR html: $html")
        assertTrue(html.contains("0.75"), "ring fill amount missing from SSR html: $html")
    }

    @Test
    fun aFreshRingRendersEmptyRatherThanUnset() {
        // Without an explicit starting value the CSS variable would be absent and the browser
        // would fall back to the stylesheet default; asserting it here keeps SSR and client
        // markup identical, which is what hydration compares.
        val html = renderHtml { circularProgress { } }
        assertTrue(html.hasAttribute("value", "0"), "a new ring should render as zero progress: $html")
    }
}
