package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.reflect.KMutableProperty0

interface CalculationContext {
    fun notifyStart() {}
    fun notifyLongComplete(result: Result<Unit>) {
        notifyComplete(result)
    }
    fun notifyComplete(result: Result<Unit>) {
        result.onFailure {
            if(it !is CancelledException) {
                it.printStackTrace2()
            }
        }
    }
    fun onRemove(action: () -> Unit)
    companion object {
    }
    object NeverEnds: CalculationContext {
        override fun onRemove(action: () -> Unit) {}
    }
    class Standard: CalculationContext, Cancellable {
        val onRemoveSet = ArrayList<()->Unit>()
        override fun onRemove(action: () -> Unit) {
            onRemoveSet.add(action)
        }
        override fun cancel() {
            onRemoveSet.invokeAllSafe()
            onRemoveSet.clear()
        }
    }
    abstract class WithLoadTracking: CalculationContext {

        var loadShown: Boolean = false
            private set
        var loadCount = 0
            set(value) {
                field = value
                if(value == 0 && loadShown) {
                    loadShown = false
                    hideLoad()
                } else if(value > 0 && !loadShown) {
                    loadShown = true
                    showLoad()
                }
            }
        abstract fun showLoad()
        abstract fun hideLoad()

        override fun notifyStart() {
            super.notifyStart()
            loadCount++
        }

        override fun notifyLongComplete(result: Result<Unit>) {
            loadCount--
            super.notifyLongComplete(result)
        }
    }
    class StandardWithLoadTracking: CalculationContext, Cancellable {
        val onRemoveSet = ArrayList<()->Unit>()
        override fun onRemove(action: () -> Unit) {
            onRemoveSet.add(action)
        }
        override fun cancel() {
            onRemoveSet.invokeAllSafe()
            onRemoveSet.clear()
        }

        val loadShown = Property(false)
        var loadCount = 0
            set(value) {
                field = value
                if(value == 0 && loadShown.value) {
                    loadShown.value = false
                } else if(value > 0 && !loadShown.value) {
                    loadShown.value = true
                }
            }

        override fun notifyStart() {
            super.notifyStart()
            loadCount++
        }

        override fun notifyLongComplete(result: Result<Unit>) {
            loadCount--
            super.notifyLongComplete(result)
        }
    }
}

fun CalculationContext.sub(): SubCalculationContext = SubCalculationContext(this)

class SubCalculationContext(val parent: CalculationContext) : CalculationContext, Cancellable {
//    override fun notifyStart() {
//        super.notifyStart()
//    }

    init {
        parent.onRemove {
            cancel()
        }
    }

    val onRemoveSet = ArrayList<()->Unit>()
    override fun onRemove(action: () -> Unit) {
        onRemoveSet.add(action)
    }
    override fun cancel() {
        onRemoveSet.forEach { it() }
        onRemoveSet.clear()
    }

//    override fun notifyComplete(result: Result<Unit>) {
//        super.notifyComplete(result)
//    }
//
//    override fun notifyLongComplete(result: Result<Unit>) {
//        super.notifyLongComplete(result)
//    }
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
    inline fun start(handler: CalculationContext) {
        stack.add(handler)
    }
    inline fun end(handler: CalculationContext) {
        if (stack.removeLast() != handler)
            throw ConcurrentModificationException("Multiple threads have been attempting to instantiate views at the same time.")
    }
}

@DslMarker
annotation class ReactiveB

@ReactiveB
inline operator fun <T, IGNORED> ((T) -> IGNORED).invoke(crossinline actionToCalculate: suspend () -> T) = CalculationContextStack.current().reactiveScope {
    this@invoke(actionToCalculate())
}

@ReactiveB
inline operator fun <T> KMutableProperty0<T>.invoke(crossinline actionToCalculate: suspend () -> T) = CalculationContextStack.current().reactiveScope {
    this@invoke.set(actionToCalculate())
}