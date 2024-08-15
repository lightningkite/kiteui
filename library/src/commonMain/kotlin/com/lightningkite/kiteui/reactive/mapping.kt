package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.Console
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

fun <O, T> Readable<O>.lens(
    get: (O) -> T
): Readable<T> {
    return object : Readable<T> {
        private var _state: ReadableState<T> = ReadableState.notReady
        override var state: ReadableState<T>
            get() {
                @Suppress("UNCHECKED_CAST")
                if (myListen == null) _state = this@lens.state.map { get(it) }
                return _state
            }
            private set(value) {
                if (_state != value) {
                    _state = value
                    myListeners.invokeAllSafe()
                }
            }

        private val myListeners = ArrayList<() -> Unit>()
        private var myListen: (() -> Unit)? = null
        override fun addListener(listener: () -> Unit): () -> Unit {
            myListeners.add(listener)
            if (myListeners.size == 1) {
                myListen = this@lens.addListener {
                    @Suppress("UNCHECKED_CAST")
                    state = this@lens.state.map { get(it) }
                }
                state = this@lens.state.map { get(it) }
            }
            return {
                myListeners.remove(listener)
                if (myListeners.isEmpty()) {
                    myListen?.invoke()
                    myListen = null
                }
            }
        }
    }
}

@Deprecated("use the new name, lens, instead", ReplaceWith("lens", "com.lightningkite.kiteui.reactive.lens"))
fun <O, T> Writable<O>.map(
    get: (O) -> T,
    set: (O, T) -> O
): Writable<T> = lens(get, set)

fun <O, T> Writable<O>.lens(
    get: (O) -> T,
    set: (O, T) -> O
): Writable<T> {
    return object : Writable<T> {
        private var _state: ReadableState<T> = ReadableState.notReady
        override var state: ReadableState<T>
            get() {
                @Suppress("UNCHECKED_CAST")
                if (myListen == null) _state = this@lens.state.map { get(it) }
                return _state
            }
            private set(value) {
                if (_state != value) {
                    _state = value
                    myListeners.invokeAllSafe()
                }
            }

        private val myListeners = ArrayList<() -> Unit>()
        private var myListen: (() -> Unit)? = null
        override fun addListener(listener: () -> Unit): () -> Unit {
            myListeners.add(listener)
            if (myListeners.size == 1) {
                myListen = this@lens.addListener {
                    @Suppress("UNCHECKED_CAST")
                    state = this@lens.state.map { get(it) }
                }
                state = this@lens.state.map { get(it) }
            }
            return {
                myListeners.remove(listener)
                if (myListeners.isEmpty()) {
                    myListen?.invoke()
                    myListen = null
                }
            }
        }

        /**
         * Queues changes
         */
        override suspend fun set(value: T) {
            val old: O = this@lens.await()
            val new: O = set(old, value)
            this@lens.set(new)
        }
    }
}

fun <E, ID, W> Writable<List<E>>.lensByElement(identity: (E) -> ID, map: CalculationContext.(Writable<E>) -> W) = WritableList<E, ID, W>(this, identity = identity, createTag = { it.map(it) })
fun <E, W> Writable<List<E>>.lensByElement(map: CalculationContext.(Writable<E>) -> W) = WritableList<E, E, W>(this, identity = { it }, createTag = { it.map(it) })
fun <E> Writable<List<E>>.lensByElement() = WritableList<E, E, Writable<E>>(this, identity = { it }, createTag = { it })

class WritableList<E, ID, T>(
    val source: Writable<List<E>>,
    val log: Console? = null,
    val identity: (E) -> ID,
    val createTag: (WritableList<E, ID, T>.ElementWritable)->T
) : Readable<List<T>> {
    inner class ElementWritable internal constructor(valueInit: E) : Writable<E>, ImmediateReadable<E>, CalculationContext {
        val job = Job()
        override val coroutineContext: CoroutineContext get() = job
        var dead = false
            set(value) {
                field = value
                listeners.invokeAllSafe()
                job.cancel()
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
        var queuedSet: ReadableState<E> = ReadableState.notReady
        val queuedOrValue: E get(){
            val qs = queuedSet
            return if(qs.success) qs.get() else value
        }
        var usedFlag = false

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

        val tagged = createTag(this)
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
        var myListen: (() -> Unit)? = null
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
    suspend fun add(index: Int, value: E) {
        elements.set(elements.awaitOnce().toMutableList().apply { add(index, newElement(value)) })
    }
    suspend fun add(value: E) {
        elements.set(elements.awaitOnce() + newElement(value))
    }
    suspend fun remove(element: E) {
        val id = identity(element)
        elements.set(elements.awaitOnce().filter { it.id != id })
    }

    override val state: ReadableState<List<T>>
        get() = elements.state.map { it.map { it.tagged } }

    override fun addListener(listener: () -> Unit): () -> Unit = elements.addListener(listener)
}
