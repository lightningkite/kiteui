@file:OptIn(DelicateCoroutinesApi::class, ExperimentalKiteUi::class)

package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.ListSemantic
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.SemanticOverrides
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.override
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.direct.TextView
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.sizeConstraints
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.l2.Recycler2
import com.lightningkite.kiteui.views.l2.Recycler2WindowedList
import com.lightningkite.kiteui.views.l2.WindowedList
import com.lightningkite.kiteui.views.l2.platformWindowedList
import com.lightningkite.kiteui.views.padding
import com.lightningkite.kiteui.views.themed
import com.lightningkite.kiteui.views.write
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.core.Signal
import kotlinx.browser.window
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Run against both implementations, so the web list and [Recycler2] keep behaving alike. */
abstract class WindowedListScenarios(private val makeList: ElementContext.() -> WindowedList) {
    private val padding = 10

    private inner class Mounted(items: List<Int>, setup: WindowedList.() -> Unit = {}) {
        val items = Signal(items)
        lateinit var list: WindowedList
        val rows = ArrayList<TextView>()

        init {
            // The list's own gap (2px, not cascading) differs from the surrounding theme's (12px).
            val theme = Theme(
                id = "unitTest",
                gap = 12.px,
                semanticOverrides = SemanticOverrides(ListSemantic.override { it.withoutBack(cascading = false, gap = 2.px) }),
            )
            root(theme) {
                col {
                    sizeConstraints(width = 300.px, height = 300.px).themed(ListSemantic).write(context.makeList()) {
                        list = this
                        this.padding = this@WindowedListScenarios.padding.px
                        setup()
                        children(this@Mounted.items, id = { it }) { item ->
                            text {
                                // Cells of different heights.
                                ::content { "row ${item()}." + " word".repeat(item() % 5 * 9) }
                                rows += this
                            }
                        }
                    }
                }
            }
        }

        fun rectOf(index: Int): Rect? = rows.filter { it.content.startsWith("row $index.") }
            .mapNotNull { it.native.screenRectangle() }.singleOrNull { it.height > 0 }
        val viewport: Rect get() = list.underlyingNativeElement.native.screenRectangle()!!
        fun topError(index: Int): Double = rectOf(index)!!.top - (viewport.top + padding.toDouble())
        fun bottomError(index: Int): Double = (viewport.bottom - padding.toDouble()) - rectOf(index)!!.bottom
        fun visible(index: Int): Boolean = rectOf(index)?.let { it.bottom > viewport.top && it.top < viewport.bottom } ?: false

        // Whether row 0 is in view in each of the next frames, recorded after the list updates and before paint.
        fun watchRow0(): List<Boolean> {
            val painted = ArrayList<Boolean>()
            fun watch(frames: Int) {
                window.requestAnimationFrame {
                    painted += visible(0)
                    if (frames > 0) watch(frames - 1)
                }
            }
            watch(20)
            return painted
        }
    }

    @Test
    fun firstCellAtContentTop() = GlobalScope.promise {
        val m = Mounted((0..39).toList())
        delay(500)
        assertEquals(0.0, m.topError(0), 1.0)
    }

    @Test
    fun alignsToPaddingBox() = GlobalScope.promise {
        val m = Mounted((0..39).toList())
        delay(500)
        m.list.scrollToIndex(20, Align.End, animate = false)
        delay(500)
        val end20 = m.bottomError(20)
        m.list.scrollToIndex(20, Align.Start, animate = false)
        delay(500)
        val start20 = m.topError(20)
        m.list.scrollToIndex(0, Align.Start, animate = false)
        delay(500)
        val start0 = m.topError(0)
        m.list.scrollToIndex(39, Align.End, animate = false)
        delay(500)
        val end39 = m.bottomError(39)
        m.list.scrollToIndex(39, Align.End, animate = false)
        delay(500)
        val end39again = m.bottomError(39)
        assertEquals(0.0, end20, 1.0, "End(20)")
        assertEquals(0.0, start20, 1.0, "Start(20)")
        assertEquals(0.0, start0, 1.0, "Start(0)")
        assertEquals(0.0, end39, 1.0, "End(39)")
        assertEquals(0.0, end39again, 1.0, "End(39) again")
    }

    @Test
    fun openedAtEndFollowsNewItems() = GlobalScope.promise {
        repeat(5) { attempt ->
            val m = Mounted((0..39).toList())
            m.list.scrollToIndex(39, Align.End, animate = false)
            delay(500)
            val opened = m.bottomError(39)
            m.items.value += 40
            delay(500)
            val added = m.bottomError(40)
            assertEquals(0.0, opened, 1.0, "on open")
            assertEquals(0.0, added, 1.0, "after adding")
        }
    }

    @Test
    fun lateDataStartsAtEnd() = GlobalScope.promise {
        val m = Mounted(emptyList()) { startAtEnd = true }
        delay(300)
        m.items.value = (0..39).toList()
        val row0Painted = m.watchRow0()
        delay(500)
        val opened = m.bottomError(39)
        m.items.value += 40
        delay(500)
        val added = m.bottomError(40)
        m.list.scrollToIndex(20, Align.Start, animate = false)
        delay(500)
        val before = m.rectOf(20)!!.top
        m.items.value += 41
        delay(500)
        val moved = m.rectOf(20)!!.top - before
        assertTrue(row0Painted.none { it }, "row 0 painted: $row0Painted")
        assertEquals(0.0, opened, 1.0, "on open")
        assertEquals(0.0, added, 1.0, "after adding")
        assertEquals(0.0, moved, 1.0, "scrolled up")
    }

    @Test
    fun startAtEndNeverPaintsTheTop() = GlobalScope.promise {
        val m = Mounted((0..39).toList()) { startAtEnd = true }
        val row0Painted = m.watchRow0()
        delay(500)
        val opened = m.bottomError(39)
        assertTrue(row0Painted.none { it }, "row 0 painted: $row0Painted")
        assertEquals(0.0, opened, 1.0, "on open")
    }

    @Test
    fun shortListGrowsAndFollows() = GlobalScope.promise {
        val m = Mounted((0..1).toList()) { startAtEnd = true }
        delay(300)
        val errors = ArrayList<Double?>()
        for (next in 2..15) {
            m.items.value += next
            delay(200)
            errors += m.rectOf(next)?.let { (m.viewport.bottom - padding.toDouble()) - it.bottom }
        }
        // Rows sit at the top while they fit; once they overflow, the newest sits at the end.
        assertTrue(errors.dropWhile { it != null && it > 1.0 }.all { it != null && kotlin.math.abs(it) <= 1.0 }, "$errors")
    }

    @Test
    fun scrollToIndexBeforeDataExists() = GlobalScope.promise {
        val m = Mounted(emptyList())
        delay(300)
        val threw = runCatching { m.list.scrollToIndex(29, Align.End, animate = false) }.exceptionOrNull()
        m.items.value = (0..39).toList()
        delay(500)
        val error = m.rectOf(29)?.let { (m.viewport.bottom - padding.toDouble()) - it.bottom }
        assertEquals(null, threw, "threw")
        assertEquals(0.0, error!!, 1.0)
    }
}

class FlowWindowedListTest : WindowedListScenarios({ platformWindowedList(this) })
class Recycler2WindowedListTest : WindowedListScenarios({ Recycler2WindowedList(Recycler2(this)) })
