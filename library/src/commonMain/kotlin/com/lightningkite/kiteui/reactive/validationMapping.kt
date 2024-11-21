package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.Console
import com.lightningkite.kiteui.printStackTrace2
import com.lightningkite.kiteui.report
import kotlinx.coroutines.*
import kotlin.coroutines.cancellation.CancellationException
import kotlin.jvm.JvmName


private open class ModifyValidationLens<O, T>(
    val source: Writable<O>,
    val get: (O) -> T,
    val check: (O) -> Unit,
    val modify: (O, T) -> O,
) :
    BaseReadable<T>(), Writable<T> {

    private var lastParentState: ReadableState<O>? = null
    override var state: ReadableState<T>
        get() {
            if (myListen == null && super.state != lastParentState) super.state = source.state.map(get)
            return super.state
        }
        set(_) = TODO()

    private var myListen: (() -> Unit)? = null

    override fun activate() {
        super.activate()
        super.state = source.state.map(get)
        myListen = source.addListener {
            lastParentState = source.state
//            source.state.handle(
//                success = { super.state = ReadableState(get(it)) },
//                exception = {},
//                notReady = { super.state = ReadableState.notReady }
//            )
            super.state = source.state.map(get)
//            source.state.onSuccess { check(it) }
        }
//        source.state.onSuccess { check(it) }
    }

    override fun deactivate() {
        super.deactivate()
        myListen?.invoke()
        myListen = null
    }

    override suspend fun set(value: T) {
        super.state = ReadableState(value)
        val v = modify(source.awaitOnce(), value)
        check(v)
        source.set(v)
    }
}

fun <O, T> Writable<O>.validationLens(
    get: (O) -> T,
    check: (O) -> Unit,
    modify: (O, T) -> O
): Writable<T> = ModifyValidationLens(this, get, check, modify)
