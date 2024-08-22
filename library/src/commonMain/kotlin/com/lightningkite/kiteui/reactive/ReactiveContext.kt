@file:OptIn(InternalKiteUi::class)

package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.launch
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.CoroutineContext

abstract class ReactiveContext : CalculationContext {
    var log: Console? = null
    // Dependency management
    var active = false
        private set
    private val dependencies = ArrayList<Pair<Any, () -> Unit>>()
    private val usedDependencies = ArrayList<Any?>()
    abstract val rerun: () -> Unit

    @Suppress("UNCHECKED_CAST")
    fun <T> existingDependency(listenable: T): T? {
        usedDependencies.add(listenable)
        if (dependencies.size > usedDependencies.size) {
            val maybe = dependencies[usedDependencies.size].first
            if (maybe == listenable) return maybe as T
        }
        return dependencies.find { it.first == listenable }?.first as? T
    }

    fun registerDependency(any: Any, remove: () -> Unit) {
        this.dependencies += any to remove
        log?.log("Registered dependency on $any")
    }

    protected fun runStart() {
        active = true
        usedDependencies.clear()
        log?.log("Run start")
    }

    protected fun runComplete() {
        log?.log("Run complete")
        val iter = dependencies.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            if (entry.first !in usedDependencies) {
                log?.log("Dependency on ${entry?.first} no longer used")
                entry.second()
                iter.remove()
            }
        }
    }

    protected open fun cancel() {
        active = false
        dependencies.forEach { it.second() }
        dependencies.clear()
    }

    //////////////////////////////////////////////////////////////////////////////////
    // Everything after this point only uses public API from above.
    // Eventually, this should use context receivers.  However, that's not stable yet.
    //////////////////////////////////////////////////////////////////////////////////

    // Operators for standard reactive tools
    fun rerunOn(listenable: Listenable) {
        if (existingDependency(listenable) != null) return
        registerDependency(listenable, listenable.addListener(rerun))
    }

    operator fun <R> Readable<R>.invoke(): R {
        if (existingDependency(this) == null) {
            registerDependency(this, addListener(rerun))
        }
        return state.handle(
            success = { it },
            exception = { throw it },
            notReady = { throw ReactiveLoading }
        )
    }

    private data class Once<T>(val wraps: Readable<T>)

    fun <T> Readable<T>.once(): T {
        val key = Once(this)
        if (existingDependency(key) == null) {
            var remover: () -> Unit = {}
            remover = addListener {
                remover()
                rerun()
            }
            registerDependency(key, remover)
        }
        return state.handle(
            success = { it },
            exception = { throw it },
            notReady = { throw ReactiveLoading }
        )
    }

    // Hack: fixes compiler weirdness around lambdas with 'this'
    inline operator fun <T> (ReactiveContext.() -> T).invoke(): T = invoke(this@ReactiveContext)
    inline operator fun <A, T> (ReactiveContext.(A) -> T).invoke(a: A): T = invoke(this@ReactiveContext, a)
    inline operator fun <A, B, T> (ReactiveContext.(A, B) -> T).invoke(a: A, b: B): T = invoke(this@ReactiveContext, a, b)

    @Deprecated("Just use the invoke operator", ReplaceWith("this()"))
    fun <T> Readable<T>.await(): T = invoke()
    @Deprecated("Just use the once function", ReplaceWith("this.once()"))
    fun <T> Readable<T>.awaitOnce(): T = once()


    // Suspending calculations

    private class SuspendCalculation<T>(val key: Any) : BaseReadable<T>() {
        override var state: ReadableState<T>
            get() = super.state
            public set(value) {
                super.state = value
            }

        override fun equals(other: Any?): Boolean = other is SuspendCalculation<*> && other.key == key
        override fun hashCode(): Int = key.hashCode() + 1
        override fun toString(): String = "SuspendCalculation($key)"
    }

    fun <T> async(vararg dependencies: Any?, action: suspend () -> T): T {
        val key = setOf(*dependencies)
        val calc = SuspendCalculation<T>(key)
        existingDependency(calc)?.let {
            return it.invoke()
        }
        launch {
            calc.state = readableState { action() }
        }
        registerDependency(calc, calc.addListener(rerun))
        return calc.state.handle(
            success = { it },
            exception = { throw it },
            notReady = { throw ReactiveLoading }
        )
    }

    operator fun <T> Deferred<T>.invoke(): T {
        val calc = SuspendCalculation<T>(this)
        existingDependency(calc)?.let {
            return it.invoke()
        }
        launch {
            calc.state = readableState { this@invoke.await() }
        }
        registerDependency(calc, calc.addListener(rerun))
        return calc.state.handle(
            success = { it },
            exception = { throw it },
            notReady = { throw ReactiveLoading }
        )
    }


    // Flows

    private class FlowLoader<T>(val flow: Flow<T>) {
        var state: ReadableState<T> = ReadableState.notReady
        override fun hashCode(): Int = flow.hashCode()
        override fun equals(other: Any?): Boolean = other is FlowLoader<*> && flow == other.flow
        override fun toString(): String = "${super.toString()}/$flow"
    }

    operator fun <T> Flow<T>.invoke(): T {
        val new = FlowLoader(this)

        val existing = existingDependency(new)
        if (existing == null) {
            var job: Job? = null
            registerDependency(new, { job?.cancel() })
            job = CoroutineScope(coroutineContext).launch {
                collect { v ->
                    try {
                        new.state = ReadableState(v)
                        rerun()
                    } catch (e: Exception) {
                        new.state = ReadableState.exception<T>(e)
                    }
                }
            }
            if (this is StateFlow<T>) return this.value
            else throw ReactiveLoading
        } else {
            return existing.state.handle(
                success = { it },
                exception = { throw it },
                notReady = { throw ReactiveLoading }
            )
        }
    }
}

@InternalKiteUi
class DirectReactiveContext<T> constructor(
    val context: CalculationContext,
    val scheduled: Boolean = false,
    val onLoad: (() -> Unit)? = null,
    val action: DirectReactiveContext<T>.() -> T
) : ReactiveContext() {
    private var slow = false
    var lastResult: ReadableState<T> = ReadableState.notReady

    override val coroutineContext: CoroutineContext get() = context.coroutineContext
    override val requireMainThread: Boolean get() = context.requireMainThread

    companion object {
        internal var queue = HashSet<DirectReactiveContext<*>>()
        fun runScheduled() {
            val old = queue
            queue = HashSet()
            old.forEach { it.start() }
        }
    }

    override val rerun: () -> Unit = when {
        scheduled -> { -> queue.add(this) }
        context.requireMainThread -> { -> onMainThread { runInternal() } }
        else -> { -> runInternal() }
    }
    fun start() {
        if(context.requireMainThread) onMainThread {
            runInternal()
        } else {
            runInternal()
        }
    }
    fun runInternal() {
        runStart()
        setResult(readableState { action(this@DirectReactiveContext) })
        runComplete()
    }

    fun setResult(state: ReadableState<T>) {
        lastResult = state
        if (state.ready) {
            if (slow) {
                slow = false
                context.notifyLongComplete(
                    if (state.success) Result.success(Unit) else Result.failure(
                        state.exception ?: NotReadyException()
                    )
                )
            } else {
                context.notifyComplete(
                    if (state.success) Result.success(Unit) else Result.failure(
                        state.exception ?: NotReadyException()
                    )
                )
            }
        } else {
            if (!slow) {
                slow = true
                context.notifyStart()
                onLoad?.invoke()
            }
        }
    }

    fun runOnceWhileDead() {
        lastResult = readableState { action(this) }
    }

    init { context.onRemove { cancel() } }
    override fun cancel() {
        super.cancel()
        if (scheduled) queue.remove(this)
    }
}

fun CalculationContext.reactive(action: ReactiveContext.() -> Unit) {
    DirectReactiveContext(this, scheduled = false, action = action).start()
}

fun CalculationContext.reactive(onLoad: () -> Unit, action: ReactiveContext.() -> Unit) {
    DirectReactiveContext(this, onLoad = onLoad, scheduled = false, action = action).start()
}

fun CalculationContext.reactiveScope(action: ReactiveContext.() -> Unit) {
    DirectReactiveContext(this, scheduled = false, action = action).start()
}

fun CalculationContext.reactiveScope(onLoad: () -> Unit, action: ReactiveContext.() -> Unit) {
    DirectReactiveContext(this, onLoad = onLoad, scheduled = false, action = action).start()
}

object ReactiveLoading : Throwable()