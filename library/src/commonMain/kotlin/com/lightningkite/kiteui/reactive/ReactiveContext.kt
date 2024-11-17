@file:OptIn(InternalKiteUi::class)

package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
var reactiveContext: ReactiveContext? = null

typealias ReactiveContext = TypedReactiveContext<*>
class TypedReactiveContext<T>(
    val scope: CalculationContext,
    override val log: Console? = null,
    private val reportTo: RawReadable<T> = RawReadable<T>(),
    val action: TypedReactiveContext<T>.() -> T
): DependencyTracker(), CalculationContext by scope, Readable<T> by reportTo {
    companion object {
    }

    var active = false
        private set
    val rerun: () -> Unit = ::startCalculation

    private var queued = false

    fun startCalculation() {
        active = true
        if (queued) return
        queued = true
        scope.onThread {
            queued = false
            if (!active) {
                return@onThread
            }
            val old = reactiveContext
            reactiveContext = this
            dependencyBlockStart()
            log?.log("Run start")
            reportTo.state = readableState { action(this@TypedReactiveContext) }
            log?.log("Run complete")
            dependencyBlockEnd()
            reactiveContext = old
        }
    }

    fun runOnceWhileDead() {
        reportTo.state = readableState { action(this) }
    }

    init {
        scope.onRemove { cancel() }
    }

    override fun cancel() {
        active = false
        queued = false
        super.cancel()
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
            ready = { it },
            exception = { throw it },
            notReady = { throw ReactiveLoading }
        )
    }

    fun <R> Readable<R?>.awaitNotNull(): R {
        if (existingDependency(this) == null) {
            registerDependency(this, addListener(rerun))
        }
        return state.handle(
            ready = { it ?: throw ReactiveLoading },
            exception = { throw it },
            notReady = { throw ReactiveLoading }
        )
    }

    fun <R> Readable<R>.state(): ReadableState<R> {
        if (existingDependency(this) == null) {
            registerDependency(this, addListener(rerun))
        }
        return state
    }
    fun <R, V> Readable<R>.state(get: (ReadableState<R>) -> V): V {
        var previous = get(state)
        if (existingDependency(this) == null) {
            registerDependency(
                this,
                addListener {
                    get(state).let {
                        if (it == previous) return@let
                        previous = it
                        rerun()
                    }

                }
            )
        }
        return previous
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
            ready = { it },
            exception = { throw it },
            notReady = { throw ReactiveLoading }
        )
    }

    // Hack: fixes compiler weirdness around lambdas with 'this'
    @Suppress("NOTHING_TO_INLINE")
    inline operator fun <T> (ReactiveContext.() -> T).invoke(): T = invoke(this@TypedReactiveContext)

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun <A, T> (ReactiveContext.(A) -> T).invoke(a: A): T = invoke(this@TypedReactiveContext, a)

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun <A, B, T> (ReactiveContext.(A, B) -> T).invoke(a: A, b: B): T = invoke(this@TypedReactiveContext, a, b)

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
        scope.launch {
            calc.state = readableState { action() }
        }
        registerDependency(calc, calc.addListener(rerun))
        return calc.state.handle(
            ready = { it },
            exception = { throw it },
            notReady = { throw ReactiveLoading }
        )
    }

    operator fun <T> Deferred<T>.invoke(): T {
        val calc = SuspendCalculation<T>(this)
        existingDependency(calc)?.let {
            return it.invoke()
        }
        scope.launch {
            calc.state = readableState { this@invoke.await() }
        }
        registerDependency(calc, calc.addListener(rerun))
        return calc.state.handle(
            ready = { it },
            exception = { throw it },
            notReady = { throw ReactiveLoading }
        )
    }


    // Flows

    private class FlowLoader<T>(val flow: Flow<T>) {
        var state: ReadableState<T> = ReadableState.NotReady
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
            job = scope.launch {
                collect { v ->
                    try {
                        new.state = ReadableState(v)
                        rerun()
                    } catch (e: Exception) {
                        new.state = ReadableState.Exception(e)
                    }
                }
            }
            if (this is StateFlow<T>) return this.value
            else throw ReactiveLoading
        } else {
            return existing.state.handle(
                ready = { it },
                exception = { throw it },
                notReady = { throw ReactiveLoading }
            )
        }
    }

    // Validation

    fun <T> Readable<T>.invalid(): ReadableState.Invalid<T>? = state { it.invalid }
}

fun <T> CalculationContext.reactive(log: Console? = null, action: ReactiveContext.() -> T): TypedReactiveContext<T> {
    val trc = TypedReactiveContext(this, action = action, log = log)
    trc.startCalculation()
    coroutineContext[StatusListener.Key]?.loading(trc)
    return trc
}

inline fun <T> CalculationContext.reactive(log: Console? = null, crossinline onLoad: () -> Unit, crossinline action: ReactiveContext.() -> Unit): TypedReactiveContext<Unit> {
    var wasLoadingLastTime = false
    return reactive(log) {
        try {
            action(this)
            wasLoadingLastTime = false
        } catch(e: ReactiveLoading) {
            if(wasLoadingLastTime) {
                onLoad()
                wasLoadingLastTime = true
            }
            throw e
        } catch(e: Exception) {
            wasLoadingLastTime = false
        }
    }
}

fun CalculationContext.reactiveScope(action: ReactiveContext.() -> Unit) = reactive(action = action)

inline fun CalculationContext.reactiveScope(crossinline onLoad: () -> Unit, crossinline action: ReactiveContext.() -> Unit) = reactive<Unit>(onLoad = onLoad, action = action)

fun <T> Readable<T>.onNextReady(action: (T) -> Unit) {
    if (state.onReady(action) == null) {
        var remover = {}
        remover = addListener {
            state.onReady {
                action(it)
                remover.invoke()
            }
        }
    }
}

object ReactiveLoading : Throwable()