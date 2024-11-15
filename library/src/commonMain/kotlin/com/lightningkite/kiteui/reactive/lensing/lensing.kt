package com.lightningkite.kiteui.reactive.lensing

import com.lightningkite.kiteui.Console
import com.lightningkite.kiteui.reactive.*

private abstract class Lens<O, T>(
    private val source: Readable<O>
) : BaseReadable<T>() {
    private val debug: Console? = null

    protected abstract fun updateFromSource(state: ReadableState<O>): ReadableState<T>

    private var sourceListen: (() -> Unit)? = null

    override var state: ReadableState<T>
        get() {
            if (sourceListen == null) super.state = updateFromSource(source.state)
            return super.state
        }
        set(value) {
            super.state = value
        }

    override fun activate() {
        debug?.log("activating")
        sourceListen = source.addListener {
            state = updateFromSource(source.state)
        }
        if (!state.ready) state = updateFromSource(source.state)
    }

    override fun deactivate() {
        debug?.log("deactivating")
        sourceListen?.invoke()
        sourceListen = null
    }
}

// Basic lensing

private open class BasicLens<O, T>(
    source: Readable<O>,
    private val get: (O) -> T
) : Lens<O, T>(source) {
    override fun updateFromSource(state: ReadableState<O>): ReadableState<T> = state.map(get)
}

fun <O, T> Readable<O>.lens(get: (O) -> T): Readable<T> = BasicLens(this, get)

fun <O, T> Writable<O>.lens(
    get: (O) -> T,
    modify: (O, T) -> O
): Writable<T> {
    return object : Writable<T>, BasicLens<O, T>(this, get) {
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
    return object : Writable<T>, BasicLens<O, T>(this, get) {
        /** Queues changes */
        override suspend fun set(value: T) {
            val new: O = set(value)
            this@lens.set(new)
        }
    }
}

// Validation lensing

private class ValidationReadable<T>(
    source: Readable<T>,
    private val vet: (T) -> T,
) : Lens<T, T>(source) {
    override fun updateFromSource(state: ReadableState<T>): ReadableState<T> =
        state.mapState {
            readableStateWithValidation(it) { vet(it) }
        }
}

fun <T> Readable<T>.vet(vetter: (T) -> T): Readable<T> = ValidationReadable(this, vetter)
fun <T> Readable<T>.validate(validation: (T) -> String?) = vet {
    validation(it)?.let { message ->
        throw InvalidException(message)
    }
    it
}

private abstract class ValidationLens<T>(
    val source: Writable<T>,
    val reportToSource: Boolean = true,
    private val vet: (T) -> T,
) : Writable<T>, Lens<T, T>(source) {
    override fun updateFromSource(state: ReadableState<T>): ReadableState<T> =
        state.mapState {
            readableStateWithValidation(it) { vet(it) }
        }


}


// Reactive lensing
fun <O, T> Writable<O>.dynamicLens(
    get: ReactiveContext.(O) -> T,
    modify: suspend (O, T) -> O
): Writable<T> = shared {
    get(this@dynamicLens.invoke())
}.withWrite {
    this@dynamicLens.set(modify(this@dynamicLens(), it))
}

fun <O, T> Writable<O>.dynamicLens(
    get: ReactiveContext.(O) -> T,
    set: suspend (T) -> O
): Writable<T> = shared {
    get(this@dynamicLens.invoke())
}.withWrite {
    this@dynamicLens.set(set(it))
}
