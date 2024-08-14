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
    inline val error: ErrorState? get() = if (raw !is ErrorState) null else raw
    inline val exception: Exception? get() = (raw as? ErrorState.ThrownException)?.exception
    inline val invalid: ErrorState.Invalid? get() = raw as? ErrorState.Invalid

    inline fun get(): T {
        if(raw is NotReady) throw NotReadyException()
        if(raw is ErrorState) when (raw) {
            is ErrorState.ThrownException -> throw raw.exception
            is ErrorState.Invalid -> throw raw
        }
        return raw
    }
    inline fun getOrNull(): T? {
        if(raw is NotReady) return null
        if(raw is ErrorState) return null
        return raw
    }
    companion object {
        @Suppress("UNCHECKED_CAST")
        val notReady: ReadableState<Nothing> = ReadableState<Any?>(NotReady) as ReadableState<Nothing>
        @Suppress("UNCHECKED_CAST")
        fun <T> exception(exception: Exception) = ReadableState<Any?>(ErrorState.ThrownException(exception)) as ReadableState<T>
        @Suppress("UNCHECKED_CAST")
        fun <T> invalid(summary: String, description: String = summary) = ReadableState<Any?>(ErrorState.Invalid(summary, description)) as ReadableState<T>
    }
    @Suppress("UNCHECKED_CAST")
    inline fun <B> map(mapper: (T)->B): ReadableState<B> {
        if(raw is NotReady || raw is ErrorState) return this as ReadableState<B>
        try {
            return ReadableState(mapper(raw))
        } catch(e: Exception) {
            return exception(e)
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

    class ThrownException(val exception: Exception): ErrorState {
        override val severity: Severity get() = Severity.High
    }

    class Invalid(val errorSummary: String, val errorDescription: String = errorSummary): ErrorState, Throwable() {
        override val severity: Severity get() = Severity.Medium
    }
}

object NotReady