package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.InternalKiteUi
import kotlin.jvm.JvmInline

@OptIn(InternalKiteUi::class)
@JvmInline
value class ReadableState<out T>(val raw: T) {
    inline val ready: Boolean get() = raw !is NotReady
    inline val success: Boolean get() = ready && raw !is ErrorState
    inline fun <R> onSuccess(action: (T)->R): R? {
        if(raw is NotReady) return null
        if(raw is ErrorState) return null
        return action(raw)
    }
    inline fun <R> onData(action: (T)->R): R? {
        val data = getOrNull() ?: return null
        return action(data)
    }
    inline val error: ErrorState? get() = raw as? ErrorState
    inline val exception: Exception? get() = (raw as? ErrorState.ThrownException)?.exception
    inline val invalid: ErrorState.Invalid<T>? get() = raw as? ErrorState.Invalid<T>

    inline fun get(): T {
        if(raw is NotReady) throw NotReadyException()
        if(raw is ErrorState) when (raw) {
            is ErrorState.HasDataAttached<*> -> raw.data as T
            is ErrorState.ThrownException -> throw raw.exception
        }
        return raw
    }
    inline fun getOrNull(): T? {
        if (raw is NotReady) return null
        if (raw is ErrorState) return when(raw) {
            is ErrorState.HasDataAttached<*> -> raw.data as? T
            else -> null
        }
        return raw
    }

    @Suppress("UNCHECKED_CAST")
    companion object {
        val notReady: ReadableState<Nothing> = ReadableState<Any?>(NotReady) as ReadableState<Nothing>
        fun <T> warning(data: T, summary: String, description: String = summary) = ReadableState<Any?>(ErrorState.Warning(data, summary, description)) as ReadableState<T>
        fun <T> invalid(data: T, summary: String, description: String = summary) = ReadableState<Any?>(ErrorState.Invalid(data, summary, description)) as ReadableState<T>
        fun <T> exception(exception: Exception) = ReadableState<Any?>(ErrorState.ThrownException(exception)) as ReadableState<T>
    inline val ready: Boolean get() = raw !is InternalReadableNotReady
    inline val success: Boolean get() = ready && raw !is InternalReadableThrownException
    inline fun <R> onSuccess(action: (T)->R): R? = handle(
        success = { action(it) },
        exception = { null },
        notReady = { null }
    )
    inline val exception: Exception? get() = (raw as? InternalReadableThrownException)?.exception
    fun get(): T = handle(
        success = { it },
        exception = { throw it },
        notReady = { throw NotReadyException() }
    )
    fun getOrNull(): T? = handle(
        success = { it },
        exception = { null },
        notReady = { null }
    )
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
        if(raw is NotReady) return this as ReadableState<B>
        if (raw is ErrorState) when (raw) {
            is ErrorState.HasDataAttached<*> -> {
                val t = raw.data as? T ?: return exception(ClassCastException("Raw data ${raw.data} could not be mapped to type T"))
                try {
                    val b = mapper(t)
                    return when (raw) {
                        is ErrorState.Warning<*> -> {
                            warning(b, raw.errorSummary, "Mapped from warning data ($t) -> ($b): " + raw.errorDescription)
                        }
                        is ErrorState.Invalid<*> -> {
                            invalid(b, raw.errorSummary, "Mapped from invalid data ($t) -> ($b): " + raw.errorDescription)
                        }
                    }
                } catch (e: Exception) {
                    return exception(e)
                }
            }
            else -> return this as ReadableState<B>
        }

        return try {
            ReadableState(mapper(raw))
        } catch(e: Exception) {
            exception(e)
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
            is InternalReadableWrapper<*> -> success(raw.other as T)
            else -> success(raw)
        }
    }
    fun asResult(): Result<T> = handle(success = { Result.success(it) }, exception = { Result.failure(it) }, notReady = { Result.failure(NotReadyException()) })

    override fun toString(): String = when(raw) {
        is InternalReadableNotReady -> "NotReady"
        is InternalReadableThrownException -> "ThrownException(${raw.exception})"
        is InternalReadableWrapper<*> -> "ReadyW($raw)"
        is NotReady -> "NotReady"
        is ErrorState.Warning<*> -> raw.toString()
        is ErrorState.Invalid<*> -> raw.toString()
        is ErrorState.ThrownException -> "ThrownException(${raw.exception})"
        else -> "Ready($raw)"
    }
}


class WarningException(val summary: String, val description: String = summary): Exception(description)
class InvalidException(val summary: String, val description: String = summary): Exception(description)

sealed interface ErrorState {
    enum class Severity { Low, Medium, High }
    val severity: Severity

    sealed interface HasDataAttached<out T>: ErrorState {
        val data: T
    }

    data class Warning<out T>(
        override val data: T,
        val errorSummary: String,
        val errorDescription: String = errorSummary
    ): HasDataAttached<T> {
        override val severity: Severity get() = Severity.Low
    }

    data class Invalid<out T>(
        override val data: T,
        val errorSummary: String,
        val errorDescription: String = errorSummary
    ): HasDataAttached<T> {
        override val severity: Severity get() = Severity.Medium
    }

    class ThrownException(val exception: Exception): ErrorState {
        override val severity: Severity get() = Severity.High
    }
}

object NotReady
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