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
            var last: V? = null
            addEventListener(eventName) {
                val v = get(this@vprop)
                if(last != v) {
                    last = v
                    invokeAllListeners()
                }
            }
        }

        override var value: V
            get() = get(this@vprop)
            set(value) {
                if(get(this@vprop) != value) {
                    set(this@vprop, value)
                    invokeAllListeners()
                }
            }
        override suspend fun set(value: V) {
            if(get(this@vprop) != value) {
                set(this@vprop, value)
                invokeAllListeners()
            }
        }
    }
}
