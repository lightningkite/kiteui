package com.lightningkite.kiteui.reactive

import kotlin.jvm.JvmInline

@JvmInline
value class ReadableState<out T>(val raw: T) {
    inline val ready: Boolean get() = raw !is NotReady
    inline val success: Boolean get() = ready && raw !is ThrownException
    inline fun <R> onSuccess(action: (T)->R): R? {
        if(raw is NotReady) return null
        if(raw is ThrownException) return null
        return action(raw)
    }
    inline val exception: Exception? get() = (raw as? ThrownException)?.exception
    inline fun get(): T {
        if(raw is NotReady) throw NotReadyException()
        if(raw is ThrownException) throw raw.exception
        return raw
    }
    companion object {
        @Suppress("UNCHECKED_CAST")
        val notReady: ReadableState<Nothing> = ReadableState<Any?>(NotReady) as ReadableState<Nothing>
        @Suppress("UNCHECKED_CAST")
        fun <T> exception(exception: Exception) = ReadableState<Any?>(ThrownException(exception)) as ReadableState<T>
    }
    @Suppress("UNCHECKED_CAST")
    inline fun <B> map(mapper: (T)->B): ReadableState<B> {
        if(raw is NotReady || raw is ThrownException) return this as ReadableState<B>
        try {
            return ReadableState(mapper(raw))
        } catch(e: Exception) {
            return exception(e)
        }
    }

    override fun toString(): String = when(raw) {
        is NotReady -> "NotReady"
        is ThrownException -> "ThrownException(${raw.exception})"
        else -> "Ready($raw)"
    }
}
class ThrownException(val exception: Exception)
object NotReady