package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.CancelledException
import com.lightningkite.kiteui.cancel
import com.lightningkite.kiteui.childCancellation
import com.lightningkite.kiteui.suspendCoroutineCancellable
import kotlin.coroutines.*

class ReactiveScopeData(
    val calculationContext: CalculationContext,
    var action: suspend () -> Unit,
    var onLoad: (() -> Unit)? = null
) : CoroutineContext.Element {
    internal val removers: HashMap<ResourceUse, () -> Unit> = HashMap()
    internal val latestPass: ArrayList<ResourceUse> = ArrayList()
    override val key: CoroutineContext.Key<ReactiveScopeData> = Key
    internal var previousContext: CoroutineContext? = null

    internal fun run() {
        val context: CoroutineContext = EmptyCoroutineContext.childCancellation() + this
        previousContext?.cancel()
        previousContext = context
        latestPass.clear()

        var done = false
        var loadStarted = false

        action.startCoroutine(object : Continuation<Unit> {
            override val context: CoroutineContext = context

            // called when a coroutine ends. do nothing.
            override fun resumeWith(result: Result<Unit>) {
                done = true
                if (loadStarted) {
                    calculationContext.notifyLongComplete(result)
                } else {
                    calculationContext.notifyComplete(result)
                }
                if (previousContext !== context) return
                for (entry in removers.entries.toList()) {
                    if (entry.key !in latestPass) {
                        entry.value()
                        removers.remove(entry.key)
                    }
                }
            }
        })

        if (!done) {
            // start load
            loadStarted = true
            calculationContext.notifyStart()
            onLoad?.invoke()
        }
    }

    internal fun shutdown() {
        action = {}
        onLoad = {}
        removers.forEach { it.value() }
        removers.clear()
        latestPass.clear()
    }

    init {
        run()
        calculationContext.onRemove {
            shutdown()
        }
    }

    object Key : CoroutineContext.Key<ReactiveScopeData> {
//        init {
//            println("ReactiveScopeData V6")
//        }
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

suspend fun <T> Readable<T>.await(): T {
    return coroutineContext[ReactiveScopeData.Key]?.let {
        // If we're in a reactive scope,
        val state = state
        if(state.ready) {
            // and the value is ready to go, just add the listener and proceed with the value.
            rerunOn(this@await)
            state.get()
        } else {
            // otherwise, wait for the first instance of it
            suspendCoroutineCancellable { cont ->
                val listenable = this@await
                if (it.removers.containsKey(listenable)) {
                    println("Congratulations!  You've hit a weird case!  You have a readable whose state has changed to 'Not Ready' in the middle of a second read.  We're just going to abandon and try again.")
                    throw CancelledException()
                }
                var runOnce = false
                val remover = listenable.addListener {
                    // The first time the listener runs, resume.  After that, rerun the whole scope.
                    val state = this@await.state
                    if(state.ready) {
                        if(runOnce) it.run()
                        else {
                            runOnce = true
                            cont.resumeState(state)
                        }
                    }
                }
                it.latestPass.add(listenable)
                it.removers[listenable] = remover
                return@suspendCoroutineCancellable remover
            }
        }
    } ?: awaitOnce()
}

@Deprecated("Replace with 'awaitOnce'", ReplaceWith("this.awaitOnce()", "com.lightningkite.kiteui.reactive.awaitOnce"))
suspend fun <T> Readable<T>.awaitRaw(): T = awaitOnce()

suspend fun <T> Readable<T>.awaitOnce(): T {
    val state = state
    return if(state.ready) state.get()
    else suspendCoroutineCancellable {
        // If it's not ready, we need to wait until it is then never bother with this again.
        var remover: (()->Unit)? = null
        var alreadyRun = false
        remover = addListener {
            val state = this.state
            if(state.ready) {
                it.resumeState(state)
                remover?.invoke() ?: run {
                    alreadyRun = true
                }
            }
        }
        if(alreadyRun) remover.invoke()
        return@suspendCoroutineCancellable remover
    }
}

fun <T> Readable<Readable<T>>.flatten(): Readable<T> {
    val first = shared { this@flatten.await() }
    return shared { first.await().await() }
}
