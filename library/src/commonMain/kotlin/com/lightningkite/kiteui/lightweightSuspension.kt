@file:OptIn(DelicateCoroutinesApi::class)

package com.lightningkite.kiteui

import com.lightningkite.kiteui.reactive.CalculationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlin.coroutines.*
import kotlin.random.Random
import kotlin.time.Duration

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

suspend fun <T> async(action: suspend () -> T): Async<T> = CoroutineScope(coroutineContext).async(block = { action() })
//fun <T> CoroutineScope.async(action: suspend () -> T): Async<T> = async(block = { action() })
fun <T> asyncGlobal(action: suspend () -> T): Async<T> = GlobalScope.async(block = { action() })

fun launchGlobal(action: suspend () -> Unit) = GlobalScope.launch(block = { action() })

@OptIn(ExperimentalStdlibApi::class)
fun CalculationContext.launch(action: suspend () -> Unit): Job {
//    var id = Random.nextInt()
    var justStarted = true
    var done = false
//    println("$id will start")
    val result = launch(
        block = {
//            println("$id launched")
            action()
//            println("$id complete")
            if (!justStarted) notifyLongComplete(Result.success(Unit))
            else done = true
        },
        start = if (coroutineContext[CoroutineDispatcher.Key]?.isDispatchNeeded(
                coroutineContext
            ) == false
        ) CoroutineStart.UNDISPATCHED else CoroutineStart.DEFAULT
    )
//    println("$id started")
    justStarted = false
    if (!done) {
        notifyStart()
    }
    return result
}

@OptIn(ExperimentalStdlibApi::class)
fun CalculationContext.launchManualCancel(action: suspend () -> Unit): Job {
//    var id = Random.nextInt()
    var justStarted = true
    var done = false
//    println("$id will start")
    val result = (if(requireMainThread) GlobalScope + Dispatchers.Main else GlobalScope + Dispatchers.Default).launch(
        block = {
//            println("$id launched")
            action()
//            println("$id complete")
            if (!justStarted) notifyLongComplete(Result.success(Unit))
            else done = true
        },
        start = if (coroutineContext[CoroutineDispatcher.Key]?.isDispatchNeeded(
                coroutineContext
            ) == false
        ) CoroutineStart.UNDISPATCHED else CoroutineStart.DEFAULT
    )
//    println("$id started")
    justStarted = false
    if (!done) {
        notifyStart()
    }
    return result
}
