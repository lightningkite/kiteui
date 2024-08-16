package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.launch
import com.lightningkite.kiteui.reactive.ReactiveScopeData2.Companion.queue

class ReactiveScopeData2<T>(
    val context: CalculationContext,
    val scheduled: Boolean,
    val action: ReactiveContext.()->T
) {
    var lastResult: ReadableState<T> = ReadableState.notReady
    companion object {
        internal var queue = ArrayList<ReactiveScopeData2<*>>()
        fun runScheduled() {
            val old = queue
            queue = ArrayList()
            old.forEach { it.run() }
        }
    }
    data class SubscriptionInfo(val key: Any, val remove: ()->Unit)
    val dependencies = HashMap<Any, SubscriptionInfo>()
    private var slow = false
    fun run() {
        val c = ReactiveContext(this)
        lastResult = readableState { action(c) }
        if(lastResult.ready) {
            if(slow) {
                slow = false
                context.notifyLongComplete(if(lastResult.success) Result.success(Unit) else Result.failure(lastResult.exception ?: NotReadyException()))
            } else {
                context.notifyComplete(if(lastResult.success) Result.success(Unit) else Result.failure(lastResult.exception ?: NotReadyException()))
            }
        } else {
            if(!slow) {
                slow = true
                context.notifyStart()
            }
        }
        c.complete()
    }
    init {
        context.onRemove {
            if(scheduled) queue.remove(this)
            dependencies.values.forEach { it.remove() }
            dependencies.clear()
        }
    }
}

fun <T> shared2(action: ReactiveContext.()->T): Readable<T> {
    return object: Readable<T>, CalculationContext {
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
            listeners.invokeAllSafe()
        }

        override fun notifyComplete(result: Result<Unit>) {
            super.notifyComplete(result)
            listeners.invokeAllSafe()
        }

        override fun notifyLongComplete(result: Result<Unit>) {
            super.notifyLongComplete(result)
            listeners.invokeAllSafe()
        }
        private val scope = ReactiveScopeData2(this, false, action)
        override val state: ReadableState<T> get() = scope.lastResult
        val listeners = ArrayList<()->Unit>()
        override fun addListener(listener: () -> Unit): () -> Unit {
            listeners.add(listener)
            if(listeners.size == 1) { scope.run() }
            return {
                listeners.remove(listener)
                if(listeners.isEmpty()) this.cancel()
            }
        }
    }
}

fun CalculationContext.reactive(action: ReactiveContext.()->Unit) {
    ReactiveScopeData2(this, scheduled = false, action = action).run()
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

class ReactiveContext(
    val scope: ReactiveScopeData2<*>
): CalculationContext by scope.context {

    val usedDependencies = HashSet<Any?>()
    fun rerunOn(listenable: Listenable) {
        if (scope.dependencies[listenable] != null) return
        scope.dependencies[listenable] = ReactiveScopeData2.SubscriptionInfo(listenable, listenable.addListener {
            if(scope.scheduled) queue.add(scope)
            else scope.run()
        })
    }
    @Suppress("UNCHECKED_CAST")
    fun <T> async(vararg dependencies: Any?, action: suspend () -> T): T {
        val key = setOf(*dependencies)
        val calc = SuspendCalculation<T>(key)
        usedDependencies.add(calc)
        (scope.dependencies[calc]?.key as? SuspendCalculation<*>)?.let {
            println("Found previous calculation instance for $key, using")
            return (it.invoke() as T).also { println("Previous calc is now $it") }
        }
        println("Starting new calculation instance for $key")
        launch {
            calc.state = readableState { action() }
            println("Completed calculation; ${calc.uses}")
        }
        scope.dependencies[calc] = ReactiveScopeData2.SubscriptionInfo(calc, calc.addListener {
            println("Recalculating")
            if(scope.scheduled) queue.add(scope)
            else scope.run()
        })
        println("Listener added")
        return calc.state.handle(
            success = { it },
            exception = { throw it },
            notReady = { throw ReactiveLoading }
        )
    }
    operator fun <T> Readable<T>.invoke(): T {
        rerunOn(this)
        usedDependencies.add(this)
        return state.handle(
            success = { it },
            exception = { throw it },
            notReady = { throw ReactiveLoading }
        )
    }
    internal fun complete() {
        val iter = scope.dependencies.entries.iterator()
        while(iter.hasNext()) {
            val entry = iter.next()
            if(entry.key !in usedDependencies) {
                println("Cleaned dependency ${entry.value.key}")
                entry.value.remove()
                iter.remove()
            }
        }
    }
}

object ReactiveLoading: Exception()