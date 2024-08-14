package com.lightningkite.kiteui.validation

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.suspendCoroutineCancellable
import kotlin.coroutines.coroutineContext

/**
 * Only signals for a recalculation when the [condition] is met
* */
suspend fun <T> Readable<T>.awaitCondition(condition: (T) -> Boolean): T {
    return coroutineContext[ReactiveScopeData.Key]?.let {
        val state = state
        if(state.ready) {
            if (!it.removers.containsKey(this)) {
                it.removers[this] = this.addListener {
                    val state = this.state
                    if(state.ready) {
                        if (condition(state.get())) it.run()
                    } else {
                        it.setLoading()
                    }
                }
            }
            it.latestPass.add(this)
            state.get()
        } else {
            val listenable = this
            if (it.removers.containsKey(listenable)) {
                return@let awaitOnce()
            }
            suspendCoroutineCancellable { cont ->
                var runOnce = false
                val remover = listenable.addListener {
                    val state = this.state
                    if(runOnce) {
                        if(state.ready) {
                            if (condition(state.get())) it.run()
                        } else {
                            it.setLoading()
                        }
                    } else if(state.ready) {
                        runOnce = true
                        cont.resumeState(state)
                    } else {
//                        println("ReactiveScope $it no resume")
                    }
                }
                it.latestPass.add(listenable)
                it.removers[listenable] = remover
                return@suspendCoroutineCancellable remover
            }
        }
    } ?: awaitOnce()
}

class SignalingList<T>
    private constructor(private val list: MutableList<T>)
    : MutableList<T> by list, ImmediateReadable<List<T>>
{
    constructor() : this(ArrayList<T>())
    constructor(startingItems: List<T>) : this(ArrayList(startingItems))
    constructor(vararg startingItems: T) : this(ArrayList(startingItems.toList()))

    override val value: List<T> get() = list

    private val listeners = ArrayList<() -> Unit>()
    override fun addListener(listener: () -> Unit): () -> Unit {
        listeners.add(listener)
        return {
            val pos = listeners.indexOfFirst { it === listener }
            if (pos != -1) {3
                listeners.removeAt(pos)
            }
        }
    }

    private fun <V> changeList(action: MutableList<T>.() -> V): V = list.action().also { listeners.invokeAllSafe() }

    override fun clear() = changeList { clear() }
    override fun removeAt(index: Int): T = changeList { removeAt(index) }
    override fun set(index: Int, element: T): T = changeList { set(index, element) }
    override fun retainAll(elements: Collection<T>): Boolean = changeList { retainAll(elements) }
    override fun removeAll(elements: Collection<T>): Boolean = changeList { removeAll(elements) }
    override fun remove(element: T): Boolean = changeList { remove(element) }
    override fun addAll(elements: Collection<T>): Boolean = changeList { addAll(elements) }
    override fun addAll(index: Int, elements: Collection<T>): Boolean = changeList { addAll(index, elements) }
    override fun add(index: Int, element: T) = changeList { add(index, element) }
    override fun add(element: T): Boolean = changeList { add(element) }
}

class SignallingSet<T>
    private constructor(private val set: MutableSet<T>)
    : MutableSet<T> by set, ImmediateReadable<Set<T>>
{
    constructor() : this(HashSet<T>())
    constructor(startingItems: List<T>) : this(HashSet(startingItems))
    constructor(vararg startingItems: T) : this(HashSet(startingItems.toList()))

    override val value: Set<T> get() = set

    private val listeners = ArrayList<() -> Unit>()
    override fun addListener(listener: () -> Unit): () -> Unit {
        listeners.add(listener)
        return {
            val pos = listeners.indexOfFirst { it === listener }
            if (pos != -1) {
                listeners.removeAt(pos)
            }
        }
    }

    private fun <V> changeSet(action: MutableSet<T>.() -> V): V = set.action().also { listeners.invokeAllSafe() }

    override fun clear() = changeSet { clear() }
    override fun retainAll(elements: Collection<T>): Boolean = changeSet { retainAll(elements.toSet()) }
    override fun removeAll(elements: Collection<T>): Boolean = changeSet { removeAll(elements.toSet()) }
    override fun remove(element: T): Boolean = changeSet { remove(element) }
    override fun addAll(elements: Collection<T>): Boolean = changeSet { addAll(elements) }
    override fun add(element: T): Boolean = changeSet { add(element) }
}