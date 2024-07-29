package com.lightningkite.kiteui.reactive

interface ResourceUse {
    fun start(): () -> Unit
}

interface Listenable : ResourceUse {
    /**
     * Adds the [listener] to be called every time this event fires.
     * @return a function to remove the [listener] that was added.  Removing multiple times should not cause issues.
     */
    fun addListener(listener: () -> Unit): () -> Unit
    override fun start(): () -> Unit = addListener { }
}

interface Readable<out T> : Listenable {
    val state: ReadableState<T>
}

interface Writable<T> : Readable<T> {
    suspend infix fun set(value: T)
}

interface ImmediateReadable<out T> : Readable<T> {
    val value: T
    override val state: ReadableState<T> get() = ReadableState(value)
}

interface ImmediateWritable<T> : Writable<T>, ImmediateReadable<T> {
    override var value: T
    override suspend fun set(value: T) {
        this.value = value
    }
}

class NotReadyException(message: String? = null) : IllegalStateException(message)

