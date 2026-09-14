package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.WordBreak
import com.lightningkite.kiteui.views.direct.TextView
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.text
import org.w3c.dom.HTMLElement
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `TextView.wordBreak` on web.
 *
 * The getter was once unimplemented and threw, so merely reading back a value the same class lets
 * you set crashed - which also meant the setter's CSS write was never verifiable. These tests read
 * the property back (proving the getter returns rather than throws) and check the resolved CSS
 * (proving the setter's write still reaches the DOM).
 */
class TextViewWordBreakTest {

    private fun mount(setup: TextView.() -> Unit): TextView {
        lateinit var element: TextView
        root(Theme(id = "unitTest")) {
            col { text { element = this; setup() } }
        }
        return element
    }

    @Test
    fun freshTextViewReadsBackNormal() {
        // A bare property read: this alone would have thrown NotImplementedError before the fix.
        val view = mount { }
        assertEquals(WordBreak.Normal, view.wordBreak, "a fresh TextView must default to Normal, matching Android")
    }

    @Test
    fun settingBreakAllRoundTripsAndAppliesTheCss() {
        val view = mount { wordBreak = WordBreak.BreakAll }

        assertEquals(WordBreak.BreakAll, view.wordBreak, "reading back the value just set must not throw")

        val computed = kotlinx.browser.window.getComputedStyle(view.native.element as HTMLElement)
        assertEquals("break-all", computed.wordBreak, "the CSS must actually be applied, not just remembered")
    }

    @Test
    fun settingNormalAfterBreakAllRoundTrips() {
        val view = mount { wordBreak = WordBreak.BreakAll; wordBreak = WordBreak.Normal }

        assertEquals(WordBreak.Normal, view.wordBreak)

        val computed = kotlinx.browser.window.getComputedStyle(view.native.element as HTMLElement)
        assertEquals("normal", computed.wordBreak, "switching back to Normal must update the resolved CSS too")
    }
}
