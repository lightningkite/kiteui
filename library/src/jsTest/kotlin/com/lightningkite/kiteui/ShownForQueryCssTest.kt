package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.MediaQuery
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.shownForQuery
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.native
import org.w3c.dom.HTMLElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `shownForQuery` on web, checked against a real browser's cascade and layout.
 *
 * On the native targets this is a reactive condition with unit tests
 * (`MediaQueryEvaluationTest`); on web it is a generated CSS class, so the only thing worth
 * asserting is what the browser actually does with it. Two regressions live here:
 *
 * - Every element stayed hidden regardless of viewport, because the class was emitted as a base
 *   rule plus an `@media` override and `DynamicCss.rule` inserts at the *top* of the sheet - so the
 *   override, added second, ended up first and lost the cascade to an equal-specificity rule.
 * - Hidden elements still reserved their full layout box, because `visibility: collapse` computes
 *   to `hidden` on anything that is not a table row or column.
 *
 * The queries below are chosen to match or not match at any window size, so the test does not
 * depend on the size of the browser karma happens to launch.
 */
class ShownForQueryCssTest {

    private fun mount(query: MediaQuery): HTMLElement {
        lateinit var element: Element
        root(Theme(id = "unitTest")) {
            col {
                shownForQuery(query).col {
                    element = this
                    text("content")
                }
            }
        }
        return element.native.element as HTMLElement
    }

    /** Always true: every viewport is at least one pixel wide. */
    private val alwaysMatches = MediaQuery.MinWidth(1.px)

    /** Never true, short of a wall of monitors. */
    private val neverMatches = MediaQuery.MinWidth(999999.px)

    @Test
    fun aMatchingQueryLeavesTheElementVisible() {
        val el = mount(alwaysMatches)

        assertTrue(
            kotlinx.browser.window.getComputedStyle(el).display != "none",
            "a matching query must not hide the element",
        )
        assertTrue(
            el.getBoundingClientRect().height > 0,
            "a matching element must still be laid out",
        )
    }

    @Test
    fun aNonMatchingQueryHidesTheElement() {
        val el = mount(neverMatches)

        assertEquals(
            "none",
            kotlinx.browser.window.getComputedStyle(el).display,
            "a non-matching query must hide the element",
        )
    }

    @Test
    fun aNonMatchingElementTakesNoSpace() {
        // The other half of the bug: the element was hidden but still reserved its whole box, so a
        // page full of alternatives showed blank gaps rather than collapsing.
        val el = mount(neverMatches)

        val rect = el.getBoundingClientRect()
        assertEquals(0.0, rect.height, "a hidden element must not reserve height")
        assertEquals(0.0, rect.width, "a hidden element must not reserve width")
    }

    @Test
    fun aMatchingElementIsNotJustInvisibleButActuallyDisplayed() {
        // Guards the specific wrong fix: visibility keeps the box, so asserting only "not hidden"
        // would pass on the broken version too.
        val el = mount(alwaysMatches)

        val computed = kotlinx.browser.window.getComputedStyle(el)
        assertTrue(
            computed.visibility != "collapse" && computed.visibility != "hidden",
            "a matching element must be visible, was '${computed.visibility}'",
        )
    }

    @Test
    fun theSameQueryReusesOneGeneratedClass() {
        // querySet used to compute a name, emit its rules, and never record it - so every element
        // using a given query re-emitted the same CSS, growing the stylesheet without bound.
        val first = mount(alwaysMatches)
        val second = mount(alwaysMatches)

        val firstQueryClass = first.className.split(" ").single { it.startsWith("querySet_") }
        val secondQueryClass = second.className.split(" ").single { it.startsWith("querySet_") }
        assertEquals(firstQueryClass, secondQueryClass, "one query should mean one class")
    }
}
