@file:OptIn(DelicateCoroutinesApi::class)

package com.lightningkite.kiteui

import com.lightningkite.signal.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlin.coroutines.*

@OptIn(ExperimentalStdlibApi::class)
public fun CoroutineScope.load(context: CoroutineContext = EmptyCoroutineContext, action: suspend () -> Unit): Job {
    val state = RawReadable<Unit>()
    val result = launch(
        context,
        block = {
            val r = readableState { action() }
            state.state = r
        },
        start = if (coroutineContext[CoroutineDispatcher.Key]?.isDispatchNeeded(
                coroutineContext
            ) == false
        ) CoroutineStart.UNDISPATCHED else CoroutineStart.DEFAULT
    )
    coroutineContext[StatusListener]?.loading(state)
    return result
}
