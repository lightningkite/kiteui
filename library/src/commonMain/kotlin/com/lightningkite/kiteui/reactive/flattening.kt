package com.lightningkite.kiteui.reactive

import kotlin.js.JsName
import kotlin.jvm.JvmName

fun <T> Readable<Readable<T>>.flatten(): Readable<T> = shared { this@flatten.await().await() }

fun <T> Readable<Writable<T>>.flatten(): Writable<T> = shared { this@flatten()() }.withWrite { this@flatten() set it }

fun <T, V> Readable<Iterable<T>>.map(transform: (T) -> V): Readable<List<V>> = shared {
    this@map.await().map { transform(it) }
}

fun <T, V> Readable<Iterable<T>>.map(onRefresh: () -> Unit, transform: (T) -> V): Readable<List<V>> = shared {
    onRefresh.invoke()
    this@map.await().map { transform(it) }
}

fun <K, V> Readable<Iterable<K>>.associateWith(valueSelector: (K) -> V): Readable<Map<K, V>> = shared {
    this@associateWith.await().associateWith(valueSelector)
}

fun <K, V> Readable<Iterable<V>>.associateBy(keySelector: (V) -> K): Readable<Map<K, V>> = shared {
    this@associateBy.await().associateBy(keySelector)
}

fun <T> Readable<Collection<T>>.toList(): Readable<List<T>> = shared { this@toList.await().toList() }

@JvmName("readableMapToList")
@JsName("readableMapToList")
fun <K, V> Readable<Map<K, V>>.toList(): Readable<List<Pair<K, V>>> = shared { this@toList.await().toList() }

operator fun <A, B> Readable<Pair<A, B>>.component1(): Readable<A> = shared { this@component1.await().first }
operator fun <A, B> Readable<Pair<A, B>>.component2(): Readable<B> = shared { this@component2.await().second }