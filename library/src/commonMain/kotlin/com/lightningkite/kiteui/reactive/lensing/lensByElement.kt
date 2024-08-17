package com.lightningkite.kiteui.reactive.lensing

import com.lightningkite.kiteui.Console
import com.lightningkite.kiteui.reactive.*
import kotlin.jvm.JvmName

fun <E, ID, W> Writable<List<E>>.lensByElement(identity: (E) -> ID, map: CalculationContext.(Writable<E>) -> W) = WritableList<E, ID, W>(this, identity = identity, elementLens = { it.map(it) })
fun <E, ID> Writable<List<E>>.lensByElement(identity: (E) -> ID) = WritableListWithoutMap<E, ID>(this, identity = identity, elementLens = { it })

@JvmName("setLensByElement") fun <E, ID, W> Writable<Set<E>>.lensByElement(identity: (E) -> ID, map: CalculationContext.(Writable<E>) -> W) = lens(get = { it.toList() }, set = { it.toSet() }).lensByElement(identity, map)
@JvmName("setLensByElement") fun <E, ID> Writable<Set<E>>.lensByElement(identity: (E) -> ID) = lens(get = { it.toList() }, set = { it.toSet() }).lensByElement(identity)


typealias WritableListWithoutMap<E, ID> = WritableList<E, ID, WritableList<E, ID, *>.ElementWritable>
class WritableList<E, ID, T>(
    val source: Writable<List<E>>,
    internal val log: Console? = null,
    val identity: (E) -> ID,
    val elementLens: (WritableList<E, ID, T>.ElementWritable)->T
) : Readable<List<T>> {
    inner class ElementWritable internal constructor(valueInit: E) : Writable<E>, ImmediateReadable<E>,
        CalculationContext {
        private val onRemoves = ArrayList<()->Unit>()
        override fun onRemove(action: () -> Unit) {
            onRemoves.add(action)
        }
        internal var dead = false
            set(value) {
                field = value
                listeners.invokeAllSafe()
                onRemoves.invokeAllSafe()
                onRemoves.clear()
            }
        var id: ID = identity(valueInit)
            private set
        private val listeners = ArrayList<() -> Unit>()
        override val state: ReadableState<E>
            get() = if(dead) ReadableState.notReady else ReadableState(value)
        override var value: E = valueInit
            set(value) {
                if (field != value) {
                    id = identity(value)
                    field = value
                    listeners.invokeAllSafe()
                }
            }
        internal var queuedSet: ReadableState<E> = ReadableState.notReady
        internal val queuedOrValue: E get(){
            val qs = queuedSet
            return if(qs.success) qs.get() else value
        }
        internal var usedFlag = false

        override suspend fun set(value: E) {
            queuedSet = ReadableState(value)
            val allWritables = elements()
            val newList = allWritables.map { it.queuedOrValue }
            if (allWritables.contains(this)) {
                try {
                    source.set(newList)
                } finally {
                    queuedSet = ReadableState.notReady
                }
                if (elements.myListen == null) {
                    this.value = value
                }
            }
        }

        override fun addListener(listener: () -> Unit): () -> Unit {
            listeners.add(listener)
            return {
                val pos = listeners.indexOfFirst { it === listener }
                if (pos != -1) {
                    listeners.removeAt(pos)
                }
            }
        }

        val view = elementLens(this)
    }

    inner class Elements: Writable<List<ElementWritable>> {
        override suspend fun set(value: List<ElementWritable>) {
            source.set(value.map { it.queuedOrValue })
        }

        private var lastElements: List<WritableList<E, ID, T>.ElementWritable> = listOf()
        private var _state: ReadableState<List<WritableList<E, ID, T>.ElementWritable>> = ReadableState.notReady
            set(value) {
                if (value.success) lastElements = value.get()
                field = value
            }
        override var state: ReadableState<List<WritableList<E, ID, T>.ElementWritable>>
            get() {
                if (myListen == null || !_state.ready) _state = getStateFromSource()
                return _state.map { it }
            }
            private set(value) {
                if (_state != value) {
                    _state = value
                    myListeners.invokeAllSafe()
                }
            }

        private val myListeners = ArrayList<() -> Unit>()
        internal var myListen: (() -> Unit)? = null
        override fun addListener(listener: () -> Unit): () -> Unit {
            if (myListeners.isEmpty()) {
                myListen = source.addListener {
                    state = getStateFromSource()
                }
                state = getStateFromSource()
            }
            myListeners.add(listener)
            return {
                myListeners.remove(listener)
                if (myListeners.isEmpty()) {
                    myListen?.invoke()
                    myListen = null
                }
            }
        }

        private fun getStateFromSource() = source.state.map { newList ->
            lastElements.forEach { it.usedFlag = false }
            val result = newList.mapIndexed { index, newElement ->
                val existing = lastElements.getOrNull(index)?.takeIf { it.id == identity(newElement) } ?: lastElements.find { old -> old.id == identity(newElement) }
                if(existing != null) {
                    existing.usedFlag = true
                    existing.value = newElement
                    existing
                } else {
                    newElement(newElement)
                }
            }
            lastElements.forEach { if(!it.usedFlag) it.dead = true }
            result
        }
    }

    val elements = Elements()

    fun newElement(e: E): ElementWritable = ElementWritable(e)
    suspend fun add(index: Int, value: E): T {
        val newly = newElement(value)
        elements.set(elements.awaitOnce().toMutableList().apply { add(index, newly) })
        return newly.view
    }
    suspend fun add(value: E): T {
        val newly = newElement(value)
        elements.set(elements.awaitOnce() + newly)
        return newly.view
    }
    suspend fun upsert(value: E): T {
        val id = identity(value)
        val existing = elements.awaitOnce().find { it.id == id }
        return if (existing == null) add(value) else {
            existing.set(value)
            existing.view
        }
    }
    suspend fun remove(element: E) {
        val id = identity(element)
        removeById(id)
    }
    suspend fun removeById(id: ID) {
        elements.set(elements.awaitOnce().filter { it.id != id })
    }

    override val state: ReadableState<List<T>>
        get() = elements.state.map { it.map { it.view } }

    override fun addListener(listener: () -> Unit): () -> Unit = elements.addListener(listener)
}
