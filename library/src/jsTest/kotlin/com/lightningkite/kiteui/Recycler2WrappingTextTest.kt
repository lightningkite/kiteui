@file:OptIn(DelicateCoroutinesApi::class)

package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.card
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.recyclerView
import com.lightningkite.kiteui.views.direct.sizeConstraints
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.expanding
import com.lightningkite.kiteui.views.l2.children
import com.lightningkite.kiteui.views.native
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.core.Constant
import kotlinx.browser.document
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Recycler2 places cells by their measured size.  The default web font (Montserrat, display=swap)
 * usually arrives after the first layout and makes wrapped text taller without any DOM mutation or
 * resize, so the stale measurements left cells overlapping (a chat list of multi-line messages).
 */
class Recycler2WrappingTextTest {

    @Test
    fun cellsAreRemeasuredWhenFontsFinishLoading() = GlobalScope.promise {
        val messages = (1..8).map { n ->
            "Message $n is long enough that it has to wrap onto several lines in a narrow chat column."
        }
        val cells = ArrayList<Element>()
        root(Theme(id = "recyclerFontTest")) {
            sizeConstraints(width = 300.px).col {
                expanding.recyclerView {
                    gap = 0.5.rem
                    children(Constant(messages), id = { it }) { message ->
                        card.col {
                            cells += this
                            text { ::content { message() } }
                        }
                    }
                }
            }
        }
        delay(300)

        // Stand-in for a web font arriving: wider text, with no mutation inside any cell.
        val style = document.createElement("style")
        style.textContent = ".t-recyclerFontTest * { letter-spacing: 4px !important; }"
        document.head!!.appendChild(style)
        document.asDynamic().fonts.dispatchEvent(Event("loadingdone"))
        delay(300)

        val placed = cells.map { it.native.element as HTMLElement }
            .filter { !it.hidden && it.isConnected }
            .sortedBy { it.getBoundingClientRect().top }
        assertTrue(placed.size >= 2, "expected several cells placed, got ${placed.size}")
        for (cell in placed) {
            assertTrue(
                cell.scrollHeight <= cell.clientHeight + 1,
                "content (${cell.scrollHeight}px) overflows its cell (${cell.clientHeight}px): ${cell.textContent}",
            )
        }
        for ((above, below) in placed.zipWithNext()) {
            assertTrue(
                above.getBoundingClientRect().bottom <= below.getBoundingClientRect().top + 0.5,
                "'${above.textContent}' overlaps '${below.textContent}'",
            )
        }
        document.head!!.removeChild(style)
    }
}
