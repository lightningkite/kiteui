@file:OptIn(DelicateCoroutinesApi::class)

package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.views.direct.TextView
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.recyclerView
import com.lightningkite.kiteui.views.direct.sizeConstraints
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.l2.Recycler2
import com.lightningkite.kiteui.views.l2.children
import com.lightningkite.kiteui.views.padding
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Signal
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Scrolling a recycler to its end, and staying there.
 *
 * Aligning an item with the end once ignored the end padding, so a chat opened at its latest message
 * stopped one padding short.  And a list resting at its end once stayed put while its last cell grew
 * (a message rendering late), cutting that cell off.
 */
class Recycler2ScrollToEndTest {
    private val padding = 15
    private val lastRow = 19

    /** A 200px list of [lastRow] + 1 one-line rows; the last row's text is [lastRowText]. */
    private class Mounted(padding: Int, lastRow: Int) {
        val lastRowText = Signal("row")
        lateinit var recycler: Recycler2
        val rows = ArrayList<TextView>()

        init {
            root(Theme(id = "unitTest")) {
                col {
                    sizeConstraints(width = 200.px, height = 200.px).recyclerView {
                        recycler = this
                        this.padding = padding.px
                        gap = 0.px
                        children(Constant((0..lastRow).toList()), id = { it }) { item ->
                            text {
                                ::content { if (item() == lastRow) lastRowText() else "row ${item()}" }
                                rows += this
                            }
                        }
                    }
                }
            }
        }

        fun rectOf(content: String): Rect {
            val visible = rows.filter { it.content == content }.map { it.native.screenRectangle()!! }.filter { it.height > 0 }
            assertEquals(1, visible.size, "exactly one cell should show '$content', found $visible")
            return visible.single()
        }
        val viewport: Rect get() = recycler.outerFrame.native.screenRectangle()!!
    }

    @Test
    fun alignEndLeavesBottomPadding() = GlobalScope.promise {
        val list = Mounted(padding, lastRow)
        delay(500)
        list.recycler.scrollToIndex(lastRow, Align.End, animate = false)
        delay(500)
        assertEquals(list.viewport.bottom - padding, list.rectOf(list.lastRowText.value).bottom, 1.0)
    }

    @Test
    fun staysAtEndWhenLastCellGrows() = GlobalScope.promise {
        val list = Mounted(padding, lastRow)
        delay(500)
        list.recycler.scrollToIndex(lastRow, Align.End, animate = false)
        delay(500)
        list.lastRowText.value = "row ".repeat(30)
        delay(500)
        val last = list.rectOf(list.lastRowText.value)
        assertEquals(list.viewport.bottom - padding, last.bottom, 1.0, "the grown last row is at $last")
    }

    @Test
    fun staysPutAwayFromEndWhenLastCellGrows() = GlobalScope.promise {
        val list = Mounted(padding, lastRow)
        delay(500)
        list.recycler.scrollToIndex(lastRow, Align.End, animate = false)
        delay(500)
        list.recycler.scrollToIndex(5, Align.Start, animate = false)
        delay(500)
        val before = list.rectOf("row 5").top
        list.lastRowText.value = "row ".repeat(30)
        delay(500)
        assertEquals(before, list.rectOf("row 5").top, 1.0)
    }
}
