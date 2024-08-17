package com.lightningkite.kiteui.reactive.lensing

import com.lightningkite.kiteui.reactive.*

private open class ReadableView<O, T>(
    private val source: Readable<O>,
    private val get: (O) -> T
): Readable<T> {
    private var _state: ReadableState<T> = ReadableState.notReady
    final override var state: ReadableState<T>
        get() {
            if (myListen == null) _state = source.state.map { get(it) }
            return _state
        }
        set(value) {
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

fun <O, T> Readable<O>.lens(get: (O) -> T): Readable<T> = ReadableView(this, get)

@Deprecated("use the new name, lens, instead", ReplaceWith("lens", "com.lightningkite.kiteui.reactive.lensing.lens"))
fun <O, T> Writable<O>.map(
    get: (O) -> T,
    set: (O, T) -> O
): Writable<T> = lens(get, set)

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


// Validation lenses

fun <O, T> Writable<O>.validationLens(
    get: (O) -> T,
    modify: (O, T) -> O
): Writable<T> {
    return object : Writable<T>, ReadableView<O, T>(this, get) {
        override suspend fun set(value: T) {
            val old = this@validationLens.await()
            try {
                val new = modify(old, value)
                this@validationLens.set(new)
            } catch (e: ErrorState.Invalid) {
                state = ReadableState.error(e)
            } catch (e: Exception) {
                state = ReadableState.exception(e)
            }
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
            } catch (e: ErrorState.Invalid) {
                state = ReadableState.error(e)
            } catch (e: Exception) {
                state = ReadableState.exception(e)
            }
        }
    }
}