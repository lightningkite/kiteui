package com.lightningkite.kiteui.reactive

class Draft<T>(val published: Writable<T>): Writable<T> {
    constructor(initialValue: T) : this(Property(initialValue))
    constructor(initialValue: suspend CalculationContext.() -> T) : this(LazyProperty(initialValue = initialValue))

    val draft = LazyProperty(stopListeningWhenOverridden = false) { published() }

    override val state: ReadableState<T> get() = draft.state
    override fun addListener(listener: () -> Unit): () -> Unit = draft.addListener(listener)
    override suspend fun set(value: T) = draft.set(value)

    val changesMade = shared { draft() != published() }

    suspend fun publish() { published.set(draft.awaitOnce()) }
    fun cancel() { draft.reset() }
}
