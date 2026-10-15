package com.lightningkite.kiteui.views

import androidx.compose.ui.test.*
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.lang.ref.WeakReference
import kotlin.test.Test
import kotlin.test.assertFailsWith

@OptIn(ExperimentalTestApi::class)
class LeakTest {
    @Test
    fun swappedOutElementsAreCollected() = runKiteUiTest {
        val page = Signal(0)
        val root = showKiteUi {
            swapView {
                swapping(current = { page() }) {
                    scrolling.col {
                        text("Page $it")
                        button { text("Click"); onClick { } }
                        textInput { }
                    }
                }
            }
        }

        val removed = ArrayList<WeakReference<Element>>()
        fun collect(element: Element) {
            removed += WeakReference(element)
            (element as? ElementWithChildren)?.children?.forEach { collect(it) }
        }
        (root.root as SwapView).children.forEach { collect(it) }

        runBlocking(Dispatchers.Main) { page.value = 1 }
        waitForIdle()
        onNodeWithText("Page 1").assertExists()

        repeat(10) {
            if (removed.all { it.get() == null }) return@runKiteUiTest
            System.gc()
            Thread.sleep(50)
        }
        val leaked = removed.mapNotNull { it.get() }
        throw AssertionError("${leaked.size} of ${removed.size} swapped-out elements are still reachable: $leaked")
    }

    // Compose 1.12.1 can keep a removed node's modifiers, and so its element, for a while (see the plan's known
    // issues).  What the element holds must not keep that composition's layout nodes too: through them, every
    // page the app had shown stayed reachable.
    @Test
    fun removedElementsLetGoOfTheirLayout() = runKiteUiTest {
        val page = Signal(0)
        lateinit var first: NativeElement
        showKiteUi {
            swapView {
                swapping(current = { page() }) {
                    col {
                        if (it == 0) first = this
                        text("Page $it")
                    }
                }
            }
        }
        val layout = WeakReference(checkNotNull(first.coordinates))

        runBlocking(Dispatchers.Main) { page.value = 1 }
        waitForIdle()
        onNodeWithText("Page 1").assertExists()

        repeat(10) {
            if (layout.get() == null) return@runKiteUiTest
            System.gc()
            Thread.sleep(50)
        }
        throw AssertionError("A removed element still holds its layout: $first")
    }

    @Test
    fun programmaticLayoutPassesEndWithThePass() = runKiteUiTest {
        var kept: ProgrammingLayoutInProgress? = null
        lateinit var child: Element
        showKiteUi {
            programmatic {
                // Recycler2 keeps the pass it was given, like this.
                delegate = object : ProgrammaticLayoutDelegate {
                    override fun measure(layout: ProgrammaticLayout, inProgress: ProgrammingLayoutInProgress, within: Size): Size {
                        kept = inProgress
                        return within
                    }
                    override fun layout(layout: ProgrammaticLayout, inProgress: ProgrammingLayoutInProgress, within: Size) =
                        ProgrammaticLayoutDelegate.AllFull.layout(layout, inProgress, within)
                }
                child = text("Child")
            }
        }
        // It no longer reaches the pass's layout nodes, so it can't measure.
        assertFailsWith<IllegalStateException> { checkNotNull(kept).measure(child, Size(100.0, 100.0)) }
    }
}
