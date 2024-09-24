package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.coroutines.*

class SuspendingReactiveContext(
    val calculationContext: CalculationContext,
    var action: suspend () -> Unit,
    var onLoad: (() -> Unit)? = null,
    val debug: Console? = null
) : CoroutineContext.Element {
    internal val removers: HashMap<Any?, () -> Unit> = HashMap()
    internal val lastValue: HashMap<Any?, Any?> = HashMap()
    internal val latestPass: ArrayList<Any?> = ArrayList()
    override val key: CoroutineContext.Key<SuspendingReactiveContext> = Key
    internal var lastJob: Job? = null
    val listener = calculationContext.coroutineContext[StatusListener]

    internal fun setLoading() {
        listener?.report(this, ReadableState.notReady, false)
    }

    private var executionInstance = 0

    @OptIn(ExperimentalStdlibApi::class)
    internal fun run() {
        var notReadyReported = false
        latestPass.clear()

        debug?.log("Calculating")
        var done = false
        var index = ++executionInstance
        lastJob = calculationContext.launch(
            this,
            start = if (calculationContext.coroutineContext[CoroutineDispatcher.Key]?.isDispatchNeeded(
                    calculationContext.coroutineContext
                ) == false
            ) CoroutineStart.UNDISPATCHED else CoroutineStart.DEFAULT
        ) {
            val result = readableState { action() }
            if (index != executionInstance) return@launch
            done = true
            debug?.log("Complete, got result $result")
            listener?.report(this, result, !notReadyReported)
            for (entry in removers.entries.toList()) {
                if (entry.key !in latestPass) {
                    entry.value()
                    removers.remove(entry.key)
                    lastValue.remove(entry.key)
                }
            }
        }

        if (!done) {
            // start load
            debug?.log("Load started")
            listener?.report(this, ReadableState.notReady, false)
            notReadyReported = true
        }
    }

    internal fun shutdown() {
        action = {}
        onLoad = {}
        removers.forEach { it.value() }
        removers.clear()
        lastValue.clear()
        lastJob?.let {
            lastJob = null
            it.cancel()
        }
        latestPass.clear()
    }

    init {
        run()
        calculationContext.onRemove {
            shutdown()
        }
    }

    object Key : CoroutineContext.Key<SuspendingReactiveContext> {
        init {
//            println("ReactiveScopeData V7")
        }
    }
}


@Suppress("NOTHING_TO_INLINE") inline fun CalculationContext.reactiveSuspending(noinline action: suspend () -> Unit) = reactiveSuspending(null, action)
@Suppress("NOTHING_TO_INLINE") inline fun CalculationContext.reactiveSuspending(noinline onLoad: (() -> Unit)?, noinline action: suspend () -> Unit) {
    SuspendingReactiveContext(this, action, onLoad)
}

private fun <T> Continuation<T>.resumeState(state: ReadableState<T>) {
    state.handle(
        success = { resume(it) },
        exception = { resumeWithException(it) },
        notReady = { resumeWithException(CancellationException("State not ready")) }
    )
}

suspend fun rerunOn(listenable: Listenable) {
    coroutineContext[SuspendingReactiveContext.Key]?.let {
        if (!it.removers.containsKey(listenable)) {
            it.removers[listenable] = listenable.addListener {
                it.run()
            }
        }
        it.latestPass.add(listenable)
    }
}

suspend inline operator fun <T> Readable<T>.invoke(): T = await()
suspend inline operator fun <T> ImmediateReadable<T>.invoke(): T = await()
suspend inline fun <T> Readable<T>.exception(): Exception? = state { it.exception }

suspend fun <T, V> Readable<T>.state(get: (ReadableState<T>) -> V): V {
    coroutineContext[SuspendingReactiveContext.Key]?.let {
        // and the value is ready to go, just add the listener and proceed with the value.
        var last = state.let(get)
        if (!it.removers.containsKey(this)) {
            it.debug?.log("adding listener to $this")
            it.removers[this] = this.addListener {
                val newVal = state.let(get)
                if (last != newVal) {
                    last = newVal
                    it.run()
                }
            }
            // Repull in case of activation
            last = state.let(get)
        } else {
            it.debug?.log("already depends on $this")
        }
        it.latestPass.add(this)
        return last
    } ?: return state.let(get)
}

suspend fun <T> Readable<T>.state(): ReadableState<T> {
    coroutineContext[SuspendingReactiveContext.Key]?.let {
        // and the value is ready to go, just add the listener and proceed with the value.
        if (!it.removers.containsKey(this)) {
            it.debug?.log("adding listener to $this")
            it.removers[this] = this.addListener {
                it.run()
            }
        } else {
            it.debug?.log("already depends on $this")
        }
        it.latestPass.add(this)
        return state
    } ?: return state
}

suspend fun <T> ImmediateReadable<T>.await(): T {
    coroutineContext[SuspendingReactiveContext.Key]?.let {
        // and the value is ready to go, just add the listener and proceed with the value.
        if (!it.removers.containsKey(this)) {
            it.debug?.log("adding listener to $this")
            it.removers[this] = this.addListener {
                it.run()
            }
        } else {
            it.debug?.log("already depends on $this")
        }
        it.latestPass.add(this)
        return value
    } ?: return value
}

suspend fun <T> Readable<T>.await(): T {
    coroutineContext[SuspendingReactiveContext.Key]?.let {
        var cont: Continuation<T>? = null
        if (!it.removers.containsKey(this)) {
            it.debug?.log("adding listener to $this")
            it.removers[this] = this.addListener {
                it.debug?.log("READABLE LISTENER HIT A")
                state.handle(
                    success = { r ->
                        cont?.let { c ->
                            c.resume(r)
                            cont = null
                        } ?: it.run()
                    },
                    exception = { r ->
                        cont?.let { c ->
                            c.resumeWithException(r)
                            cont = null
                        } ?: it.run()
                    },
                    notReady = {
                        if(cont == null) it.setLoading()
                    }
                )
            }
        } else {
            it.debug?.log("already depends on $this")
        }
        it.latestPass.add(this)

        this.state.handle(
            success = { return@let it },
            exception = { throw it },
            notReady = {
                return@let suspendCancellableCoroutine {
                    cont = it
                }
            }
        )
    }
    return awaitOnce()
}

@Deprecated("Replace with 'awaitOnce'", ReplaceWith("this.awaitOnce()", "com.lightningkite.kiteui.reactive.awaitOnce"))
suspend fun <T> Readable<T>.awaitRaw(): T = awaitOnce()

suspend fun <T> Readable<T>.awaitOnce(): T {
    val state = state
    return if (state.ready) state.get()
    else suspendCoroutineCancellable {
        // If it's not ready, we need to wait until it is then never bother with this again.
        var remover: (() -> Unit)? = null
        var alreadyRun = false
        var done = false
        remover = addAndRunListener {
            val state = this.state
            if (state.ready && !done) {
                done = true
                it.resumeState(state)
                remover?.invoke() ?: run {
                    alreadyRun = true
                }
            }
        }
        if (alreadyRun) remover.invoke()
        return@suspendCoroutineCancellable remover
    }
}

@Deprecated("STAHP")
fun <T> Readable<Readable<T>>.flatten(): Readable<T> {
    val first = shared { this@flatten() }
    return shared { first()() }
}

suspend operator fun <T> Flow<T>.invoke(): T {
    coroutineContext[SuspendingReactiveContext.Key]?.let {
        it.latestPass.add(this@invoke)
        if (it.removers[this@invoke] == null) {
            val job = CoroutineScope(coroutineContext).launch {
                collect { v ->
                    it.lastValue[this@invoke] = v
                    it.run()
                }
            }
            it.removers[this@invoke] = {
                job.cancel()
            }
            return suspendCancellableCoroutine { }
        } else {
            if (it.lastValue.containsKey(this@invoke)) {
                @Suppress("UNCHECKED_CAST")
                return it.lastValue[this@invoke] as T
            } else {
                return suspendCancellableCoroutine { }
            }
        }
    } ?: run {
        return first()
    }
}

suspend operator fun <R> (ReactiveContext.()->R).invoke(): R {
    return shared { this@invoke() }.awaitOnce()
}
suspend operator fun <A, R> (ReactiveContext.(A)->R).invoke(a: A): R {
    return shared { this@invoke(a) }.awaitOnce()
}
suspend operator fun <A, B, R> (ReactiveContext.(A, B)->R).invoke(a: A, b: B): R {
    return shared { this@invoke(a, b) }.awaitOnce()
}
suspend operator fun <A, B, C, R> (ReactiveContext.(A, B, C)->R).invoke(a: A, b: B, c: C): R {
    return shared { this@invoke(a, b, c) }.awaitOnce()
}