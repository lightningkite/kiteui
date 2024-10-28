package com.lightningkite.kiteui.reactive

class Draft<T> private constructor(
    val published: Writable<T>,
    private val draft: LazyProperty<T>
): ReadableWithImmediateWrite<T> by draft {
    constructor(published: Writable<T>) : this(published, LazyProperty(stopListeningWhenOverridden = false) { published() })
    constructor(initialValue: ReactiveContext.() -> T) : this(
        LazyProperty(
            useLastWhileLoading = true,
            initialValue = initialValue
        )
    )
    constructor(initialValue: T) : this(Property(initialValue))

    val changesMade = shared { draft() != published() }

    suspend fun publish(): T {
        published.set(draft.awaitOnce())
        draft.reset()
        return awaitOnce()
    }
    fun cancel() { draft.reset() }

    override suspend fun reportSetException(exception: Exception) { draft.reportSetException(exception) }
    override suspend fun set(value: T) { draft.setImmediate(value) }
}