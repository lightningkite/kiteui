package com.lightningkite.kiteui.reactive.mapping

import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.reactive.shared

suspend fun <E, ID> WritableList<E, ID>.remove(element: Readable<WritableList<E, ID>.ElementWritable>) {
    remove(element())
}

suspend fun <E, ID> WritableList<E, ID>.filter(
    predicate: (WritableList<E, ID>.ElementWritable) -> Boolean
): Readable<List<WritableList<E, ID>.ElementWritable>> =
    shared { await().filter(predicate) }

suspend fun <E, ID, T> WritableList<E, ID>.map(
    transform: (WritableList<E, ID>.ElementWritable) -> T
): Readable<List<T>> =
    shared { await().map(transform) }