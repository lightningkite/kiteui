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

@Deprecated("use the new name, lens, instead", ReplaceWith("lens", "com.lightningkite.kiteui.reactive.lensing.lens"))
fun <O, T> Writable<O>.map(
    get: (O) -> T,
    set: (O, T) -> O
): Writable<T> = lens(get, set)

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


private abstract class ValidationWritable<O, T>(
    val source: Writable<O>,
    val get: (O) -> T,  // NOTE: get cannot be used for validation
) : Writable<T>, Lens<O, T>(source) {
    protected val debug: Console? = null // ConsoleRoot.tag("ValWritable")

    abstract fun validate(model: O, value: T)

    private var previous: T? = null
    override fun updateFromSource(state: ReadableState<O>): ReadableState<T> =
        state.mapState {
            val value = get(it)
            readableStateWithValidation(value) {
                validate(it, value)
                previous = value
                value
            }
        }

    protected abstract suspend fun getSetValue(input: T): O
    override suspend fun set(value: T) {
        val invalidState = readableStateWithValidation(value) {
            val mapped = getSetValue(value)
            source.set(mapped)
            if (value == previous) state = ReadableState(value)
            return
        }
        debug?.log("validation error caught: ($value) -> ${invalidState.raw}")
        state = invalidState
    }
}

fun <O, T> Writable<O>.validationLens(
    get: (O) -> T,
    modify: (O, T) -> O
): Writable<T> =
    object : ValidationWritable<O, T>(this, get) {
        override fun validate(model: O, value: T) {
            modify(model, value)
        }
        override suspend fun getSetValue(input: T): O = modify(source.awaitOnce(), input)
    }

fun <O, T> Writable<O>.validationLens(
    get: (O) -> T,
    set: (T) -> O
): Writable<T> =
    object : ValidationWritable<O, T>(this, get) {
        override fun validate(model: O, value: T) {
            set(value)
        }
        override suspend fun getSetValue(input: T): O = set(input)
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
