package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.Console
import com.lightningkite.kiteui.ConsoleRoot


/**
 * Essentially SharedReadable but can be set.
 *
 * @property stopListeningWhenOverridden When true, the LazyProperty stops listening to its initial value calculation
 * when set. It's recommended this be set `false` when the property is likely to be reset.
 *
 * @property useLastWhileLoading When true, the most recent set value or calculated result is used while new results
 * are being calculated
 * */
class LazyProperty<T>(
    private val stopListeningWhenOverridden: Boolean = true,
    private val useLastWhileLoading: Boolean = false,
    private val log: Console? = null,
    initialValue: ReactiveContext.() -> T
): ReadableWithImmediateWrite<T> {

    private val shared = SharedReadable(useLastWhileLoading = useLastWhileLoading, action = initialValue)

    private val listeners = ArrayList<() -> Unit>()
    override fun addListener(listener: () -> Unit): () -> Unit {
        log?.log("LazyProperty Adding listener")

        listeners.add(listener)

        if (!overridden && sharedRemover == null) startListeningToShared()

        return {
            val pos = listeners.indexOfFirst { it === listener }
            if (pos != -1) {
                listeners.removeAt(pos)
                shutdownIfNotNeeded()
            }
        }
    }

    override var state: ReadableState<T> = ReadableState.NotReady
        private set(value) {
            if(field != value) {
                field = value
                log?.log("LazyProperty: Informing ${listeners.size} listeners of new state $value")
                listeners.invokeAllSafe()
                shutdownIfNotNeeded()
            }
        }

    var overridden: Boolean = false
        private set

    private var sharedRemover: (() -> Unit)? = null

    private fun startListeningToShared() {
        log?.log("Starting listening to shared")
        sharedRemover = shared.addListener {
            log?.log("Shared sharing result with LazyProperty")
            if (!overridden) {
                state = shared.state
            }
        }
        val currentSharedState = shared.state
        if(!overridden && (!useLastWhileLoading || currentSharedState.ready)) state = shared.state
    }

    private fun stopListeningToShared() {
        sharedRemover?.invoke()
        sharedRemover = null
        log?.log("Stopped listening to shared")
    }
    private fun shutdownIfNotNeeded() {
        if (listeners.isNotEmpty()) return
        if (sharedRemover == null) return
        log?.log("LazyProperty shutting down shared behavior")
        stopListeningToShared()
        if (!useLastWhileLoading) state = ReadableState.NotReady
    }

    var value: T
        get() = state.get()
        set(value) {
            if (!overridden) {
                overridden = true
                if (stopListeningWhenOverridden) stopListeningToShared()
            }
            state = ReadableState(value)
        }

    override fun setImmediate(value: T) { this.value = value }

    /**
     * Resets the LazyProperty to the initial value calculation.
     *
     * If [stopListeningWhenOverridden] then the LazyProperty starts the shared readable again
     *
     * If [useLastWhileLoading] then the LazyProperty will continue to use the last set value until the
     * calculation is finished
     * */
    fun reset() {
        if (overridden) {
            overridden = false
            if (stopListeningWhenOverridden) startListeningToShared()

            val currentSharedState = shared.state
            if (!useLastWhileLoading || currentSharedState.ready) {
                state = currentSharedState
                // useLastWhileLoading = true: If shared is not ready, the state will remain as the previously set value until shared is ready.
            }
        }
    }
}