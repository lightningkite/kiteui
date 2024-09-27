package com.lightningkite.kiteui.reactive

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KMutableProperty0

public interface StatusListener : CoroutineContext.Element {
    override val key: CoroutineContext.Key<*> get() = Key
    /**
     * Key for [Job] instance in the coroutine context.
     */
    public companion object Key : CoroutineContext.Key<StatusListener>

    fun loading(readable: Readable<*>)
    fun working(readable: Readable<*>) = loading(readable)
}

fun CoroutineScope.onRemove(action: () -> Unit) {
    coroutineContext[CoroutineName.Key]
    this.coroutineContext[Job]?.invokeOnCompletion { action() }
}

typealias CalculationContext = CoroutineScope
@OptIn(ExperimentalStdlibApi::class)
val CoroutineScope.requireMainThread: Boolean get() = coroutineContext[CoroutineDispatcher.Key] is MainCoroutineDispatcher
@OptIn(ExperimentalStdlibApi::class)
inline fun CoroutineScope.onThread(crossinline action: ()->Unit) {
    val d = coroutineContext[CoroutineDispatcher.Key] ?: return action()
    if(d.isDispatchNeeded(coroutineContext)) {
        d.dispatch(coroutineContext, Runnable(action))
    } else {
        action()
    }
}

object CoroutineScopeStack {
    val stack = ArrayList<CoroutineScope>()
    fun current() = stack.lastOrNull() ?: throw IllegalStateException("CalculationContextStack.onRemove called outside of a builder.")

    inline fun useIn(handler: CoroutineScope, action: () -> Unit) {
        start(handler)
        try {
            action()
        } finally {
            end(handler)
        }
    }
    // Performance is very sensitive here, and this is a one-liner.  No need to perform a whole call for this.
    @Suppress("NOTHING_TO_INLINE")
    inline fun start(handler: CoroutineScope) {
        stack.add(handler)
    }
    fun end(handler: CoroutineScope) {
        if (stack.removeLast() != handler)
            throw ConcurrentModificationException("Multiple threads have been attempting to instantiate views at the same time.")
    }
}

@DslMarker
annotation class Reactive

@Reactive
inline operator fun <T, IGNORED> ((T) -> IGNORED).invoke(crossinline actionToCalculate: ReactiveContext.() -> T) = CoroutineScopeStack.current().reactiveScope {
    this@invoke(actionToCalculate(this))
}

@Reactive
inline operator fun <T> KMutableProperty0<T>.invoke(crossinline actionToCalculate: ReactiveContext.() -> T) = CoroutineScopeStack.current().reactiveScope {
    this@invoke.set(actionToCalculate(this))
}