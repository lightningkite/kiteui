@file:OptIn(ExperimentalCoroutinesApi::class)

package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Scroll positioning has to be inert during server-side rendering, not fatal.
 *
 * SSR emits markup once and has no scroll position to move, so all four positioning calls are
 * no-ops. [ScrollingBehaviors.scrollToKeepAnimations] was the exception: it threw. That matters
 * more than it looks, because the recycler calls it from inside its layout pass
 * (`Recycler2.kt`), so the crash took down any server-rendered page containing a recycler -
 * a whole page lost, not a scroll position.
 */
class ScrollViewSsrNoOpTest {
    init {
        Dispatchers.setMain(Dispatchers.IO)
    }

    private fun scroller(build: ViewWriter.() -> Unit = {}): ScrollingBehaviors {
        val writer = Frame(ElementContext("/"))
        lateinit var behaviors: ScrollingBehaviors
        with(writer) {
            col {
                sizeConstraints(height = 12.rem).scrolling { behaviors = this }.col { build() }
            }
        }
        return behaviors
    }

    @Test
    fun everyScrollPositioningCallIsInert() {
        val scroll = scroller { text("content") }

        // None of these may throw. Asserting "returns normally" is the entire contract here -
        // there is no observable scroll position on a server to assert against.
        scroll.scrollTo(10.0, 20.0, animated = false)
        scroll.scrollTo(10.0, 20.0, animated = true)
        scroll.scrollToKeepAnimations(10.0, 20.0)
        scroll.disableScrollAnchoring()

        assertNotNull(scroll)
    }

    @Test
    fun scrollToKeepAnimationsSurvivesRepeatedAndExtremeValues() {
        val scroll = scroller()

        // The recycler drives this from its layout pass with whatever offsets it computed, which
        // includes negatives and zero on the first pass before anything has been measured.
        scroll.scrollToKeepAnimations(0.0, 0.0)
        scroll.scrollToKeepAnimations(-50.0, -50.0)
        scroll.scrollToKeepAnimations(Double.MAX_VALUE, Double.MAX_VALUE)
        repeat(100) { scroll.scrollToKeepAnimations(it.toDouble(), it.toDouble()) }

        assertNotNull(scroll)
    }
}
