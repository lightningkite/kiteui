package com.lightningkite.kiteui.reactive.mapping

import com.lightningkite.kiteui.reactive.*

fun <O, T> Writable<O>.map(
    get: (O) -> T,
    set: (O, T) -> O
): Writable<T> {
    return object : Writable<T> {
        private var _state: ReadableState<T> = ReadableState.notReady
        override var state: ReadableState<T>
            get() {
                @Suppress("UNCHECKED_CAST")
                if (myListen == null) _state = this@map.state.map { get(it) }
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
                myListen = this@map.addListener {
                    @Suppress("UNCHECKED_CAST")
                    state = this@map.state.map { get(it) }
                }
                state = this@map.state.map { get(it) }
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
            val old: O = this@map.await()
            val new: O = set(old, value)
            this@map.set(new)
        }
    }
}

