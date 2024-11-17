package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.CancelledException
import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.reactive.lensing.InvalidException
import kotlin.jvm.JvmInline

@OptIn(InternalKiteUi::class)
sealed interface ReadableState<out T> {
    fun get(): T
    fun <R> map(mapper: (T)->R): ReadableState<R>

    data object NotReady : ReadableState<Nothing> {
        override fun get(): Nothing { throw NotReadyException() }
        override fun <R> map(mapper: (Nothing) -> R): ReadableState<Nothing> = NotReady
        override fun toString(): String = "NotReady"
    }

    sealed interface Ready<T> : ReadableState<T> {
        val value: T
        override fun get(): T = value
        override fun <R> map(mapper: (T)->R): Ready<R>
    }

    @JvmInline
    value class Success<T>(override val value: T) : Ready<T> {
        override fun toString(): String = "Success($value)"
        override fun <R> map(mapper: (T) -> R) = Success(mapper(value))
    }

    sealed interface Issue {
        val summary: String
        val description: String
    }

    data class Invalid<T>(
        override val value: T,
        override val summary: String,
        override val description: String = summary
    ) : Issue, Ready<T> {
        override fun <R> map(mapper: (T) -> R) = Invalid(mapper(value), summary, description)
    }

    @InternalKiteUi
    data class LensingIssues<T> private constructor(
        override val value: T,
        val lensIssues: Map<String, Issue>
    ) : Issue, Ready<T> {
        constructor(value: T, name: String, issue: Issue) : this(value, mapOf(name to issue))

        override val summary: String = lensIssues
            .asIterable()
            .joinToString(separator = "\n -") { (name, issue) ->
                "$name : ${issue.summary}"
            }
        override val description: String = lensIssues
            .asIterable()
            .joinToString(separator = "\n -") { (name, issue) ->
                "$name : ${issue.description}"
            }

        override fun <R> map(mapper: (T) -> R) = LensingIssues(mapper(value), lensIssues)

        fun updateLensIssue(updatedValue: T, name: String, issue: Issue): LensingIssues<T> =
            LensingIssues(
                updatedValue,
                lensIssues + Pair(name, issue)
            )

        fun removeLensIssue(updatedValue: T, name: String): Ready<T> {
            val updated = lensIssues - name
            return if (updated.isEmpty()) Success(updatedValue)
            else LensingIssues(updatedValue, updated)
        }
    }

    data class Exception(
        val exception: kotlin.Exception
    ) : Issue, ReadableState<Nothing> {
        override fun get(): Nothing { throw exception }
        override fun <R> map(mapper: (Nothing) -> R) = this
        override val summary: String
            get() = exception.message ?: "Exception Occurred: $exception"
        override val description: String
            get() = summary
    }

    companion object {
        inline operator fun <T> invoke(value: T) = Success(value)
    }
}

inline val ReadableState<*>.ready: Boolean get() = this is ReadableState.Ready
inline val ReadableState<*>.success: Boolean get() = this is ReadableState.Success

inline val <T> ReadableState<T>.invalid: ReadableState.Invalid<T>? get() = this as? ReadableState.Invalid<T>
inline val ReadableState<*>.issue: ReadableState.Issue? get() = this as? ReadableState.Issue
inline val ReadableState<*>.exception: Exception? get() = (this as? ReadableState.Exception)?.exception

inline fun <T, R> ReadableState<T>.handle(
    ready: (T)->R,
    exception: (Exception)->R,
    notReady: ()->R
): R = when (this) {
    is ReadableState.Ready -> ready(value)
    is ReadableState.Exception -> exception(this.exception)
    ReadableState.NotReady -> notReady()
}

inline fun <T, R> ReadableState<T>.handle(
    success: (T)->R,
    issue: (ReadableState.Issue)->R,
    notReady: ()->R
): R = when (this) {
    is ReadableState.Success -> success(value)
    is ReadableState.Issue -> issue(this)
    ReadableState.NotReady -> notReady()
}

inline fun <T, R> ReadableState<T>.onReady(action: (T)->R): R? =
    handle(
        ready = action,
        notReady = { null },
        exception = { null }
    )

inline fun <T> readableState(action: ()->T): ReadableState<T> =
    try {
        ReadableState.Success(action())
    } catch (e: CancelledException) {
        ReadableState.NotReady
    } catch (e: ReactiveLoading) {
        ReadableState.NotReady
    } catch (e: Exception) {
        ReadableState.Exception(e)
    }

inline fun <T> readableStateWithValidation(data: T, action: () -> T): ReadableState<T> =
    try {
        ReadableState.Success(action())
    } catch (e: CancelledException) {
        ReadableState.NotReady
    } catch (e: ReactiveLoading) {
        ReadableState.NotReady
    } catch (e: InvalidException) {
        ReadableState.Invalid(data, e.summary, e.description)
    } catch (e: Exception) {
        ReadableState.Exception(e)
    }

@OptIn(InternalKiteUi::class)
inline fun <T> ReadableState<T>.updateFromLens(name: String, updatedState: ReadableState<T>): ReadableState<T> =
    when(updatedState) {
        is ReadableState.Success -> {
            if (this is ReadableState.LensingIssues) removeLensIssue(updatedState.value, name)
            else updatedState
        }
        is ReadableState.Invalid -> {
            if (this is ReadableState.LensingIssues) updateLensIssue(updatedState.value, name, updatedState)
            else ReadableState.LensingIssues(updatedState.value, name, updatedState)
        }
        is ReadableState.LensingIssues -> {
            if (this is ReadableState.LensingIssues) updateLensIssue(updatedState.value, name, updatedState)
            else ReadableState.LensingIssues(updatedState.value, name, updatedState)
        }
        is ReadableState.Exception -> this
        ReadableState.NotReady -> this
    }

@OptIn(InternalKiteUi::class)
inline fun <T> ReadableState.Ready<T>.updateFromLens(name: String, updatedState: ReadableState<T>): ReadableState.Ready<T> =
    when(updatedState) {
        is ReadableState.Success -> {
            if (this is ReadableState.LensingIssues) removeLensIssue(updatedState.value, name)
            else updatedState
        }
        is ReadableState.Invalid -> {
            if (this is ReadableState.LensingIssues) updateLensIssue(updatedState.value, name, updatedState)
            else ReadableState.LensingIssues(updatedState.value, name, updatedState)
        }
        is ReadableState.LensingIssues -> {
            if (this is ReadableState.LensingIssues) updateLensIssue(updatedState.value, name, updatedState)
            else ReadableState.LensingIssues(updatedState.value, name, updatedState)
        }
        is ReadableState.Exception -> this
        ReadableState.NotReady -> this
    }

fun <T> ReadableState<T>.asResult(): Result<T> = handle(
    ready = { Result.success(it) },
    exception = { Result.failure(it) },
    notReady = { Result.failure(NotReadyException()) }
)

fun <T> Result<T>.toReadableStateV2(): ReadableState<T> =
    if (isFailure) ReadableState.Exception(exceptionOrNull() as Exception)
    else ReadableState.Success(getOrNull()!!)