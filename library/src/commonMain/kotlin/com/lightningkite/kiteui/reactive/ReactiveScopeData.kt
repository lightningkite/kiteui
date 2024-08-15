package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.coroutines.*

class ReactiveScopeData(
    val calculationContext: CalculationContext,
    var action: suspend () -> Unit,
    var onLoad: (() -> Unit)? = null,
    val debug: Console? = null
) : CoroutineContext.Element {
    internal val removers: HashMap<Any?, () -> Unit> = HashMap()
    internal val lastValue: HashMap<Any?, Any?> = HashMap()
    internal val latestPass: ArrayList<Any?> = ArrayList()
    override val key: CoroutineContext.Key<ReactiveScopeData> = Key
    internal var lastJob: Job? = null

    var runningLong: Result<Unit>? = Result.success(Unit)
        set(value) {
            val previous = field
            field = value
            if ((previous == null) != (value == null)) {
                if (value != null) {
                    calculationContext.notifyLongComplete(value)
                } else {
//                    try {
                    onLoad?.invoke()
//                    } catch(e: Exception) {
//                        e.printStackTrace2()
//                    }
                    calculationContext.notifyStart()
                }
            } else if (value != null) {
                calculationContext.notifyComplete(value)
            }
        }

    internal fun setLoading() {
//        println("Skip calc attempt $this")
        runningLong = null
    }

    private var executionInstance = 0
    internal fun run() {
        latestPass.clear()

        debug?.log("Calculating")
        var done = false
        var index = ++executionInstance
        lastJob = calculationContext.launch(this) {
            val result = kotlin.runCatching { action() }
            if (index != executionInstance) return@launch
            done = true
            debug?.log("Complete, got result $result")
            runningLong = result
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
            runningLong = null
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

    object Key : CoroutineContext.Key<ReactiveScopeData> {
        init {
//            println("ReactiveScopeData V7")
        }
    }
}


inline fun CalculationContext.reactiveScope(noinline action: suspend () -> Unit) = reactiveScope(null, action)
inline fun CalculationContext.reactiveScope(noinline onLoad: (() -> Unit)?, noinline action: suspend () -> Unit) {
    ReactiveScopeData(this, action, onLoad)
}

inline fun <T> Continuation<T>.resumeState(state: ReadableState<T>) {
    state.exception?.let { resumeWithException(it) } ?: resume(state.get())
}

suspend fun rerunOn(listenable: Listenable) {
    coroutineContext[ReactiveScopeData.Key]?.let {
        if (!it.removers.containsKey(listenable)) {
            it.removers[listenable] = listenable.addListener {
                it.run()
            }
        }
        it.latestPass.add(listenable)
    }
}
suspend inline operator fun <T> Readable<T>.invoke(): T = await()
suspend inline fun <T> Readable<T>.exception(): Exception? = state { it.exception }

suspend fun <T, V> Readable<T>.state(get: (ReadableState<T>) -> V): V {
    coroutineContext[ReactiveScopeData.Key]?.let {
        // and the value is ready to go, just add the listener and proceed with the value.
        val s = state.let(get)
        if (!it.removers.containsKey(this)) {
            it.debug?.log("adding listener to $this")
            var last = s
            it.removers[this] = this.addListener {
                val newVal = state.let(get)
                if(last != newVal) {
                    it.run()
                }
            }
        } else {
            it.debug?.log("already depends on $this")
        }
        it.latestPass.add(this)
        return s
    } ?: return state.let(get)
}

suspend fun <T> Readable<T>.state(): ReadableState<T> {
    coroutineContext[ReactiveScopeData.Key]?.let {
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

suspend fun <T> Readable<T>.await(): T {
    coroutineContext[ReactiveScopeData.Key]?.let {
        // If we're in a reactive scope,
        val state = state
        if (state.ready) {
            // and the value is ready to go, just add the listener and proceed with the value.
            if (!it.removers.containsKey(this)) {
                it.debug?.log("adding listener to $this")
                it.removers[this] = this.addListener {
                    it.debug?.log("READABLE LISTENER HIT A")
                    if (this.state.ready) {
                        it.run()
                    } else {
                        // Don't even bother trying to calculate, since we don't have data yet.
                        it.setLoading()
                    }
                }
            } else {
                it.debug?.log("already depends on $this")
            }
            it.latestPass.add(this)
            return state.get()
        } else {
            val listenable = this@await
            if (it.removers.containsKey(listenable)) {
//                println("ReactiveScope $it already depends on $this")
                return awaitOnce()
            }
            // otherwise, wait for the first instance of it
            return suspendCoroutineCancellable { cont ->
                var runOnce = false
                it.debug?.log("adding listener to $this")
                val remover = listenable.addListener {
                    it.debug?.log("READABLE LISTENER HIT B")
                    // The first time the listener runs, resume.  After that, rerun the whole scope.
                    val state = this@await.state
                    if (runOnce) {
                        if (state.ready) {
                            it.run()
                        } else {
                            it.setLoading()
                        }
                    } else if (state.ready) {
                        runOnce = true
                        cont.resumeState(state)
                    } else {
                        it.debug?.log("no resume")
                    }
                }
                it.latestPass.add(listenable)
                it.removers[listenable] = remover
                return@suspendCoroutineCancellable remover
            }
        }
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
        remover = addListener {
            val state = this.state
            if (state.ready) {
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

fun <T> Readable<Readable<T>>.flatten(): Readable<T> {
    val first = shared { this@flatten.await() }
    return shared { first.await().await() }
}

operator suspend fun <T> Flow<T>.invoke(): T {
    coroutineContext[ReactiveScopeData.Key]?.let {
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
                return it.lastValue[this@invoke] as T
            } else {
                return suspendCancellableCoroutine { }
            }
        }
    } ?: run {
        return first()
    }
}
