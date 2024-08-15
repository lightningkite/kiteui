package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.Console

val <T> Readable<T>.stateReadable: Readable<ReadableState<T>> get() = TReadableStateReadable(this)

private class TReadableStateReadable<T>(val wraps: Readable<T>) : Readable<ReadableState<T>> {
    override fun hashCode(): Int = wraps.hashCode() + 1
    override fun equals(other: Any?): Boolean = other is TReadableStateReadable<*> && wraps == other.wraps
    override val state: ReadableState<ReadableState<T>> get() = ReadableState.wrap(wraps.state)
    override fun addListener(listener: () -> Unit): () -> Unit = wraps.addListener(listener)
}

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

fun <T> Writable<T>.validationLens(
    check: (T) -> Unit
): Writable<T> = validationLens<T, T>(get = { it }, set = { _, it -> check(it); it })

fun <O, T> Writable<O>.validationLens(
    get: (O) -> T,
    set: (O, T) -> O
): Writable<T> {
    return object : Writable<T> {
        private var _state: ReadableState<T> = ReadableState.notReady
        override var state: ReadableState<T>
            get() {
                @Suppress("UNCHECKED_CAST")
                if (myListen == null) _state = this@validationLens.state.map { get(it) }
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
                myListen = this@validationLens.addListener {
                    @Suppress("UNCHECKED_CAST")
                    state = this@validationLens.state.map { get(it) }
                }
                state = this@validationLens.state.map { get(it) }
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
            val old: O = this@validationLens.await()
            try {
                val new: O = set(old, value)
                this@validationLens.set(new)
                if(!_state.success) state = this@validationLens.state.map { get(it) }
            } catch(e: Exception) {
                state = ReadableState.exception(e)
            }
        }
    }
}

fun <E, ID> Writable<List<E>>.lensByElement(identity: (E) -> ID) = WritableList<E, ID>(this, identity = identity)
fun <E> Writable<List<E>>.lensByElement() = WritableList<E, E>(this) { it }

class WritableList<E, ID>(
    val source: Writable<List<E>>,
    val log: Console? = null,
    val identity: (E) -> ID,
) : Writable<List<WritableList<E, ID>.ElementWritable>> {
    inner class ElementWritable internal constructor(valueInit: E) : Writable<E>, ImmediateReadable<E> {
        var id: ID = identity(valueInit)
            private set
        private val listeners = ArrayList<() -> Unit>()
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

        override suspend fun set(value: E) {
            queuedSet = ReadableState(value)
            val allWritables = this@WritableList()
            val newList = allWritables.map { it.queuedOrValue }
            if (allWritables.contains(this)) {
                try {
                    source.set(newList)
                } finally {
                    queuedSet = ReadableState.notReady
                }
                if (myListen == null) {
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
    }

    fun newElement(e: E): ElementWritable = ElementWritable(e)
    suspend fun add(index: Int, value: E) {
        set(awaitOnce().toMutableList().apply { add(index, newElement(value)) })
    }
    suspend fun add(value: E) {
        set(awaitOnce() + newElement(value))
    }
    suspend fun remove(element: ElementWritable) {
        set(awaitOnce() - element)
    }

    private var lastElements: List<ElementWritable> = listOf()
    private var _state: ReadableState<List<ElementWritable>> = ReadableState.notReady
        set(value) {
            if (value.success) lastElements = value.get()
            field = value
        }
    override var state: ReadableState<List<ElementWritable>>
        get() {
            if (myListen == null || !_state.ready) _state = getStateFromSource()
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
        newList.mapIndexed { index, newElement ->
            val existing = lastElements.getOrNull(index)?.takeIf { it.id == identity(newElement) } ?: lastElements.find { old -> old.id == identity(newElement) }
            if(existing != null) {
                existing.value = newElement
                existing
            } else {
                newElement(newElement)
            }
        }
    }

    override suspend fun set(value: List<ElementWritable>) {
        source.set(value.map { it.queuedOrValue })
    }
}