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
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import org.w3c.dom.HTMLElement
import org.w3c.dom.Text
import kotlin.test.Test
import kotlin.test.assertTrue

class Recycler2TextNodeEditTest {

    @Test
    fun cellsAreRemeasuredWhenATextNodeIsEditedInPlace() = GlobalScope.promise {
        val cells = ArrayList<Element>()
        root(Theme(id = "recyclerTextNodeTest")) {
            sizeConstraints(width = 300.px).col {
                expanding.recyclerView {
                    gap = 0.5.rem
                    children(Constant((1..8).map { "Message $it" }), id = { it }) { message ->
                        card.col {
                            cells += this
                            text { ::content { message() } }
                        }
                    }
                }
            }
        }
        delay(300)

        val placed = cells.map { it.native.element as HTMLElement }
            .filter { !it.hidden && it.isConnected }
            .sortedBy { it.getBoundingClientRect().top }
        assertTrue(placed.size >= 2, "expected several cells placed, got ${placed.size}")
        val textNode = generateSequence(placed.first().firstChild) { it.firstChild }.filterIsInstance<Text>().first()
        textNode.data = "This message was edited in place and is now long enough to wrap onto several lines."
        delay(300)

        val edited = placed.first()
        assertTrue(
            edited.scrollHeight <= edited.clientHeight + 1,
            "content (${edited.scrollHeight}px) overflows its cell (${edited.clientHeight}px)",
        )
    }
}
