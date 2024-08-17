package com.lightningkite.kiteui.reactive

import kotlin.Error
import kotlin.jvm.JvmInline

@JvmInline
value class ReadableState<out T>(val raw: T) {
    inline val ready: Boolean get() = raw !is NotReady
    inline val success: Boolean get() = ready && raw !is ErrorState
    inline fun <R> onSuccess(action: (T)->R): R? {
        if(raw is NotReady) return null
        if(raw is ErrorState) return null
        return action(raw)
    }
    inline val error: ErrorState? get() = raw as? ErrorState
    inline val exception: Exception? get() = (raw as? ErrorState.ThrownException)?.exception
    inline val invalid: ErrorState.Invalid? get() = raw as? ErrorState.Invalid

    inline fun get(): T {
        if(raw is NotReady) throw NotReadyException()
        if(raw is ErrorState) when (raw) {
            is ErrorState.ThrownException -> throw raw.exception
            is ErrorState.HasDataAttached -> raw.data as T
        }
        return raw
    }
    inline fun dataOrNull(): T? {
        if (raw is NotReady) return null
        if (raw is ErrorState) return when(raw) {
            is ErrorState.HasDataAttached -> raw.data as? T
            else -> null
        }
        return raw
    }
    inline fun getOrNull(): T? {
        if(raw is NotReady) return null
        if(raw is ErrorState) return null
        return raw
    }
    @Suppress("UNCHECKED_CAST")
    companion object {
        val notReady: ReadableState<Nothing> = ReadableState<Any?>(NotReady) as ReadableState<Nothing>
        fun <T> exception(exception: Exception) = ReadableState<Any?>(ErrorState.ThrownException(exception)) as ReadableState<T>
        fun <T> invalid(data: T, summary: String, description: String = summary) = ReadableState<Any?>(ErrorState.Invalid(data, summary, description)) as ReadableState<T>
        fun <T> error(error: ErrorState) = ReadableState<Any?>(error) as ReadableState<T>
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <B> map(mapper: (T)->B): ReadableState<B> {
        if(raw is NotReady) return this as ReadableState<B>
        if (raw is ErrorState) when (raw) {
            is ErrorState.Invalid -> {
                val t = raw.data as? T ?: return exception(ClassCastException())
                return try {
                    invalid(mapper(t), raw.errorSummary , "mapped from invalid data [$t]: ${raw.errorDescription}" )
                } catch (e: Exception) {
                    exception(e)
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

    override fun toString(): String = when(raw) {
        is NotReady -> "NotReady"
        is ErrorState.ThrownException -> "ThrownException(${raw.exception})"
        is ErrorState.Invalid -> "Invalid(${raw.errorSummary}, ${raw.errorDescription}"
        else -> "Ready($raw)"
    }
}

sealed interface ErrorState {
    enum class Severity { Low, Medium, High }

    val severity: Severity

    sealed interface HasDataAttached: ErrorState {
        val data: Any?
    }

    class ThrownException(val exception: Exception): ErrorState {
        override val severity: Severity get() = Severity.High
    }

    class Invalid(
        override val data: Any?,
        val errorSummary: String,
        val errorDescription: String = errorSummary
    ): HasDataAttached, Exception() {
        override val severity: Severity get() = Severity.Medium
    }
}

object NotReady