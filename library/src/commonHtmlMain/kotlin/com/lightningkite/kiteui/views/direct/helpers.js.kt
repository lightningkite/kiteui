package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.Event
import com.lightningkite.kiteui.reactive.BaseListenable
import com.lightningkite.kiteui.reactive.BasicListenable
import com.lightningkite.kiteui.reactive.ReadableState
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.HtmlElementLike

fun <V> HtmlElementLike.vprop(
    eventName: String,
    get: HtmlElementLike.() -> V,
    set: HtmlElementLike.(V) -> Unit
): Writable<V> {
    return object : Writable<V>, BaseListenable() {
        init {
            events[eventName] = { invokeAll() }
        }
        override val state: ReadableState<V> get() = ReadableState(get(this@vprop))
        override suspend fun set(value: V) {
            set(this@vprop, value)
        }
    }
}