package com.lightningkite.kiteui.views

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.Recycler2
import com.lightningkite.kiteui.views.l2.children
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class RecyclerTest {
    private fun ComposeUiTest.showList(vertical: Boolean = true, setup: Recycler2.() -> Unit): Recycler2 {
        var recycler: Recycler2? = null
        showKiteUi {
            col {
                val sized = sizeConstraints(width = 300.px, height = 400.px)
                val build: Recycler2.() -> Unit = { recycler = this; setup() }
                if (vertical) sized.recyclerView(setup = build) else sized.horizontalRecyclerView(setup = build)
            }
        }
        return recycler!!
    }

    private fun items(count: Int) = Signal((0 until count).toList())

    private fun Recycler2.cellsOf(items: Signal<List<Int>>) = children(items, id = { it }) { item ->
        text { ::content { "Item ${item()}" } }
    }

    /** The items on screen, top to bottom (or left to right).  Bounds in root are clipped to the list's viewport. */
    private fun ComposeUiTest.visibleItems(vertical: Boolean = true): List<String> =
        onAllNodes(hasText("Item ", substring = true)).fetchSemanticsNodes()
            .filter { it.boundsInRoot.width > 1f && it.boundsInRoot.height > 1f }
            .sortedBy { if (vertical) it.boundsInRoot.top else it.boundsInRoot.left }
            .map { it.config[SemanticsProperties.Text].joinToString() }

    private fun ComposeUiTest.awaitFirstVisible(expected: String, vertical: Boolean = true) {
        try {
            waitUntil(timeoutMillis = 3_000) { visibleItems(vertical).firstOrNull() == expected }
        } catch (e: ComposeTimeoutException) {
            throw AssertionError("Expected $expected first, saw ${visibleItems(vertical)}", e)
        }
    }

    private fun onMain(action: () -> Unit) = runBlocking(Dispatchers.Main) { action() }

    @Test
    fun showsTheFirstItemsInOrder() = runKiteUiTest {
        showList { cellsOf(items(1000)) }
        assertEquals((0..9).map { "Item $it" }, visibleItems().take(10))
    }

    @Test
    fun mouseWheelScrollsOnward() = runKiteUiTest {
        showList { cellsOf(items(1000)) }
        onNodeWithText("Item 3").performMouseInput { moveTo(center); scroll(20f) }
        waitUntil(timeoutMillis = 3_000) { visibleItems().firstOrNull() != "Item 0" }
        val shown = visibleItems().map { it.removePrefix("Item ").toInt() }
        assertEquals((shown.first()..shown.last()).toList(), shown, "Expected consecutive items, saw $shown")
    }

    @Test
    fun jumpsToAnIndex() = runKiteUiTest {
        val recycler = showList { cellsOf(items(1000)) }
        onMain { recycler.scrollToIndex(500, Align.Start, animate = false) }
        awaitFirstVisible("Item 500")
    }

    @Test
    fun scrollsToAnIndexOffScreen() = runKiteUiTest {
        val recycler = showList { cellsOf(items(1000)) }
        onMain { recycler.scrollToIndex(500, Align.Start, animate = true) }
        awaitFirstVisible("Item 500")
    }

    @Test
    fun scrollsToAnIndexOnScreen() = runKiteUiTest {
        val recycler = showList { cellsOf(items(1000)) }
        onMain { recycler.scrollToIndex(5, Align.Start, animate = true) }
        awaitFirstVisible("Item 5")
    }

    @Test
    fun scrollsBackAfterAJump() = runKiteUiTest {
        val recycler = showList { cellsOf(items(1000)) }
        onMain { recycler.scrollToIndex(500, Align.Start, animate = false) }
        awaitFirstVisible("Item 500")
        onMain { recycler.scrollToIndex(10, Align.Start, animate = true) }
        awaitFirstVisible("Item 10")
    }

    @Test
    fun followsItsData() = runKiteUiTest {
        val data = items(1000)
        val recycler = showList { cellsOf(data) }
        onMain { recycler.scrollToIndex(500, Align.Start, animate = false) }
        awaitFirstVisible("Item 500")
        data.value = (0 until 5).toList()
        waitUntil(timeoutMillis = 3_000) { visibleItems() == (0 until 5).map { "Item $it" } }
    }

    private fun ComposeUiTest.showPager(): Recycler2 {
        var pager: Recycler2? = null
        showKiteUi {
            col {
                sizeConstraints(width = 300.px, height = 200.px).viewPager {
                    pager = this
                    cellsOf(items(10))
                }
            }
        }
        return pager!!
    }

    @Test
    fun pagerButtonsTurnPages() = runKiteUiTest {
        val pager = showPager()
        onNodeWithContentDescription("Next", useUnmergedTree = true).performClick()
        waitUntil(timeoutMillis = 3_000) { pager.centerIndex.state.getOrNull() == 1 }
        awaitFirstVisible("Item 1", vertical = false)
    }

    @Test
    fun pagerSettlesOnAPage() = runKiteUiTest {
        showPager()
        onNodeWithText("Item 0").performMouseInput { moveTo(center); scroll(2f, ScrollWheel.Horizontal) }
        waitUntil(timeoutMillis = 3_000) { visibleItems(vertical = false).size == 1 }
    }

    // Pagers set scrollSnapStop, so however far a scroll goes, it stops at the next page.
    @Test
    fun pagerStopsAtTheNextPage() = runKiteUiTest {
        showPager()
        onNodeWithText("Item 0").performMouseInput { moveTo(center); scroll(200f, ScrollWheel.Horizontal) }
        awaitFirstVisible("Item 1", vertical = false)
        waitUntil(timeoutMillis = 3_000) { visibleItems(vertical = false) == listOf("Item 1") }
    }
}
