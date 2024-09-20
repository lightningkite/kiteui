package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.launchGlobal
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class DebounceReadable<T>(val source: Readable<T>, val duration: Duration) : Readable<T>, Listenable by DebounceListenable(source, duration) {
    override val state: ReadableState<T> get() = source.state
}
data class DebounceListenable(val source: Listenable, val duration: Duration) : Listenable {
    private var changeCount = 0
    override fun addListener(listener: () -> Unit): () -> Unit {
        return source.addListener {
            val num = ++changeCount
            afterTimeout(duration.inWholeMilliseconds) {
                if (num == changeCount) listener()
            }
        }
    }
}

fun <T> Readable<T>.debounce(timeMs: Long): Readable<T> = DebounceReadable(this, timeMs.milliseconds)
fun <T> Readable<T>.debounce(duration: Duration): Readable<T> = DebounceReadable(this, duration)
fun Listenable.debounce(timeMs: Long): Listenable = DebounceListenable(this, timeMs.milliseconds)
fun Listenable.debounce(duration: Duration): Listenable = DebounceListenable(this, duration)

fun <T> Writable<T>.debounceWrite(duration: Duration): Writable<T> = object: Writable<T> by this {
    var setIndex = 0
    override suspend fun set(value: T) {
        val mine = ++setIndex
        delay(duration)
        if(mine == setIndex) this@debounceWrite.set(value)
    }
}