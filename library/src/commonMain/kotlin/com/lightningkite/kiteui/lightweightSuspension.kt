@file:OptIn(DelicateCoroutinesApi::class)

package com.lightningkite.kiteui

import com.lightningkite.reactive.context.StatusListener
import com.lightningkite.reactive.core.RawReactive
import com.lightningkite.reactive.core.Release
import com.lightningkite.reactive.core.reactiveState
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@OptIn(ExperimentalStdlibApi::class)
public fun CoroutineScope.load(context: CoroutineContext = EmptyCoroutineContext, action: suspend () -> Unit): Job {
    val state = RawReactive<Unit>()
    val result = launch(
        context,
        block = {
            val r = reactiveState { action() }
            state.state = r
        },
        start = if (coroutineContext[CoroutineDispatcher.Key]?.isDispatchNeeded(
                coroutineContext
            ) == false
        ) CoroutineStart.UNDISPATCHED else CoroutineStart.DEFAULT
    )
    coroutineContext.plus(context)[StatusListener]?.watchBackgroundProcess(state)
    return result
}
