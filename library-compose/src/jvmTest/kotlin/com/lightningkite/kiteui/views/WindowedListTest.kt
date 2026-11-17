@file:OptIn(ExperimentalKiteUi::class)

package com.lightningkite.kiteui.views

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.WindowedList
import com.lightningkite.kiteui.views.l2.windowedList
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class WindowedListTest {
    private fun ComposeUiTest.showList(items: Signal<List<Int>>): WindowedList {
        var list: WindowedList? = null
        showKiteUi {
            col {
                sizeConstraints(width = 300.px, height = 400.px).windowedList {
                    list = this
                    children(items, id = { it }) { item ->
                        text { ::content { "Item ${item()}" } }
                    }
                }
            }
        }
        return list!!
    }

    /** The items on screen, top to bottom.  Bounds in root are clipped to the list's viewport. */
    private fun ComposeUiTest.visibleItems(): List<String> =
        onAllNodes(hasText("Item ", substring = true)).fetchSemanticsNodes()
            .filter { it.boundsInRoot.width > 1f && it.boundsInRoot.height > 1f }
            .sortedBy { it.boundsInRoot.top }
            .map { it.config[SemanticsProperties.Text].joinToString() }

    private fun ComposeUiTest.awaitVisible(description: String, condition: (List<String>) -> Boolean) {
        try {
            waitUntil(timeoutMillis = 3_000) { condition(visibleItems()) }
        } catch (e: ComposeTimeoutException) {
            throw AssertionError("Expected $description, saw ${visibleItems()}", e)
        }
    }

    private fun onMain(action: () -> Unit) = runBlocking(Dispatchers.Main) { action() }

    @Test
    fun showsTheFirstItemsInOrder() = runKiteUiTest {
        showList(Signal((0 until 1000).toList()))
        assertEquals((0..9).map { "Item $it" }, visibleItems().take(10))
    }

    @Test
    fun showsEachItemByItsOwnKindOfCell() = runKiteUiTest {
        val items = Signal((0 until 1000).toList())
        var list: WindowedList? = null
        showKiteUi {
            col {
                sizeConstraints(width = 300.px, height = 400.px).windowedList {
                    list = this
                    childrenMultipleTypes(items, id = { it }) {
                        elementsMatching { it % 3 == 0 } renderedAs { item -> text { ::content { "Item ${item()} third" } } }
                        elementsMatching { true } renderedAs { item -> text { ::content { "Item ${item()} plain" } } }
                    }
                }
            }
        }
        fun kindOf(shown: String) = if (shown.removePrefix("Item ").substringBefore(' ').toInt() % 3 == 0) "third" else "plain"
        assertEquals(listOf("Item 0 third", "Item 1 plain", "Item 2 plain", "Item 3 third"), visibleItems().take(4))
        // Far enough that the cells on screen are reused ones.
        onMain { list!!.scrollToIndex(500, Align.Start, animate = false) }
        awaitVisible("Item 500 first") { it.firstOrNull()?.startsWith("Item 500 ") == true }
        val shown = visibleItems()
        assertEquals(shown.map { kindOf(it) }, shown.map { it.substringAfterLast(' ') }, "Expected each item in its own kind of cell, saw $shown")
    }

    @Test
    fun jumpsToAnIndex() = runKiteUiTest {
        val list = showList(Signal((0 until 1000).toList()))
        onMain { list.scrollToIndex(500, Align.Start, animate = false) }
        awaitVisible("Item 500 first") { it.firstOrNull() == "Item 500" }
    }

    @Test
    fun aListAtItsEndFollowsANewItem() = runKiteUiTest {
        val items = Signal((0 until 1000).toList())
        val list = showList(items)
        onMain { list.scrollToIndex(999, Align.End, animate = false) }
        awaitVisible("Item 999 last") { it.lastOrNull() == "Item 999" }
        onMain { items.value += 1000 }
        awaitVisible("Item 1000 last") { it.lastOrNull() == "Item 1000" }
    }
}
