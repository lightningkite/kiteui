package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.suspendCoroutineCancellable

object Never: Readable<Nothing> {
    override val state: ReadableState<Nothing> get() = ReadableState.NotReady
    override fun addListener(listener: () -> Unit): () -> Unit = {}
}