package com.lightningkite.kiteui.reactive.lensing

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.reactive.ReadableState.Companion.exception
import com.lightningkite.kiteui.reactive.ReadableState.Companion.invalid
import com.lightningkite.kiteui.reactive.ReadableState.Companion.warning

private open class ReadableView<O, T>(
    private val source: Readable<O>,
    private val get: (O) -> T
): Readable<T> {
    protected var _state: ReadableState<T> = ReadableState.notReady
    override var state: ReadableState<T>
        get() {
            if (myListen == null) _state = source.state.map { get(it) }
            return _state
        }
        protected set(value) {
            if (_state != value) {
                _state = value
                listeners.invokeAllSafe()
            }
        }

    private val listeners = ArrayList<() -> Unit>()
    private var myListen: (() -> Unit)? = null
    override fun addListener(listener: () -> Unit): () -> Unit {
        listeners.add(listener)
        if (listeners.size == 1) {
            myListen = source.addListener {
                state = source.state.map { get(it) }
            }
            state = source.state.map { get(it) }
        }
        return {
            listeners.remove(listener)
            if (listeners.isEmpty()) {
                myListen?.invoke()
                myListen = null
            }
        }
    }
}


@Deprecated("use the new name, lens, instead", ReplaceWith("lens", "com.lightningkite.kiteui.reactive.lensing.lens"))
fun <O, T> Writable<O>.map(
    get: (O) -> T,
    set: (O, T) -> O
): Writable<T> = lens(get, set)

// Basic lensing

fun <O, T> Readable<O>.lens(get: (O) -> T): Readable<T> = ReadableView(this, get)

fun <O, T> Writable<O>.lens(
    get: (O) -> T,
    modify: (O, T) -> O
): Writable<T> {
    return object : Writable<T>, ReadableView<O, T>(this, get) {
        /** Queues changes*/
        override suspend fun set(value: T) {
            val old: O = this@lens.await()
            val new: O = modify(old, value)
            this@lens.set(new)
        }
    }
}

fun <O, T> Writable<O>.lens(
    get: (O) -> T,
    set: (T) -> O
): Writable<T> {
    return object : Writable<T>, ReadableView<O, T>(this, get) {
        /** Queues changes */
        override suspend fun set(value: T) {
            val new: O = set(value)
            this@lens.set(new)
        }
    }
}


// Validation lensing
class WarningException(val summary: String, val description: String = summary): Exception()
class InvalidException(val summary: String, val description: String = summary): Exception()

private open class ValidatedReadableView<O, T>(
    private val source: Readable<O>,
    private val get: (O) -> T,
    private val vet: (T) -> T
): Readable<T> {
    @Suppress("UNCHECKED_CAST")
    protected inline fun ReadableState<O>.mapValidation(mapper: (T)->T): ReadableState<T> {
        if(raw is NotReady) return this as ReadableState<T>
        if (raw is ErrorState) when (raw) {
            is ErrorState.HasDataAttached<*> -> {
                val t = raw.data as? T ?: return exception(ClassCastException("Raw data ${raw.data} could not be mapped to type T"))
                try {
                    val b = mapper(t)
                    return when (raw) {
                        is ErrorState.Warning<*> -> {
                            warning(b, raw.errorSummary, "Mapped from warning data ($t) -> ($b): " + raw.errorDescription)
                        }
                        is ErrorState.Invalid<*> -> {
                            invalid(b, raw.errorSummary, "Mapped from invalid data ($t) -> ($b): " + raw.errorDescription)
                        }
                    }
                } catch (e: Exception) {
                    return exception(e)
                }
            }
            else -> return this as ReadableState<B>
        }

        return try {
            ReadableState(mapper(raw))
        } catch(e: Exception) {
            exception(e)
        }
    }

    var _state: ReadableState<T> = ReadableState.notReady
        set(value) {

        }
    override var state: ReadableState<T>
        get() {
            if (myListen == null) _state = source.state.map { get(it) }
            return _state
        }
        protected set(value) {
            if (_state != value) {
                _state = value
                listeners.invokeAllSafe()
            }
        }

    private val listeners = ArrayList<() -> Unit>()
    private var myListen: (() -> Unit)? = null
    override fun addListener(listener: () -> Unit): () -> Unit {
        listeners.add(listener)
        if (listeners.size == 1) {
            myListen = source.addListener {
                state = source.state.map { get(it) }
            }
            state = source.state.map { get(it) }
        }
        return {
            listeners.remove(listener)
            if (listeners.isEmpty()) {
                myListen?.invoke()
                myListen = null
            }
        }
    }
}

fun <O, T> Writable<O>.validationLens(
    get: (O) -> T,
    modify: (model: O, value: T) -> O
): Writable<T> {
    return object : Writable<T>, ReadableView<O, T>(this, get) {
        inline fun validate(old: O, value: T, action: (O) -> Unit = {}) {
            try {
                val new = modify(old, value)
                action(new)
            } catch (e: WarningException) {
                state = ReadableState.warning(value, e.summary, e.description)
            } catch (e: InvalidException) {
                state = ReadableState.invalid(value, e.summary, e.description)
            } catch (e: Exception) {
                state = ReadableState.exception(e)
            }
        }

        override suspend fun set(value: T) {
            val old = this@validationLens.await()
            validate(old, value) { this@validationLens.set(it) }
        }

        init {
            val state = this@validationLens.state
            if (state.success)
        }
    }
}

fun <O, T> Writable<O>.validationLens(
    get: (O) -> T,
    set: (T) -> O
): Writable<T> {
    return object : Writable<T>, ReadableView<O, T>(this, get) {
        override suspend fun set(value: T) {
            try {
                val new = set(value)
                this@validationLens.set(new)
            } catch (e: WarningException) {
                state = ReadableState.warning(value, e.summary, e.description)
            } catch (e: InvalidException) {
                state = ReadableState.invalid(value, e.summary, e.description)
            } catch (e: Exception) {
                state = ReadableState.exception(e)
            }
        }
    }
}

// Reactive lensing

