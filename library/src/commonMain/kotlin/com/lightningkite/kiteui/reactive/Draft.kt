package com.lightningkite.kiteui.reactive

class Draft<T> private constructor(
    val published: Writable<T>,
    private val draft: LazyProperty<T>
): Writable<T> by draft {
    constructor(published: Writable<T>) : this(published, LazyProperty(stopListeningWhenOverridden = false) { published() })
    constructor(initialValue: T) : this(Property(initialValue))
    constructor(initialValue: suspend CalculationContext.() -> T) : this(LazyProperty(initialValue = initialValue))

    val changesMade = shared { draft() != published() }

    suspend fun publish() { published.set(draft.awaitOnce()) }
    fun cancel() { draft.reset() }
}