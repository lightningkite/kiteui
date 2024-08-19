package com.lightningkite.kiteui.validation

import com.lightningkite.kiteui.reactive.*
import kotlin.coroutines.coroutineContext

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

    suspend fun watchIsEmpty(): Boolean? = state { state -> state.onData { it.isEmpty() } }

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