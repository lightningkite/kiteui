package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.Signal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Focused tests for the [renderList] API.
 *
 * Note: JVM SSR implements [com.lightningkite.kiteui.afterTimeout] as a no-op — it discards
 * the callback without ever calling it.  The keyed path schedules both the "show" (afterTimeout 1ms)
 * and the "remove" (afterTimeout transitionDuration+100ms) actions via afterTimeout, so in SSR:
 *
 * - A newly created keyed view is born with shown=false and never made visible.
 * - A keyed view that should be removed stays in children with shown=false rather than being
 *   destroyed.
 *
 * The tests below work around this by asserting on children.size (structural membership) for
 * the keyed path and on the shown flag for the unkeyed path (which uses synchronous show/hide).
 */
@OptIn(InternalKiteUi::class)
class RenderListTest {

    /**
     * Like [elementTree] but keeps [Dispatchers.Main] set for the duration of [action] so that
     * reactive updates triggered inside the action propagate synchronously.  Tests that mutate
     * reactive state after the initial tree is built must use this instead of [elementTree].
     */
    @OptIn(ExperimentalCoroutinesApi::class, OverrideOnly::class)
    private fun elementTreeAndRun(
        content: ViewWriter.() -> Unit,
        action: (ElementTreeHandle) -> Unit,
    ) {
        Dispatchers.setMain(Dispatchers.Unconfined)
        try {
            val context = ElementContext("/")
            val root = Frame(context)
            root.onStartup()
            root.themeChoice = ThemeDerivation.SetAsBase(ElementTestTheme)
            with(root) { content() }
            val handle = ElementTreeHandle(root, context)
            action(handle)
        } finally {
            Dispatchers.resetMain()
        }
    }

    // -----------------------------------------------------------------------
    // 1. Unkeyed simple path (placeholders=0): N items → N children
    // -----------------------------------------------------------------------

    @Test
    fun unkeyed_addsNChildrenForNItems() {
        val items = Signal(listOf("a", "b", "c"))
        val tree = elementTree {
            col {
                debugName = "list"
                renderList(items) { text { ::content { it() } } }
            }
        }
        try {
            val listCol = tree.findByName("list")!! as ContainerElement
            assertEquals(3, listCol.children.size,
                "renderList should create exactly 3 children for a 3-item list")
        } finally {
            tree.shutdown()
        }
    }

    @Test
    fun unkeyed_emptyList_noChildren() {
        val items = Signal(emptyList<String>())
        val tree = elementTree {
            col {
                debugName = "list"
                renderList(items) { text { ::content { it() } } }
            }
        }
        try {
            val listCol = tree.findByName("list")!! as ContainerElement
            assertEquals(0, listCol.children.size,
                "renderList with empty list should have 0 children")
        } finally {
            tree.shutdown()
        }
    }

    @Test
    fun unkeyed_growsAndShrinksChildCount() {
        // Reactive updates after elementTree() returns need Main set; use elementTreeAndRun.
        val items = Signal(listOf("a", "b", "c"))
        elementTreeAndRun(
            content = {
                col {
                    debugName = "list"
                    renderList(items) { text { ::content { it() } } }
                }
            }
        ) { tree ->
            val listCol = tree.findByName("list")!! as ContainerElement
            assertEquals(3, listCol.children.size, "Initial: 3 children")

            // Grow
            items.value = listOf("a", "b", "c", "d", "e")
            assertEquals(5, listCol.children.size, "After grow: 5 children")

            // Shrink — simple path rebuilds, so exactly 2 children remain
            items.value = listOf("a", "b")
            assertEquals(2, listCol.children.size, "After shrink: 2 children")

            tree.shutdown()
        }
    }

    // -----------------------------------------------------------------------
    // 2. Unkeyed positional pool: grow / shrink / poolCap eviction
    //
    //    The positional path uses synchronous shown=true/false (no afterTimeout),
    //    so visibility assertions work correctly in SSR.
    // -----------------------------------------------------------------------

    @Test
    fun unkeyedPool_growThenShrink_visibilityCorrect() {
        val items = Signal(listOf("x", "y", "z"))
        elementTreeAndRun(
            content = {
                col {
                    debugName = "list"
                    renderList(items, placeholders = 3) { text { ::content { it() } } }
                }
            }
        ) { tree ->
            val listCol = tree.findByName("list")!! as ContainerElement
            assertEquals(3, listCol.children.count { it.shown },
                "Initially 3 visible slots")

            // Grow to 5
            items.value = listOf("x", "y", "z", "a", "b")
            assertEquals(5, listCol.children.count { it.shown },
                "After grow to 5: 5 visible")
            assertEquals(5, listCol.children.size,
                "5 total slots after grow")

            // Shrink back to 2 — slots are hidden (not removed)
            items.value = listOf("x", "y")
            assertEquals(2, listCol.children.count { it.shown },
                "After shrink to 2: 2 visible")
            assertEquals(5, listCol.children.size,
                "Pool retains 5 total slots (3 hidden)")
            assertEquals(3, listCol.children.count { !it.shown },
                "3 hidden slots in pool")

            tree.shutdown()
        }
    }

    @Test
    fun unkeyedPool_poolCapEviction_removesExcessSlots() {
        // poolCap=2: after a large shrink, the pool is trimmed so at most
        // currentSize + 2 slots remain.
        val cap = 2
        val items = Signal((1..10).map { "item$it" })
        elementTreeAndRun(
            content = {
                col {
                    debugName = "list"
                    renderList(items, placeholders = 5, poolCap = cap) {
                        text { ::content { it() } }
                    }
                }
            }
        ) { tree ->
            val listCol = tree.findByName("list")!! as ContainerElement
            assertEquals(10, listCol.children.count { it.shown },
                "Initially all 10 visible")

            // Shrink to 1 item — with poolCap=2 the pool should be evicted down
            // to at most 1 + 2 = 3 total slots.
            items.value = listOf("item1")
            val totalSlots = listCol.children.size
            val maxAllowed = 1 + cap
            assertTrue(totalSlots <= maxAllowed,
                "After shrink with poolCap=$cap: $totalSlots slots, expected <= $maxAllowed")
            assertEquals(1, listCol.children.count { it.shown },
                "Exactly 1 visible slot")

            tree.shutdown()
        }
    }

    // -----------------------------------------------------------------------
    // 3. Keyed path: structural membership assertions
    //
    //    afterTimeout is a no-op in JVM SSR, so:
    //    - Newly created views are born with shown=false and never become visible.
    //    - Views scheduled for removal stay in children (removal deferred).
    //    We test children.size instead of visibility.
    // -----------------------------------------------------------------------

    private data class Item(val id: Int, val name: String)

    @Test
    fun keyed_initialRenderCreatesNChildren() {
        val items = Signal(listOf(Item(1, "Alpha"), Item(2, "Beta"), Item(3, "Gamma")))
        val tree = elementTree {
            col {
                debugName = "list"
                renderList(items, id = { it.id }, animate = false) { reactive ->
                    text { ::content { reactive().name } }
                }
            }
        }
        try {
            val listCol = tree.findByName("list")!! as ContainerElement
            assertEquals(3, listCol.children.size,
                "Keyed renderList should create 3 child elements for 3 items")
        } finally {
            tree.shutdown()
        }
    }

    @Test
    fun keyed_addItems_increasesChildrenSize() {
        val items = Signal(listOf(Item(1, "Alpha")))
        elementTreeAndRun(
            content = {
                col {
                    debugName = "list"
                    renderList(items, id = { it.id }, animate = false) { reactive ->
                        text { ::content { reactive().name } }
                    }
                }
            }
        ) { tree ->
            val listCol = tree.findByName("list")!! as ContainerElement
            assertEquals(1, listCol.children.size, "Initial: 1 child")

            // Add two more items — new views are created and appended
            items.value = listOf(Item(1, "Alpha"), Item(2, "Beta"), Item(3, "Gamma"))
            assertEquals(3, listCol.children.size,
                "After adding 2 items: 3 children in the element tree")

            tree.shutdown()
        }
    }

    @Test
    fun keyed_removeItem_retainsChildInTree() {
        // In SSR, afterTimeout for removal is a no-op, so the removed view stays in children.
        // The no-animation keyed path does NOT use shownWhen, so element.shown is not managed
        // via the internal shown Signal — elements default to shown=true and stay that way.
        val all = listOf(Item(1, "Alpha"), Item(2, "Beta"), Item(3, "Gamma"))
        val items = Signal(all)
        elementTreeAndRun(
            content = {
                col {
                    debugName = "list"
                    renderList(items, id = { it.id }, animate = false) { reactive ->
                        text { ::content { reactive().name } }
                    }
                }
            }
        ) { tree ->
            val listCol = tree.findByName("list")!! as ContainerElement
            assertEquals(3, listCol.children.size, "Initial: 3 children")

            // Remove id=2 — the view is scheduled for removal via afterTimeout (no-op in SSR),
            // so it stays in the children list.
            items.value = all.filter { it.id != 2 }

            assertEquals(3, listCol.children.size,
                "Removed view stays in tree (afterTimeout is no-op in SSR)")

            tree.shutdown()
        }
    }
}
