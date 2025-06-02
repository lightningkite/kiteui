package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.onMainThread
import com.lightningkite.readable.Property
import com.lightningkite.readable.Readable
import com.lightningkite.readable.StatusListener
import com.lightningkite.readable.addAndRunListener
import com.lightningkite.readable.onRemove
import kotlinx.coroutines.CoroutineScope

class WorkAndLoadTracker(
    val scope: CoroutineScope,
    val onException: (Exception, working: Boolean) -> (() -> Unit)?
) {
    val loading = Property(false)
    private var loadCount = 0
        set(value) {
            field = value
            if (value == 0 && loading.value) {
                loading.value = false
            } else if (value > 0 && !loading.value) {
                loading.value = true
            }
        }
    val working = Property(false)
    private var workCount = 0
        set(value) {
            field = value
            if (value == 0 && working.value) {
                working.value = false
            } else if (value > 0 && !working.value) {
                working.value = true
            }
        }


    internal fun listenForWorking(readable: Readable<*>): () -> Unit {
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

    internal fun listenForStatus(readable: Readable<*>): () -> Unit {
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


    val statusListener = object : StatusListener {
        override fun working(readable: Readable<*>) {
            listenForWorking(readable)
        }

        override fun loading(readable: Readable<*>) {
            listenForStatus(readable)
        }
    }
}