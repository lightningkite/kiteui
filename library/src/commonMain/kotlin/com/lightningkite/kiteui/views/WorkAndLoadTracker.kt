package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.onMainThread
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.coroutines.CoroutineScope

public class WorkAndLoadTracker(
    public val scope: CoroutineScope,
    public val onException: (Exception, working: Boolean) -> (() -> Unit)?
) {
    public val loading: Signal<Boolean> = Signal(false)
    private var loadCount = 0
        set(value) {
            field = value
            if (value == 0 && loading.value) {
                loading.value = false
            } else if (value > 0 && !loading.value) {
                loading.value = true
            }
        }
    public val working: Signal<Boolean> = Signal(false)
    private var workCount = 0
        set(value) {
            field = value
            if (value == 0 && working.value) {
                working.value = false
            } else if (value > 0 && !working.value) {
                working.value = true
            }
        }


    internal fun listenForWorking(readable: Reactive<*>): () -> Unit {
        var loading = false
        var excEnder: (() -> Unit)? = null
        val r = readable.addAndRunListener {
            onMainThread {
                val s = readable.state
                if (loading != !s.ready) {
                    if (s.ready) {
                        workCount--
                    } else {
                        workCount++
                    }
                    loading = !s.ready
                }
                excEnder?.invoke()
                s.exception?.let { onException(it, true)?.let { excEnder = it } }
            }
        }
        scope.onRemove(r)
        return r
    }

    internal fun listenForStatus(readable: Reactive<*>): () -> Unit {
        var loading = false
        var excEnder: (() -> Unit)? = null
        val r = readable.addAndRunListener {
            onMainThread {
                val s = readable.state
                if (loading != !s.ready) {
                    if (s.ready) {
                        loadCount--
                    } else {
                        loadCount++
                    }
                    loading = !s.ready
                }
                excEnder?.invoke()
                s.exception?.let { onException(it, false)?.let { excEnder = it } }
            }
        }
        scope.onRemove(r)
        return r
    }


    public val statusListener: StatusListener = object : StatusListener {
        override fun working(readable: Reactive<*>) {
            listenForWorking(readable)
        }

        override fun loading(readable: Reactive<*>) {
            listenForStatus(readable)
        }
    }
}