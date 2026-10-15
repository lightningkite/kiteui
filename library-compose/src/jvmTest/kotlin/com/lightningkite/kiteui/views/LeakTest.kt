package com.lightningkite.kiteui.views

import androidx.compose.ui.test.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.lang.ref.WeakReference
import kotlin.test.Test

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
}
