package com.lightningkite.kiteui

import com.lightningkite.kiteui.reactive.CalculationContext
import kotlin.coroutines.*
import kotlin.time.Duration

class CancelledException(): Exception()
suspend fun <T> suspendCoroutineCancellable(start: (Continuation<T>)->()->Unit): T {
    stopIfCancelled()
    var canceller: (()->Unit)? = null
    val result: T = try {
        val context = coroutineContext
        suspendCoroutine<T> {
            val c = context[CancellationState.Key]!!
            var terminate: (()->Unit)? = null
            val st = SingleThreadContinuation(it) {
                c.waitingOn = null
                terminate?.invoke()
            }
            c.waitingOn = st
            if(c.debug != null) {
                val e = Exception("Still waiting for this to complete")
                var printer = {}
                printer = {
                    e.printStackTrace2()
                    terminate = afterTimeout(1000L, printer)
                }
                terminate = afterTimeout(1000L, printer)
            }
            canceller = start(st)
        }
    } catch(e: CancelledException) {
        canceller?.invoke()
        throw e
    }
    return result
}

suspend fun delay(duration: Duration): Unit = delay(duration.inWholeMilliseconds)
suspend fun delay(milliseconds: Long): Unit = suspendCoroutineCancellable<Unit> {
    val handle = afterTimeout(milliseconds) { ->
        it.resume(Unit)
    }
    return@suspendCoroutineCancellable {
        handle()
    }
}

private class SingleThreadContinuation<T>(val wraps: Continuation<T>, val onDone: ()->Unit): Continuation<T> {
    var done = false
    override fun resumeWith(result: Result<T>) {
        if(done) return
        done = true
        onDone()
        wraps.resumeWith(result)
    }

    override val context: CoroutineContext
        get() = wraps.context
}

suspend fun <T> race(vararg actions: suspend () -> T): T = suspendCoroutineCancellable { top ->
    val ctx = top.context + CancellationState(false, top.context[CancellationState.Key])
    actions.forEach {
        it.startCoroutine(object: Continuation<T> {
            override val context: CoroutineContext = ctx
            override fun resumeWith(result: Result<T>) {
                top.resumeWith(result)
                ctx.cancel()
            }
        })
    }
    return@suspendCoroutineCancellable {
        ctx.cancel()
    }
}

class TimeoutException(): Exception()
suspend fun <T> timeout(milliseconds: Long, action: suspend () -> T): T {
    return race(
        action,
        {
            delay(milliseconds)
            throw TimeoutException()
        }
    )
}
suspend fun <T> timeoutOrNull(milliseconds: Long, action: suspend () -> T): T? {
    return race(
        action,
        {
            delay(milliseconds)
            null
        }
    )
}

fun CoroutineContext.childCancellation(): CoroutineContext = this + CancellationState(false, this.get(CancellationState.Key))
private class CancellationState(var stop: Boolean, var parent: CancellationState? = null, var waitingOn: Continuation<*>? = null, var waitingDebugException: Exception? = null): CoroutineContext.Element {
    override val key: CoroutineContext.Key<CancellationState> = Key
    val shouldStop: Boolean get() = stop || (parent?.stop ?: false)
    var debug: Console? = null
    object Key: CoroutineContext.Key<CancellationState>
}
fun CoroutineContext.setDebugLog(console: Console) {
    val c = this[CancellationState.Key]!!
    c.debug = console
}
fun CoroutineContext.cancel() {
    val c = this[CancellationState.Key]!!
    if(c.debug != null) CancelledException().printStackTrace2()
    c.stop = true
    c.waitingOn?.resumeWithException(CancelledException())
}
suspend fun stopIfCancelled() {
    val state = coroutineContext[CancellationState.Key]!!
    if(state.shouldStop) {
        throw CancelledException()
    }
}

interface Async<T>: Cancellable {
    suspend fun await(): T
}

suspend fun <T> async(action: suspend ()->T): Async<T> {
    val context: CoroutineContext = coroutineContext.childCancellation()
    var toReturn: Result<T>? = null
    val awaiters = ArrayList<Continuation<T>>()
    action.startCoroutine(object : Continuation<T> {
        override val context: CoroutineContext = context
        // called when a coroutine ends. do nothing.
        override fun resumeWith(result: Result<T>) {
            toReturn = result
            awaiters.forEach { it.resumeWith(result) }
        }
    })
    return object: Async<T> {
        override suspend fun await(): T = suspendCoroutineCancellable<T> {
            val toReturn = toReturn
            if(toReturn != null) {
                it.resumeWith(toReturn)
                return@suspendCoroutineCancellable {}
            } else {
                awaiters.add(it)
                return@suspendCoroutineCancellable {
                    context.cancel()
                }
            }
        }
        override fun cancel() {
            context.cancel()
        }
    }
}

fun <T> asyncGlobal(action: suspend ()->T): Async<T> {
    val context: CoroutineContext = EmptyCoroutineContext.childCancellation()
    var toReturn: Result<T>? = null
    val awaiters = ArrayList<Continuation<T>>()
    action.startCoroutine(object : Continuation<T> {
        override val context: CoroutineContext = context
        // called when a coroutine ends. do nothing.
        override fun resumeWith(result: Result<T>) {
            toReturn = result
            awaiters.forEach { it.resumeWith(result) }
        }
    })
    return object: Async<T> {
        override suspend fun await(): T = suspendCoroutineCancellable<T> {
            val toReturn = toReturn
            if(toReturn != null) {
                it.resumeWith(toReturn)
                return@suspendCoroutineCancellable {}
            } else {
                awaiters.add(it)
                return@suspendCoroutineCancellable {
                    context.cancel()
                }
            }
        }
        override fun cancel() {
            context.cancel()
        }
    }
}

interface Cancellable {
    fun cancel()
}

fun launchGlobal(action: suspend () -> Unit): Cancellable {
    val context: CoroutineContext = EmptyCoroutineContext.childCancellation()
    action.startCoroutine(object : Continuation<Unit> {
        override val context: CoroutineContext = context
        // called when a coroutine ends. do nothing.
        override fun resumeWith(result: Result<Unit>) {
            result.onFailure { ex : Throwable ->
                if(ex !is CancelledException) {
                    ConsoleRoot.error("launchGlobal $action experienced an exception:")
                    ex.printStackTrace2()
                }
            }
        }
    })
    return object: Cancellable {
        override fun cancel() {
            context.cancel()
        }
    }
}

fun CalculationContext.launch(action: suspend () -> Unit) {
    val context: CoroutineContext = EmptyCoroutineContext.childCancellation()
    var done = false
    var loadStarted = false
    action.startCoroutine(object : Continuation<Unit> {
        override val context: CoroutineContext = context
        // called when a coroutine ends. do nothing.
        override fun resumeWith(result: Result<Unit>) {
            done = true
            if (loadStarted) {
                notifyLongComplete(result)
            } else {
                notifyComplete(result)
            }
        }
    })
    if(!done) {
        // start load
        loadStarted = true
        notifyStart()
    }
    this.onRemove { context.cancel() }
}

fun CalculationContext.launchManualCancel(action: suspend () -> Unit): Cancellable {
    val context: CoroutineContext = EmptyCoroutineContext.childCancellation()
    var done = false
    var loadStarted = false
    action.startCoroutine(object : Continuation<Unit> {
        override val context: CoroutineContext = context
        // called when a coroutine ends. do nothing.
        override fun resumeWith(result: Result<Unit>) {
            done = true
            if (loadStarted) {
                notifyLongComplete(result)
            } else {
                notifyComplete(result)
            }
        }
    })
    if(!done) {
        // start load
        loadStarted = true
        notifyStart()
    }
    return object: Cancellable {
        override fun cancel() {
            context.cancel()
        }
    }
}
