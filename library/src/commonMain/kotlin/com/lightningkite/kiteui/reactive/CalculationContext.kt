package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.*
import kotlinx.coroutines.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.reflect.KMutableProperty0

interface CalculationContext: CoroutineScope {
    @OptIn(ExperimentalStdlibApi::class)
    val requireMainThread: Boolean get() = coroutineContext[CoroutineDispatcher.Key] is MainCoroutineDispatcher
    fun notifyStart() {}
    fun notifyLongComplete(result: Result<Unit>) {
        notifyComplete(result)
    }
    fun notifyComplete(result: Result<Unit>) {
        result.onFailure {
            if(it !is CancelledException) {
                it.report()
            }
        }
    }

//    fun startReporting(): Reporter = NoOpReporter
//    interface Reporter {
//        fun willDelay()
//        fun complete(result: Result<Unit>)
//        fun invalidate()
//    }
//    object NoOpReporter: Reporter {
//        override fun willDelay() {}
//        override fun complete(result: Result<Unit>) {}
//        override fun invalidate() {}
//    }

    fun onRemove(action: () -> Unit) {
        this.coroutineContext[Job]?.invokeOnCompletion { action() }
    }
    companion object {
    }
    @DelicateCoroutinesApi
    object NeverEnds: CalculationContext, CoroutineScope by GlobalScope {
    }
    class Standard: CalculationContext {
        val job = Job()
        override val coroutineContext: CoroutineContext get() = job
        fun cancel() = job.cancel()
    }
}

fun CalculationContext.sub(): SubCalculationContext = SubCalculationContext(this)

class SubCalculationContext(parent: CalculationContext) : CalculationContext {
    private val sub = Job(parent.coroutineContext[Job])
    override val coroutineContext: CoroutineContext = parent.coroutineContext + sub
}

object CalculationContextStack {
    val stack = ArrayList<CalculationContext>()
    fun current() = stack.lastOrNull() ?: throw IllegalStateException("CalculationContextStack.onRemove called outside of a builder.")

    inline fun useIn(handler: CalculationContext, action: () -> Unit) {
        start(handler)
        try {
            action()
        } finally {
            end(handler)
        }
    }
    // Performance is very sensitive here, and this is a one-liner.  No need to perform a whole call for this.
    @Suppress("NOTHING_TO_INLINE")
    inline fun start(handler: CalculationContext) {
        stack.add(handler)
    }
    fun end(handler: CalculationContext) {
        if (stack.removeLast() != handler)
            throw ConcurrentModificationException("Multiple threads have been attempting to instantiate views at the same time.")
    }
}

@DslMarker
annotation class Reactive

@Reactive
inline operator fun <T, IGNORED> ((T) -> IGNORED).invoke(crossinline actionToCalculate: ReactiveContext.() -> T) = CalculationContextStack.current().reactiveScope {
    this@invoke(actionToCalculate(this))
}

@Reactive
inline operator fun <T> KMutableProperty0<T>.invoke(crossinline actionToCalculate: ReactiveContext.() -> T) = CalculationContextStack.current().reactiveScope {
    this@invoke.set(actionToCalculate(this))
}