@file:OptIn(DelicateCoroutinesApi::class)

package com.lightningkite.kiteui

import com.lightningkite.kiteui.reactive.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlin.coroutines.*

typealias CancelledException = CancellationException

suspend fun <T> suspendCoroutineCancellable(start: (Continuation<T>) -> () -> Unit): T {
    return suspendCancellableCoroutine<T> {
        val canceller = start(it)
        it.invokeOnCancellation { canceller() }
    }
}

//suspend fun delay(duration: Duration): Unit = kotlinx.coroutines.delay(duration)
//suspend fun delay(milliseconds: Long): Unit = kotlinx.coroutines.delay(milliseconds)

suspend fun <T> race(vararg actions: suspend () -> T): T {
    return channelFlow {
        for (race in actions) {
            launch { send(race()) }
        }
    }.first()
}

typealias TimeoutException = kotlinx.coroutines.TimeoutCancellationException

suspend fun <T> timeout(milliseconds: Long, action: suspend () -> T): T =
    kotlinx.coroutines.withTimeout(milliseconds, { action() })

suspend fun <T> timeoutOrNull(milliseconds: Long, action: suspend () -> T): T? =
    kotlinx.coroutines.withTimeoutOrNull(milliseconds, { action() })

//fun CoroutineContext.cancel() = cancel()
suspend fun stopIfCancelled() = yield()

typealias Async<T> = kotlinx.coroutines.Deferred<T>

val AppJob = SupervisorJob()
val AppScope = CoroutineScope(AppJob + CoroutineExceptionHandler { coroutineContext, throwable ->
    throwable.report("AppScope")
} + Dispatchers.Main.immediate)

suspend fun <T> async(action: suspend () -> T): Async<T> = CoroutineScope(coroutineContext).async(block = { action() })
//fun <T> CoroutineScope.async(action: suspend () -> T): Async<T> = async(block = { action() })
fun <T> asyncGlobal(action: suspend () -> T): Async<T> = AppScope.async(block = { action() })

fun launchGlobal(action: suspend () -> Unit) = AppScope.launch(block = { action() })

@OptIn(ExperimentalStdlibApi::class)
fun CoroutineScope.load(context: CoroutineContext = EmptyCoroutineContext, action: suspend () -> Unit): Job {
    val state = RawReadable<Unit>()
    val result = launch(
        context,
        block = {
            val r = ReadableState { action() }
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
