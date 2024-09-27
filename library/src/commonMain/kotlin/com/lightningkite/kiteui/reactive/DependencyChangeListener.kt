package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.Console
import com.lightningkite.kiteui.suspendCoroutineCancellable
import kotlinx.coroutines.*
import kotlin.coroutines.*

abstract class DependencyChangeListener : DependencyTracker(), CoroutineContext.Element {
    override val key: CoroutineContext.Key<DependencyChangeListener> get() = Key
    object Key : CoroutineContext.Key<DependencyChangeListener> {}
    abstract fun onDependencyChange()
    open fun onDependencyNotReady() = onDependencyChange()
}

private fun <T> Continuation<T>.resumeState(state: ReadableState<T>) {
    state.handle(
        success = { resume(it) },
        exception = { resumeWithException(it) },
        notReady = { resumeWithException(CancellationException("State not ready")) }
    )
}

suspend fun rerunOn(listenable: Listenable) {
    coroutineContext[DependencyChangeListener.Key]?.let {
        if(it.existingDependency(listenable) == null) {
            it.registerDependency(listenable) { it.onDependencyChange() }
        }
    }
}

suspend inline operator fun <T> Readable<T>.invoke(): T = await()
suspend inline operator fun <T> ImmediateReadable<T>.invoke(): T = await()
suspend inline fun <T> Readable<T>.exception(): Exception? = state { it.exception }

suspend fun <T, V> Readable<T>.state(get: (ReadableState<T>) -> V): V {
    coroutineContext[DependencyChangeListener.Key]?.let {
        // and the value is ready to go, just add the listener and proceed with the value.
        var last = state.let(get)
        if(it.existingDependency(this) == null) {
            it.registerDependency(this, addListener {
                val newVal = state.let(get)
                if (last != newVal) {
                    last = newVal
                    it.onDependencyChange()
                }
            })
            // Repull in case of activation
            last = state.let(get)
        }
        return last
    } ?: return state.let(get)
}

suspend fun <T> Readable<T>.state(): ReadableState<T> {
    coroutineContext[DependencyChangeListener.Key]?.let {
        // and the value is ready to go, just add the listener and proceed with the value.
        var last = state
        if(it.existingDependency(this) == null) {
            it.registerDependency(this, addListener {
                val newVal = state
                if (last != newVal) {
                    last = newVal
                    it.onDependencyChange()
                }
            })
            // Repull in case of activation
            last = state
        }
        return last
    } ?: return state
}

suspend fun <T> ImmediateReadable<T>.await(): T {
    coroutineContext[DependencyChangeListener.Key]?.let {
        // and the value is ready to go, just add the listener and proceed with the value.
        var last = value
        if(it.existingDependency(this) == null) {
            it.registerDependency(this, addListener {
                val newVal = value
                if (last != newVal) {
                    last = newVal
                    it.onDependencyChange()
                }
            })
            // Repull in case of activation
            last = value
        }
        return last
    } ?: return value
}

suspend fun <T> Readable<T>.await(): T {
    coroutineContext[DependencyChangeListener.Key]?.let {
        var cont: Continuation<T>? = null
        if (it.existingDependency(this) == null) {
            it.registerDependency(this, addListener {
                it.log?.log("readable listener hit with $state, cont is $cont")
                state.handle(
                    success = { r ->
                        cont?.let { c ->
                            c.resume(r)
                            cont = null
                        } ?: it.onDependencyChange()
                    },
                    exception = { r ->
                        cont?.let { c ->
                            c.resumeWithException(r)
                            cont = null
                        } ?: it.onDependencyChange()
                    },
                    notReady = {
                        if(cont == null) it.onDependencyNotReady()
                    }
                )
            })
        } else {
            it.log?.log("already depends on $this")
        }

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