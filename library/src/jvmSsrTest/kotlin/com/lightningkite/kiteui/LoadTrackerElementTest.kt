package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.loadTracker
import com.lightningkite.kiteui.views.withLoadTracking
import com.lightningkite.reactive.core.LateInitSignal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class LoadTrackerElementTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun aTrackedElementLoadsWhileDataInsideItLoads() {
        val data = LateInitSignal<String>()
        lateinit var section: com.lightningkite.kiteui.views.Element
        val tree = elementTree { section = withLoadTracking().col { text { ::content { data() } } } }
        // Claude: elementTree only provides a main dispatcher while it builds, and the data arrives afterwards.
        Dispatchers.setMain(Dispatchers.Unconfined)
        try {
            assertFalse(section.context.loadTracker.state.ready)
            data.valueSet("loaded")
            assertTrue(section.context.loadTracker.state.ready)
        } finally {
            tree.shutdown()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun theTrackedElementItselfAndTheRootReport() {
        lateinit var leaf: com.lightningkite.kiteui.views.Element
        val tree = elementTree { leaf = withLoadTracking().text { ::content { LateInitSignal<String>()() } } }
        assertFalse(leaf.context.loadTracker.state.ready)
        assertNotSame(tree.context.loadTracker, leaf.context.loadTracker)
        assertFalse(tree.context.loadTracker.state.ready, "the root tracker covers the whole tree")
        tree.shutdown()
        assertTrue(tree.context.loadTracker.state.ready, "removing a loading element finishes its load")
    }

    @Test
    fun siblingsAreNotCovered() {
        lateinit var tracked: com.lightningkite.kiteui.views.Element
        val tree = elementTree {
            tracked = withLoadTracking().text { }
            text { ::content { LateInitSignal<String>()() } }
        }
        assertTrue(tracked.context.loadTracker.state.ready)
        tree.shutdown()
    }

    @Test
    fun applyingTwiceNestsTrackers() {
        lateinit var leaf: com.lightningkite.kiteui.views.Element
        val tree = elementTree { leaf = withLoadTracking().withLoadTracking().text { ::content { LateInitSignal<String>()() } } }
        assertFalse(leaf.context.loadTracker.state.ready)
        assertFalse(tree.context.loadTracker.state.ready)
        tree.shutdown()
    }
}
