@file:OptIn(DelicateCoroutinesApi::class)

package com.lightningkite.kiteui

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.coroutines.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalStdlibApi::class)
fun CoroutineScope.load(context: CoroutineContext = EmptyCoroutineContext, action: suspend () -> Unit): Job {
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
    coroutineContext[StatusListener]?.watchBackgroundProcess(state)
    return result
}
