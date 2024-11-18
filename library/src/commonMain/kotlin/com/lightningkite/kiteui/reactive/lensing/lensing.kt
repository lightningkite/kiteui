package com.lightningkite.kiteui.reactive.lensing

import com.lightningkite.kiteui.CancelledException
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

private abstract class ImmediateLens<O, T>(
    private val source: ImmediateReadable<O>,
    start: T
) : BaseImmediateReadable<T>(start) {
    private val debug: Console? = null

    protected abstract fun updateFromSource(state: ReadableState.Ready<O>): ReadableState.Ready<T>

    private var sourceListen: (() -> Unit)? = null

    override var state: ReadableState.Ready<T>
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
    private val source: Readable<O>,
    val get: (O) -> T
) : Lens<O, T>(source) {
    override fun hashCode(): Int = source.hashCode() + get.hashCode()
    override fun equals(other: Any?): Boolean = other === this
    override fun updateFromSource(state: ReadableState<O>): ReadableState<T> = state.map(get)
}

private open class BasicImmediateLens<O, T>(
    private val source: ImmediateReadable<O>,
    val get: (O) -> T
) : ImmediateLens<O, T>(source, get(source.value)) {
    override fun hashCode(): Int = source.hashCode() + get.hashCode()
    override fun equals(other: Any?): Boolean = other === this
    override fun updateFromSource(state: ReadableState.Ready<O>): ReadableState.Ready<T> = state.map(get)
}

fun <O, T> Readable<O>.lens(get: (O) -> T): Readable<T> = BasicLens(this, get)
fun <O, T> ImmediateReadable<O>.lens(get: (O) -> T): ImmediateReadable<T> = BasicImmediateLens(this, get)

private class SetLens<O,T>(
    val source: Writable<O>,
    val name: String,
    get: (O)->T,
    val setter: (T)->O
) : Writable<T>, BasicLens<O, T>(source, get) {

    /** Queues changes*/
    override suspend fun set(value: T) {
        val new: O = setter(value)
        source.updateFromLens(hashCode(), name, ReadableState(new))
    }

    override suspend fun updateFromLens(hash: Int, name: String?, update: ReadableState<T>) {
        if (update.ready) state = update
        source.updateFromLens(
            hash,
            name ?: this.name,
            update.map { setter(it) }
        )
    }
}

private class ModifyLens<O,T>(
    val source: Writable<O>,
    val name: String,
    get: (O)->T,
    val modify: (O, T)->O
) : Writable<T>, BasicLens<O, T>(source, get) {
    /** Queues changes*/
    override suspend fun set(value: T) {
        val old: O = source.awaitOnce()
        val new: O = modify(old, value)
        source.updateFromLens(hashCode(), name, ReadableState(new))
    }

    override suspend fun updateFromLens(hash: Int, name: String?, update: ReadableState<T>) {
        val old = source.awaitOnce()
        source.updateFromLens(
            hash,
            name ?: this.name,
            update.map { modify(old, it) }
        )
    }
}

private class ImmediateSetLens<O,T>(
    val source: ImmediateWritable<O>,
    val name: String,
    get: (O)->T,
    val setter: (T)->O
) : ImmediateWritable<T>, BasicImmediateLens<O, T>(source, get) {
    /** Queues changes*/
    override suspend fun set(value: T) {
        val new: O = setter(value)
        source.updateFromLens(hashCode(), name, ReadableState(new))
    }

    override suspend fun updateFromLens(hash: Int, name: String?, update: ReadableState<T>) {
        source.updateFromLens(
            hash,
            name ?: this.name,
            update.map { setter(it) }
        )
    }
}

private class ImmediateModifyLens<O,T>(
    val source: ImmediateWritable<O>,
    val name: String,
    get: (O)->T,
    val modify: (O, T)->O
) : ImmediateWritable<T>, BasicImmediateLens<O, T>(source, get) {
    /** Queues changes*/
    override suspend fun set(value: T) {
        val old: O = source.value
        val new: O = modify(old, value)
        source.updateFromLens(hashCode(), name, ReadableState(new))
    }

    override suspend fun updateFromLens(hash: Int, name: String?, update: ReadableState<T>) {
        val old = source.value
        source.updateFromLens(
            hash,
            name ?: this.name,
            update.map { modify(old, it) }
        )
    }
}

fun <O, T> Writable<O>.lens(name: String = "", get: (O) -> T, modify: (O, T) -> O): Writable<T> =
    ModifyLens(this, name, get, modify)

fun <O, T> Writable<O>.lens(name: String = "", get: (O) -> T, set: (T) -> O): Writable<T> =
    SetLens(this, name, get, set)

fun <O, T> ImmediateWritable<O>.lens(name: String = "", get: (O) -> T, modify: (O, T) -> O): ImmediateWritable<T> =
    ImmediateModifyLens(this, name, get, modify)

fun <O, T> ImmediateWritable<O>.lens(name: String = "", get: (O) -> T, set: (T) -> O): ImmediateWritable<T> =
    ImmediateSetLens(this, name, get, set)


// Validation lensing
class InvalidException(val summary: String, val description: String = summary): Exception(description)

inline fun <T> ReadableState.Companion.validating(data: T, action: () -> T): ReadableState<T> =
    try {
        ReadableState.Success(action())
    } catch (e: CancelledException) {
        ReadableState.NotReady
    } catch (e: ReactiveLoading) {
        ReadableState.NotReady
    } catch (e: InvalidException) {
        ReadableState.Invalid(data, e.summary, e.description)
    } catch (e: Exception) {
        ReadableState.Exception(e)
    }

private open class ValidationLens<T>(
    private val source: Readable<T>,
    private val vet: (T) -> T,
) : Lens<T, T>(source) {
    override fun hashCode(): Int = source.hashCode() + vet.hashCode()
    override fun equals(other: Any?): Boolean = other === this
    override fun updateFromSource(state: ReadableState<T>): ReadableState<T> =
        if (state is ReadableState.Ready)
            ReadableState.validating(state.value) { vet(state.value) }
        else
            state
}

fun <T> Readable<T>.vet(vetter: (T) -> T): Readable<T> = ValidationLens(this, vetter)
fun <T> Readable<T>.validate(validation: (T) -> String?) = vet {
    validation(it)?.let { message ->
        throw InvalidException(message)
    }
    it
}

private class WriteValidation<T>(
    val source: Writable<T>,
    val reportToSource: Boolean = true,
    private val vet: (T) -> T,
) : Writable<T>, ValidationLens<T>(source, vet) {
    override suspend fun updateFromLens(hash: Int, name: String?, update: ReadableState<T>) {
        state = update
        source.updateFromLens(hash, name, update)
    }

    override suspend fun set(value: T) {
        val updated = ReadableState.validating(value) { vet(value) }
        state = updated
        if (reportToSource) source.updateFromLens(hashCode(), null, updated)
    }
}

fun <T> Writable<T>.vet(reportToSource: Boolean = true, vetter: (T) -> T): Writable<T> =
    WriteValidation(this, reportToSource, vetter)

fun <T> Writable<T>.validate(reportToSource: Boolean = true, validation: (T) -> String?): Writable<T> =
    vet(reportToSource) { value ->
        validation(value)?.let { throw InvalidException(it) } ?: value
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
