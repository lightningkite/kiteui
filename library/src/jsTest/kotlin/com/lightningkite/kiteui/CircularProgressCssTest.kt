package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.direct.CircularProgress
import com.lightningkite.kiteui.views.direct.circularProgress
import com.lightningkite.kiteui.views.direct.col
import org.w3c.dom.HTMLElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The web progress ring, checked against a real browser's style resolution.
 *
 * The ring is painted by CSS rather than built in script, which means "does it draw" is a question
 * about whether the stylesheet rule actually matches and resolves - something no amount of Kotlin
 * unit testing can answer. These tests ask the browser itself: they mount the element, then read
 * back the computed style it resolved.
 */
class CircularProgressCssTest {

    private fun mount(setup: CircularProgress.() -> Unit): HTMLElement {
        lateinit var element: CircularProgress
        // root() appends the tree to document.body itself, which is what makes the browser
        // resolve styles against it - an unattached element reports nothing useful.
        root(Theme(id = "unitTest")) {
            col {
                circularProgress { element = this; setup() }
            }
        }
        return element.native.element as HTMLElement
    }

    @Test
    fun rendersANativeProgressElement() {
        val el = mount { ratio = 0.5f }
        // The tag is the accessibility contract: screen readers read the value from <progress>
        // itself, so replacing it with a styled div would silently drop the announcement.
        assertEquals("PROGRESS", el.tagName, "the ring must be a native <progress> element")
        assertEquals("1", el.getAttribute("max"), "max must be explicit so value reads as a fraction")
        assertEquals("0.5", el.getAttribute("value"), "value must carry the ratio for assistive tech")
    }

    @Test
    fun theStylesheetRuleActuallyMatches() {
        val el = mount { ratio = 0.25f }
        val computed = kotlinx.browser.window.getComputedStyle(el)

        // If the rule did not match, backgroundImage resolves to "none" and the ring is invisible
        // even though every attribute above is correct - the exact failure a markup-only test misses.
        val background = computed.backgroundImage
        assertTrue(
            background.contains("conic-gradient"),
            "the ring's conic-gradient did not resolve; background-image was '$background'"
        )
        assertEquals("50%", computed.borderRadius, "the element must be a circle")
    }

    @Test
    fun theCustomPropertyDrivesTheSweep() {
        val el = mount { ratio = 0.25f }
        assertEquals(
            "0.25",
            kotlinx.browser.window.getComputedStyle(el).getPropertyValue("--kiteui-progress").trim(),
            "the fill amount must reach CSS through the custom property"
        )
    }

    @Test
    fun updatingTheRatioUpdatesWhatIsPainted() {
        lateinit var element: CircularProgress
        root(Theme(id = "unitTest")) {
            col { circularProgress { element = this; ratio = 0.1f } }
        }
        val el = element.native.element as HTMLElement

        element.ratio = 0.9f

        assertEquals(
            "0.9",
            kotlinx.browser.window.getComputedStyle(el).getPropertyValue("--kiteui-progress").trim(),
            "changing the ratio must repaint the arc, not just the accessible value"
        )
        assertEquals("0.9", el.getAttribute("value"), "the accessible value must follow too")
    }

    @Test
    fun theRatioIsClampedBeforeItReachesTheDom() {
        val el = mount { ratio = 4f }
        assertEquals("1", el.getAttribute("value"), "an over-full ratio must clamp, not overflow the sweep")
    }
}
