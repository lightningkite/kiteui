package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.Event
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.HtmlElementLike

fun <V> HtmlElementLike.vprop(
    eventName: String,
    get: HtmlElementLike.() -> V,
    set: HtmlElementLike.(V) -> Unit
): ImmediateWritable<V> {
    return object : ImmediateWritable<V>, BaseListenable() {
        init {
            addEventListener(eventName) {
                invokeAllListeners()
            }
        }

        override var value: V
            get() = get(this@vprop)
            set(value) { set(this@vprop, value) }
        override suspend fun set(value: V) {
            set(this@vprop, value)
            invokeAllListeners()
        }
    }
}
