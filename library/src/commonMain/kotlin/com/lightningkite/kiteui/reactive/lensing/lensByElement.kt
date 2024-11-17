package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.Console
import com.lightningkite.kiteui.printStackTrace2
import com.lightningkite.kiteui.reactive.lensing.lens
import com.lightningkite.kiteui.report
import kotlinx.coroutines.*
import kotlin.coroutines.cancellation.CancellationException
import kotlin.jvm.JvmName


@Deprecated("Be specific about what kind you need.")
fun <E, ID, W> Writable<List<E>>.lensByElement(identity: (E) -> ID, map: CalculationContext.(Writable<E>) -> W) =
    WritableList<E, ID, W>(this, identity = identity, elementLens = { it.map(it) })

@Deprecated("Be specific about what kind you need.")
fun <E, ID> Writable<List<E>>.lensByElement(identity: (E) -> ID) =
    WritableListWithoutMap<E, ID>(this, identity = identity, elementLens = { it })

@Deprecated("Be specific about what kind you need.")
@JvmName("setLensByElement")
fun <E, ID, W> Writable<Set<E>>.lensByElement(identity: (E) -> ID, map: CalculationContext.(Writable<E>) -> W) =
    lens("", get = { it.toList() }, set = { it.toSet() }).lensByElement(identity, map)

@Deprecated("Be specific about what kind you need.")
@JvmName("setLensByElement")
fun <E, ID> Writable<Set<E>>.lensByElement(identity: (E) -> ID) =
    lens("", get = { it.toList() }, set = { it.toSet() }).lensByElement(identity)

fun <E, ID, W> Writable<List<E>>.lensByElementWithIdentity(
    identity: (E) -> ID,
    map: CalculationContext.(Writable<E>) -> W
) =
    WritableList<E, ID, W>(this, identity = identity, elementLens = { it.map(it) })

fun <E, ID> Writable<List<E>>.lensByElementWithIdentity(identity: (E) -> ID) =
    WritableListWithoutMap<E, ID>(this, identity = identity, elementLens = { it })

@JvmName("setLensByElementWithIdentity")
fun <E, ID, W> Writable<Set<E>>.lensByElementWithIdentity(
    identity: (E) -> ID,
    map: CalculationContext.(Writable<E>) -> W
) =
    lens("", get = { it.toList() }, set = { it.toSet() }).lensByElement(identity, map)

@JvmName("setLensByElementWithIdentity")
fun <E, ID> Writable<Set<E>>.lensByElementWithIdentity(identity: (E) -> ID) =
    lens("", get = { it.toList() }, set = { it.toSet() }).lensByElement(identity)

interface ListItemWritable<E> : ImmediateReadableWithWrite<E> {
    val index: ImmediateReadable<Int>
}

/**
 * THIS ONLY WORKS IF THE `set` on the receiver *never* manipulates the input before notifying.
 */
fun <E> Writable<List<E>>.lensByElementAssumingSetNeverManipulates(): Readable<List<ListItemWritable<E>>> =
    lensByElementAssumingSetNeverManipulates { it }

/**
 * THIS ONLY WORKS IF THE `set` on the receiver *never* manipulates the input before notifying.
 */
fun <E, W> Writable<List<E>>.lensByElementAssumingSetNeverManipulates(map: CalculationContext.(ListItemWritable<E>) -> W): Readable<List<W>> =
    LensByElementAssumingSetNeverManipulates(this, map)

private class LensByElementAssumingSetNeverManipulates<E, W>(
    val source: Writable<List<E>>,
    private val map: CalculationContext.(ListItemWritable<E>) -> W
) :
    Readable<List<W>>, BaseListenable() {

    inner class Instance(calculationContext: CalculationContext, index: Int, value: E) : ListItemWritable<E>, BaseImmediateReadable<E>(value) {
        val mapped = map(calculationContext, this)
        override val index: ImmediateReadable<Int> = Constant(index)
        override suspend fun set(value: E) {
            this.value = value
            source.set(sources.map { it.value })
        }
        override suspend fun updateFromLens(name: String, update: ReadableState<E>) {
            state = state.updateFromLens(name, update)
            source.updateFromLens(
                name,
                ReadableState.Invalid(
                    sources.map { it.value },
                    sources.mapNotNull { it.state.invalid?.summary }.toSet().joinToString()
                )
            )
        }
    }

    val sources: ArrayList<Instance> = ArrayList()
    var _state: ReadableState<List<W>> = ReadableState.NotReady
    private var myListen: (() -> Unit)? = null
    override fun activate() {
        super.activate()
        myListen = source.addListener {
            refresh()
            invokeAllListeners()
        }
        refresh()
    }

    override fun deactivate() {
        super.deactivate()
        myListen?.invoke()
        myListen = null
    }

    override val state: ReadableState<List<W>>
        get() {
            if (myListen == null) refresh()
            return _state
        }
    var suppress = false
    var old: CoroutineScope? = null

    fun refresh() {
        if (suppress) return
        old?.cancel()
        val context = CoroutineScope(Job())
        old = context
        _state = source.state.map {
            sources.clear()
            sources.addAll(it.mapIndexed { index, it -> Instance(context, index, it) })
            sources.map { it.mapped }
        }
    }
}

typealias WritableListWithoutMap<E, ID> = WritableList<E, ID, WritableList<E, ID, *>.ElementWritable>

class WritableList<E, ID, T>(
    val source: Writable<List<E>>,
    internal val log: Console? = null,
    val identity: (E) -> ID,
    val elementLens: (WritableList<E, ID, T>.ElementWritable) -> T
) : Readable<List<T>> {
    inner class ElementWritable internal constructor(valueInit: E) : Writable<E>, ImmediateReadable<E>,
        CalculationContext {
        private var job = Job()
        private val restOfContext = Dispatchers.Default + CoroutineExceptionHandler { coroutineContext, throwable ->
            if (throwable !is CancellationException) {
                throwable.report("WritableList.ElementWritable")
            }
        }
        override val coroutineContext get() = restOfContext + job

        internal var dead = false
            set(value) {
                field = value
                listeners.invokeAllSafe()
                job.cancel()
                job = Job()
            }
        var id: ID = identity(valueInit)
            private set
        private val listeners = ArrayList<() -> Unit>()
        override val state: ReadableState.Ready<E>
            get() = ReadableState(value)
        override var value: E = valueInit
            set(value) {
                if (field != value) {
                    id = identity(value)
                    field = value
                    listeners.invokeAllSafe()
                }
            }
        internal var queuedSet: ReadableState<E> = ReadableState.NotReady
        internal val queuedOrValue: E
            get() {
                val qs = queuedSet
                return if (qs.success) qs.get() else value
            }
        internal var usedFlag = false

        override suspend fun set(value: E) {
            queuedSet = ReadableState(value)
            val allWritables = elements.awaitOnce()
            val newList = allWritables.map { it.queuedOrValue }
            if (allWritables.contains(this)) {
                try {
                    source.set(newList)
                } finally {
                    queuedSet = ReadableState.NotReady
                }
                if (elements.myListen == null) {
                    this.value = value
                }
            }
        }

        override suspend fun updateFromLens(name: String, update: ReadableState<E>) {
            // TODO: Implement
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

    inner class Elements : Writable<List<ElementWritable>> {
        override suspend fun set(value: List<ElementWritable>) {
            source.set(value.map { it.queuedOrValue })
        }

        override suspend fun updateFromLens(name: String, update: ReadableState<List<ElementWritable>>) {
            // TODO: Implement
        }

        private var lastElements: List<ElementWritable> = listOf()
        private var _state: ReadableState<List<ElementWritable>> = ReadableState.NotReady
            set(value) {
                if (value.success) lastElements = value.get()
                field = value
            }
        override var state: ReadableState<List<ElementWritable>>
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
                val existing = lastElements.getOrNull(index)?.takeIf { it.id == identity(newElement) }
                    ?: lastElements.find { old -> !old.usedFlag && old.id == identity(newElement) }
                if (existing != null) {
                    existing.usedFlag = true
                    existing.value = newElement
                    existing
                } else {
                    newElement(newElement)
                }
            }
            lastElements.forEach { if (!it.usedFlag) it.dead = true }
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
