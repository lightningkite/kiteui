package com.lightningkite.kiteui.reactive.mapping

import com.lightningkite.kiteui.reactive.*

fun <T, V> Writable<T>.transform(
    get: (T) -> V,
    set: (V) -> T,
): Writable<V> = map(
    get = { get(it) },
    set = { _, value -> set(value) }
)

fun <T, V> Writable<T>.dynamicTransform(
    get: suspend (T) -> V,
    set: suspend (V) -> T,
) = object: Writable<V> {
    val late = LateInitProperty<Unit>().apply { value = Unit }

    val shared = shared {
        late.await()
        get(this@dynamicTransform.await())
    }

    override val state: ReadableState<V> get() = shared.state
    override fun addListener(listener: () -> Unit): () -> Unit = shared.addListener(listener)

    fun setLoading() = late.unset()
    fun setComplete() { late.value = Unit }

    override suspend fun set(value: V) {
        setLoading()

        this@dynamicTransform set set(value)

        setComplete()
    }
}