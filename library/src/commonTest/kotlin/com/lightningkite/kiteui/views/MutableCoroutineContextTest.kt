package com.lightningkite.kiteui.views

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MutableCoroutineContextTest {
    @Test
    fun laterElementsReplaceEarlierOnes() {
        val context = coroutineContextOf(CoroutineName("first"), Dispatchers.Unconfined) + CoroutineName("second")
        assertEquals(listOf("second"), context.fold(listOf<String>()) { names, e -> if (e is CoroutineName) names + e.name else names })
        assertEquals("second", context[CoroutineName]?.name)
    }

    // An element's scope is built on this context; launching in it adds the coroutine's own Job.
    @Test
    fun flowsCollectInCoroutinesLaunchedOnIt() = runTest {
        val scope = CoroutineScope(coroutineContextOf(SupervisorJob(), Dispatchers.Unconfined))
        var collected: List<Int>? = null
        scope.launch { collected = flowOf(1, 2).toList() }.join()
        assertEquals(listOf(1, 2), collected)
        scope.coroutineContext[Job]!!.cancel()
    }
}
