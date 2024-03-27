package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.Cancellable
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.delay
import com.lightningkite.kiteui.launchGlobal

data class DebounceReadable<T>(val source: Readable<T>, val milliseconds: Long) : Readable<T>, Listenable by DebounceListenable(source, milliseconds) {
    override val state: ReadableState<T> get() = source.state
}
data class DebounceListenable(val source: Listenable, val milliseconds: Long) : Listenable {
    private var changeCount = 0
    override fun addListener(listener: () -> Unit): () -> Unit {
        return source.addListener {
            val num = ++changeCount
            afterTimeout(milliseconds) {
                if (num == changeCount) listener()
            }
        }
    }
}

fun <T> Readable<T>.debounce(timeMs: Long): Readable<T> = DebounceReadable(this, timeMs)
fun Listenable.debounce(timeMs: Long): Listenable = DebounceListenable(this, timeMs)