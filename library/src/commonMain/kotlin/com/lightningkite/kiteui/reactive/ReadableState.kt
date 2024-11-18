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
        val lensIssues: Map<Int, Pair<String?, Issue>>
    ) : Issue, Ready<T> {
        constructor(value: T, hash: Int, name: String?, issue: Issue) : this(value, mapOf(hash to Pair(name, issue)))

        override val summary: String = lensIssues
            .values
            .joinToString(separator = "\n -") { (name, issue) ->
                if (name == null) issue.summary
                else "$name : ${issue.summary}"
            }
        override val description: String = lensIssues
            .values
            .joinToString(separator = "\n -") { (name, issue) ->
                if (name == null) issue.description
                else "$name : ${issue.description}"
            }

        override fun <R> map(mapper: (T) -> R) = LensingIssues(mapper(value), lensIssues)

        fun updateLensIssue(updatedValue: T, hash: Int, name: String?, issue: Issue): LensingIssues<T> =
            LensingIssues(
                updatedValue,
                lensIssues + (hash to Pair(name, issue))
            )

        fun removeLensIssue(updatedValue: T, hash: Int): Ready<T> {
            val updated = lensIssues - hash
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
        inline operator fun <T> invoke(action: ()->T): ReadableState<T> =
            try {
                Success(action())
            } catch (e: CancelledException) {
                NotReady
            } catch (e: ReactiveLoading) {
                NotReady
            } catch (e: kotlin.Exception) {
                Exception(e)
            }
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

@OptIn(InternalKiteUi::class)
inline fun <T> ReadableState<T>.updateFromLens(hash: Int, name: String?, updatedState: ReadableState<T>): ReadableState<T> =
    when(updatedState) {
        is ReadableState.Success -> {
            if (this is ReadableState.LensingIssues) removeLensIssue(updatedState.value, hash)
            else updatedState
        }
        is ReadableState.Invalid -> {
            if (this is ReadableState.LensingIssues) updateLensIssue(updatedState.value, hash, name, updatedState)
            else ReadableState.LensingIssues(updatedState.value, hash, name, updatedState)
        }
        is ReadableState.LensingIssues -> {
            if (this is ReadableState.LensingIssues) updateLensIssue(updatedState.value, hash, name, updatedState)
            else ReadableState.LensingIssues(updatedState.value, hash, name, updatedState)
        }
        is ReadableState.Exception -> this
        ReadableState.NotReady -> this
    }

@OptIn(InternalKiteUi::class)
inline fun <T> ReadableState.Ready<T>.updateFromLens(hash: Int, name: String?, updatedState: ReadableState<T>): ReadableState.Ready<T> =
    when(updatedState) {
        is ReadableState.Success -> {
            if (this is ReadableState.LensingIssues) removeLensIssue(updatedState.value, hash)
            else updatedState
        }
        is ReadableState.Invalid -> {
            if (this is ReadableState.LensingIssues) updateLensIssue(updatedState.value, hash, name, updatedState)
            else ReadableState.LensingIssues(updatedState.value, hash, name, updatedState)
        }
        is ReadableState.LensingIssues -> {
            if (this is ReadableState.LensingIssues) updateLensIssue(updatedState.value, hash, name, updatedState)
            else ReadableState.LensingIssues(updatedState.value, hash, name, updatedState)
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