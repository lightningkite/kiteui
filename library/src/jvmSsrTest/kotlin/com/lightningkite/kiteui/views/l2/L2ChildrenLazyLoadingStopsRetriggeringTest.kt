package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.ElementTestTheme
import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.direct.Frame
import com.lightningkite.kiteui.views.direct.ScrollingBehaviors
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Fake [ScrollingBehaviors] whose viewport/content rects are test-controlled signals, so the
 * "near the end of scroll" condition inside [childrenLazyLoading] can be driven directly without
 * a live scrolling viewport.
 */
private class FakeScrollingBehaviors : ScrollingBehaviors {
    override val horizontal: Boolean = false
    override val vertical: Boolean = true
    override var showScrollBars: Boolean = false
    val viewportSignal = Signal(Rect(0.0, 0.0, 100.0, 100.0))
    val contentSignal = Signal(Rect(0.0, 0.0, 100.0, 1000.0))
    override val viewport: Reactive<Rect> get() = viewportSignal
    override val content: Reactive<Rect> get() = contentSignal
    override val directlyInteractingWithScroller: Reactive<Boolean> = Signal(false)
    override var snapToElements: Pair<Align?, Align?> = null to null
    override var scrollSnapStop: Boolean = false
    override var ignoreInteraction: Boolean = false
    override fun scrollTo(left: Double, top: Double, animated: Boolean) {}
    override fun scrollTo(element: Element, horizontal: Align, vertical: Align, animated: Boolean) {}
    override fun scrollToKeepAnimations(x: Double, y: Double) {}
}

/**
 * Regression test for the guard added to [childrenLazyLoading] in commit 94f1ebe6b:
 * "If the last completed load didn't grow the list, don't trigger another one until something
 * changes." Before the fix, once a load job finished (`loadJob` reset to null), any further
 * reactive recomputation - e.g. a scroll/content size change unrelated to the list - re-entered
 * the "near the end" branch and called [loadMore] again even though the list never grew,
 * because only `loadJob != null` guarded re-entry.
 */
class L2ChildrenLazyLoadingStopsRetriggeringTest {

    @OptIn(InternalKiteUi::class, ExperimentalCoroutinesApi::class, OverrideOnly::class)
    @Test
    fun loadMoreIsNotRetriggeredWhileListSizeIsUnchanged() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        try {
            val context = ElementContext("/")
            val root = Frame(context)
            root.onStartup()
            root.themeChoice = ThemeDerivation.SetAsBase(ElementTestTheme)

            val items = Signal(listOf(1, 2, 3))
            val scroll = FakeScrollingBehaviors()
            var loadCount = 0

            with(root) {
                col {
                    @Suppress("DEPRECATION")
                    childrenLazyLoading(
                        scroll = scroll,
                        items = items,
                        id = { it },
                        loadMore = { loadCount++ }, // never grows `items` - simulates "no more data"
                    ) { reactive ->
                        text { ::content { reactive().toString() } }
                    }
                }
            }
            assertEquals(0, loadCount, "far from the end initially, no load should have fired yet")

            // Move content close to the viewport end - this is the only thing that should trigger
            // the first load.
            scroll.contentSignal.value = Rect(0.0, 0.0, 100.0, 105.0)
            Thread.sleep(250) // let the async load job (which itself awaits a 0.1s delay) settle
            assertEquals(1, loadCount, "first load should fire once when scrolled near the end")

            // Perturb content again without the item count changing. Pre-fix, this would re-enter
            // the "near the end" branch and call loadMore a second time.
            scroll.contentSignal.value = Rect(0.0, 0.0, 100.0, 106.0)
            Thread.sleep(250)
            assertEquals(1, loadCount, "should not retrigger loadMore while the list size is unchanged")

            root.onShutdown()
        } finally {
            Dispatchers.resetMain()
        }
    }
}
