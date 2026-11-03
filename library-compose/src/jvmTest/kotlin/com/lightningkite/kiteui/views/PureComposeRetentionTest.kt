package com.lightningkite.kiteui.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import java.lang.ref.WeakReference
import kotlin.test.Ignore
import kotlin.test.Test

/**
 * Compose UI 1.12.1, without KiteUI: a removed node's modifiers stay reachable from a sibling's
 * `NodeChain.cachedDiffer`, whose `before` list was returned to the shared buffer pool and then became the removed
 * node's modifier list.  Fails on 1.12.1; run it after upgrading Compose (see the plan's known issues).
 */
@OptIn(ExperimentalTestApi::class)
class PureComposeRetentionTest {
    class Big

    @Ignore
    @Test
    fun removedNodesModifiersAreCollected() = runComposeUiTest {
        var big: Big? = Big()
        val ref = WeakReference(big)
        var showB by mutableStateOf(true)
        var structureA by mutableStateOf(false)
        var sizeB by mutableIntStateOf(10)
        setContent {
            Column {
                Box(Modifier.size(10.dp).then(if (structureA) Modifier.padding(1.dp) else Modifier))
                if (showB) Box(Modifier.layoutId(big!!).size(sizeB.dp))
            }
        }
        waitForIdle()
        // A's modifiers change shape: its differ keeps the list it had, which goes back to the pool.
        structureA = true
        waitForIdle()
        // B's modifiers change: B takes that list from the pool.
        sizeB++
        waitForIdle()
        // B goes, and its list with it, except that A's differ still points at it.
        showB = false
        big = null
        waitForIdle()
        repeat(10) {
            if (ref.get() == null) return@runComposeUiTest
            System.gc()
            Thread.sleep(50)
        }
        throw AssertionError("A removed node's modifier is still reachable")
    }
}
