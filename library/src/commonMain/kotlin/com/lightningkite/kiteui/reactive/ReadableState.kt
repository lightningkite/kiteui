package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.InternalKiteUi
import kotlin.jvm.JvmInline

@OptIn(InternalKiteUi::class)
@JvmInline
value class ReadableState<out T>(val raw: T) {
    inline val ready: Boolean get() = raw !is InternalReadableNotReady
    inline val success: Boolean get() = ready && raw !is InternalReadableThrownException
    inline fun <R> onSuccess(action: (T)->R): R? {
        if(raw is InternalReadableNotReady) return null
        if(raw is InternalReadableThrownException) return null
        @Suppress("UNCHECKED_CAST")
        if(raw is InternalReadableWrapper<*>) return action(raw.other as T)
        return action(raw)
    }
    inline val exception: Exception? get() = (raw as? InternalReadableThrownException)?.exception
    inline fun get(): T {
        if(raw is InternalReadableNotReady) throw NotReadyException()
        if(raw is InternalReadableThrownException) throw raw.exception
        @Suppress("UNCHECKED_CAST")
        if(raw is InternalReadableWrapper<*>) return raw.other as T
        return raw
    }
    inline fun getOrNull(): T? {
        if(raw is InternalReadableNotReady) return null
        if(raw is InternalReadableThrownException) return null
        @Suppress("UNCHECKED_CAST")
        if(raw is InternalReadableWrapper<*>) return raw.other as T
        return raw
    }
    companion object {
        @Suppress("UNCHECKED_CAST")
        val notReady: ReadableState<Nothing> = ReadableState<Any?>(InternalReadableNotReady) as ReadableState<Nothing>
        @Suppress("UNCHECKED_CAST")
        fun <T> exception(exception: Exception) = ReadableState<Any?>(InternalReadableThrownException(exception)) as ReadableState<T>
        @Suppress("UNCHECKED_CAST")
        fun <T> wrap(value: T) = ReadableState<Any?>(InternalReadableWrapper(value)) as ReadableState<T>
    }
    @Suppress("UNCHECKED_CAST")
    inline fun <B> map(mapper: (T)->B): ReadableState<B> {
        if(raw is InternalReadableNotReady || raw is InternalReadableThrownException) return this as ReadableState<B>
        if(raw is InternalReadableWrapper<*>) try {
            return ReadableState(mapper(raw.other as T))
        } catch(e: Exception) {
            return exception(e)
        }
        try {
            return ReadableState(mapper(raw))
        } catch(e: Exception) {
            return exception(e)
        }
    }
    @Suppress("UNCHECKED_CAST")
    inline fun <R> handle(
        success: (T)->R,
        exception: (Exception)->R,
        notReady: ()->R
    ): R {
        return when(raw) {
            InternalReadableNotReady -> notReady()
            is InternalReadableThrownException -> exception(raw.exception)
            else -> success(raw)
        }
    }
    fun asResult(): Result<T> = handle(success = { Result.success(it) }, exception = { Result.failure(it) }, notReady = { Result.failure(NotReadyException()) })

    override fun toString(): String = when(raw) {
        is InternalReadableNotReady -> "NotReady"
        is InternalReadableThrownException -> "ThrownException(${raw.exception})"
        is InternalReadableWrapper<*> -> "ReadyW($raw)"
        else -> "Ready($raw)"
    }
}
@InternalKiteUi data class InternalReadableWrapper<T>(val other: T)
@InternalKiteUi data class InternalReadableThrownException(val exception: Exception)
@InternalKiteUi object InternalReadableNotReady

inline fun <T> readableState(action: () -> T): ReadableState<T> {
    return try {
        ReadableState(action())
    } catch (e: ReactiveLoading) {
        ReadableState.notReady
    } catch (e: Exception) {
        ReadableState.exception(e)
    }
}