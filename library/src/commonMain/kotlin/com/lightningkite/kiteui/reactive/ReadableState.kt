package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.CancelledException
import com.lightningkite.kiteui.InternalKiteUi
import kotlin.jvm.JvmInline

@OptIn(InternalKiteUi::class)
@JvmInline
value class ReadableState<out T>(val raw: T) {
    inline val ready: Boolean get() = raw !is InternalReadableNotReady
    inline val success: Boolean get() = ready && raw !is ErrorState
    inline fun get(): T = handle(
        data = { it },
        exception = { throw it },
        notReady = { throw NotReadyException() }
    )
    inline fun getOrNull(): T? = handle(
        data = { it },
        exception = { null },
        notReady = { null }
    )
    inline fun <R> onSuccess(action: (T)->R): R? = handle(
        success = { action(it) },
        error = { null },
        notReady = { null }
    )
    inline fun <R> onData(action: (T)->R): R? = getOrNull()?.let(action)

    inline val error: ErrorState? get() = raw as? ErrorState
    inline val exception: Exception? get() = (raw as? ErrorState.ThrownException)?.exception
    inline val invalid: ErrorState.Invalid<out T>? get() = raw as? ErrorState.Invalid<T>
    inline val warning: ErrorState.Warning<out T>? get() = raw as? ErrorState.Warning<T>

    @Suppress("UNCHECKED_CAST")
    companion object {
        val notReady: ReadableState<Nothing> = ReadableState<Any?>(InternalReadableNotReady) as ReadableState<Nothing>
        fun <T> wrap(value: T) = ReadableState<Any?>(InternalReadableWrapper(value)) as ReadableState<T>

        fun <T> warning(data: T, summary: String, description: String = summary) =
            ReadableState<Any?>(ErrorState.Warning(data, summary, description)) as ReadableState<T>

        fun <T> invalid(data: T, summary: String, description: String = summary) =
            ReadableState<Any?>(ErrorState.Invalid(data, summary, description)) as ReadableState<T>

        fun <T> exception(exception: Exception) =
            if (exception is CancelledException) notReady
            else ReadableState<Any?>(ErrorState.ThrownException(exception)) as ReadableState<T>
    }

    inline fun <B> map(mapper: (T)->B): ReadableState<B> {
        return handle(
            data = { readableState { mapper(it) } },
            exception = { exception(it) },
            notReady = { notReady }
        )
    }
    inline fun <B> mapState(mapper: (T)->ReadableState<B>): ReadableState<B> {
        return handle(
            data = {
                try {
                    mapper(it)
                } catch (e: Exception) {
                    exception(e)
                }
             },
            exception = { exception(it) },
            notReady = { notReady }
        )
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <R> handle(
        data: (T)->R,
        exception: (Exception)->R,
        notReady: ()->R
    ): R {
        return when(raw) {
            InternalReadableNotReady -> notReady()
            is ErrorState.ThrownException -> exception(raw.exception)
            is InternalReadableWrapper<*> -> data(raw.other as T)
            is ErrorState.MetaData<*> -> data(raw.data as T)
            else -> data(raw)
        }
    }
    inline fun <R> handle(
        success: (T)->R,
        error: (ErrorState)->R,
        notReady: ()->R
    ): R {
        return when(raw) {
            InternalReadableNotReady -> notReady()
            is ErrorState -> error(raw)
            is InternalReadableWrapper<*> -> success(raw.other as T)
            else -> success(raw)
        }
    }
    fun asResult(): Result<T> = handle(data = { Result.success(it) }, exception = { Result.failure(it) }, notReady = { Result.failure(NotReadyException()) })

    override fun toString(): String = when(raw) {
        is InternalReadableNotReady -> "NotReady"
        is ErrorState.ThrownException -> "ThrownException(${raw.exception})"
        is ErrorState.Warning<*> -> raw.toString()
        is ErrorState.Invalid<*> -> raw.toString()
        is InternalReadableWrapper<*> -> "ReadyW($raw)"
        else -> "Ready($raw)"
    }
}

@OptIn(InternalKiteUi::class)
inline fun <T> resolveLensState(
    original: ReadableState<T>,
    new: ReadableState<T>,
    lensName: String,
): ReadableState<T> =
    try {
        new.handle<ReadableState<T>>(
            success = { data ->
                if (original.raw is ErrorState.FromLensing<*>) {
                    (original.raw as ErrorState.FromLensing<T>)
                        .removeError(lensName, data)
                        ?.let { ReadableState(it) as ReadableState<T> }
                        ?: ReadableState(data)
                } else
                    ReadableState(data)
            },
            error = { error ->
                if (error !is ErrorState.MetaData<*>) return@handle original
                if (original.raw is ErrorState.FromLensing<*>) {
                    (original.raw as ErrorState.FromLensing<T>)
                        .addError(lensName, error as ErrorState.MetaData<T>)
                        .let { ReadableState(it) as ReadableState<T> }
                }
                else ReadableState(ErrorState.FromLensing(lensName, error)) as ReadableState<T>
            },
            notReady = { original }
        )
    } catch (e: Exception) {
        ReadableState.exception(e)
    }

class WarningException(val summary: String, val description: String = summary) : Exception(description)
class InvalidException(val summary: String, val description: String = summary) : Exception(description)


sealed interface ErrorState {
    enum class Severity { Low, Medium, High }
    val severity: Severity
    val errorSummary: String
    val errorDescription: String

    sealed interface MetaData<T> : ErrorState {
        val data: T
    }

    data class Warning<T>(
        override val data: T,
        override val errorSummary: String,
        override val errorDescription: String = errorSummary
    ): MetaData<T> {
        override val severity: Severity get() = Severity.Low
    }

    data class Invalid<T>(
        override val data: T,
        override val errorSummary: String,
        override val errorDescription: String = errorSummary
    ): MetaData<T> {
        override val severity: Severity get() = Severity.Medium
    }

    @InternalKiteUi class FromLensing<T> private constructor(
        override val data: T,
        val errors: Map<String, MetaData<T>>
    ): MetaData<T> {
        constructor(name: String, error: MetaData<T>) : this(error.data, mapOf(name to error))

        override val severity: Severity get() =
            errors.values.fold(Severity.Low) { acc, it -> if (it.severity > acc) it.severity else acc }

        override val errorSummary: String =
            errors.asIterable().joinToString(separator = "\n- ") { (name, state) -> "$name : ${state.errorSummary}" }

        override val errorDescription: String =
            errors.asIterable().joinToString(separator = "\n- ") { (name, state) -> "$name : ${state.errorDescription}" }

        fun addError(name: String, error: MetaData<T>): FromLensing<T> =
            FromLensing(
                error.data,
                errors + (name to error)
            )

        fun removeError(name: String, data: T): FromLensing<T>? {
            val updated = errors - name
            return if (updated.isEmpty()) null
            else FromLensing(data, updated)
        }
    }

    @InternalKiteUi class ThrownException(val exception: Exception): ErrorState {
        override val severity: Severity get() = Severity.High

        override val errorSummary: String get() = exception.message ?: "Encountered an Exception: $exception"
        override val errorDescription: String get() = errorSummary
    }
}

@InternalKiteUi data class InternalReadableWrapper<T>(val other: T)
@InternalKiteUi object InternalReadableNotReady

inline fun <T> readableState(action: () -> T): ReadableState<T> {
    return try {
        ReadableState(action())
    } catch (e: CancelledException) {
        ReadableState.notReady
    } catch (e: ReactiveLoading) {
        ReadableState.notReady
    } catch (e: Exception) {
        ReadableState.exception(e)
    }
}
inline fun <T> readableStateWithValidation(data: T, action: () -> T): ReadableState<T> {
    return try {
        ReadableState(action())
    } catch (e: WarningException) {
        ReadableState.warning(data, e.summary, e.description)
    } catch (e: InvalidException) {
        ReadableState.invalid(data, e.summary, e.description)
    } catch (e: ReactiveLoading) {
        ReadableState.notReady
    } catch (e: Exception) {
        ReadableState.exception(e)
    }
}

inline fun <T> Result<T>.toReadableState(): ReadableState<T> {
    @Suppress("UNCHECKED_CAST")
    return if(this.isFailure) ReadableState.exception(this.exceptionOrNull() as Exception)
    else ReadableState.wrap(this.getOrNull() as T)
}