package com.lightningkite.kiteui.reactive

class Draft<T>(val published: Writable<T>) {
    val draft = LazyProperty { published() }
    val changesMade = shared { draft() != published() }
    suspend fun publish() { published.set(draft.awaitOnce()) }
}
