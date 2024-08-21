package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.launch

class ReactiveContext<T>(
    val context: CalculationContext,
    val scheduled: Boolean = false,
    val onLoad: (()->Unit)? = null,
    val action: ReactiveContext<T>.()->T
): CalculationContext by context {
    private var slow = false
    var lastResult: ReadableState<T> = ReadableState.notReady

    companion object {
        internal var queue = ArrayList<ReactiveContext<*>>()
        fun runScheduled() {
            val old = queue
            queue = ArrayList()
            old.forEach { it.run() }
        }
    }

    var running = false
    val dependencies = ArrayList<Pair<Any, () -> Unit>>()
    val usedDependencies = ArrayList<Any?>()
    val rerun = { ->
        if (scheduled) queue.add(this)
        else run()
        Unit
    }

    fun rerunOn(listenable: Listenable) {
        if (existingDependency(listenable) != null) return
        dependencies += listenable to listenable.addListener(rerun)
    }

    private fun existingDependency(listenable: Any): Any? {
        usedDependencies.add(listenable)
        if (dependencies.size > usedDependencies.size) {
            val maybe = dependencies[usedDependencies.size].first
            if (maybe == listenable) return maybe
        }
        return dependencies.find { it.first == listenable }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> async(vararg dependencies: Any?, action: suspend () -> T): T {
        val key = setOf(*dependencies)
        val calc = SuspendCalculation<T>(key)
        (existingDependency(calc) as? SuspendCalculation<T>)?.let {
            return it.invoke()
        }
        this.context.launch {
            calc.state = readableState { action() }
        }
        this.dependencies += calc to calc.addListener(rerun)
        return calc.state.handle(
            success = { it },
            exception = { throw it },
            notReady = { throw ReactiveLoading }
        )
    }

    operator fun <R> Readable<R>.invoke(): R {
        if (existingDependency(this) == null) {
            dependencies += (this to this.addListener(rerun))
            // The shortcut below isn't valid because you can try/catch stuff!
//            dependencies += this to this.addListener {
//                this.state.handle(
//                    success = {
//                        if (scheduled) queue.add(this@ReactiveContext)
//                        else run()
//                        Unit
//                    },
//                    exception = {
//                        // shortcut
//                        @Suppress("UNCHECKED_CAST")
//                        setResult(this.state as ReadableState<T>)
//                    },
//                    notReady = {
//                        setResult(ReadableState.notReady)
//                    }
//                )
//            }
        }
        if (!usedDependencies.contains(this)) usedDependencies.add(this)
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
            var remover: ()->Unit = {}
            remover = addListener {
                remover()
                rerun()
            }
            dependencies += key to { remover() }
        }
        if(!usedDependencies.contains(key)) usedDependencies.add(key)
        return state.handle(
            success = { it },
            exception = { throw it },
            notReady = { throw ReactiveLoading }
        )
    }

    operator fun <T> (ReactiveContext<*>.()->T).invoke(): T = invoke(this@ReactiveContext)
    fun <T> Readable<T>.await(): T = invoke()
    fun <T> Readable<T>.awaitOnce(): T = once()

    internal fun complete() {
        val iter = dependencies.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            if (entry.first !in usedDependencies) {
                entry.second()
                iter.remove()
            }
        }
    }

    fun run() {
        running = true
        usedDependencies.clear()
        setResult(readableState { action(this) })
        val iter = dependencies.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            if (entry.first !in usedDependencies) {
                entry.second()
                iter.remove()
            }
        }
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

    init {
        context.onRemove {
            running = false
            if (scheduled) queue.remove(this)
            dependencies.forEach { it.second() }
            dependencies.clear()
        }
    }
}

fun <T> shared(action: ReactiveContext<*>.()->T): Readable<T> {
    return SharedReadable(action = action)
}

class SharedReadable<T>(useLastWhileLoading: Boolean = false, private val action: ReactiveContext<T>.() -> T) : Readable<T>, CalculationContext {
    val onRemoveSet = ArrayList<()->Unit>()
    override fun onRemove(action: () -> Unit) {
        onRemoveSet.add(action)
    }
    private fun cancel() {
        onRemoveSet.invokeAllSafe()
        onRemoveSet.clear()
    }

    override fun notifyStart() {
        super.notifyStart()
        if(lastNotified != state) {
            lastNotified = state
            listeners.invokeAllSafe()
        }
    }

    override fun notifyComplete(result: Result<Unit>) {
        super.notifyComplete(result)
        if(lastNotified != state) {
            lastNotified = state
            listeners.invokeAllSafe()
        }
    }

    override fun notifyLongComplete(result: Result<Unit>) {
        super.notifyLongComplete(result)
        if(lastNotified != state) {
            lastNotified = state
            listeners.invokeAllSafe()
        }
    }
    private val scope = ReactiveContext(this, scheduled = false, action = action)
    override val state: ReadableState<T> get() {
        if(!scope.running) scope.runOnceWhileDead()
        return scope.lastResult
    }
    private var lastNotified: ReadableState<T> = ReadableState.notReady
    val listeners = ArrayList<()->Unit>()
    override fun addListener(listener: () -> Unit): () -> Unit {
        if(listeners.size == 0) { scope.run() }
        listeners.add(listener)
        return {
            val pos = listeners.indexOfFirst { it === listener }
            if (pos != -1) {
                listeners.removeAt(pos)
            }
            if(listeners.size == 0) { cancel() }
        }
    }
}

fun CalculationContext.reactiveScope(action: ReactiveContext<Unit>.()->Unit) {
    ReactiveContext(this, scheduled = false, action = action).run()
}
fun CalculationContext.reactiveScope(onLoad: ()->Unit, action: ReactiveContext<Unit>.()->Unit) {
    ReactiveContext(this, onLoad = onLoad, scheduled = false, action = action).run()
}

private inline fun <T> readableState(action: ()->T): ReadableState<T> {
    try {
        return ReadableState(action())
    } catch(e: ReactiveLoading) {
        return ReadableState.notReady
    } catch(e: Exception) {
        return ReadableState.exception(e)
    }
}

private class SuspendCalculation<T>(val key: Any): BaseReadable<T>() {
    override var state: ReadableState<T>
        get() = super.state
        public set(value) { super.state = value }
    override fun equals(other: Any?): Boolean = other is SuspendCalculation<*> && other.key == key
    override fun hashCode(): Int = key.hashCode() + 1
    val uses get() = listeners.size
    override fun toString(): String = "SuspendCalculation($key)"
}

object ReactiveLoading: Throwable()