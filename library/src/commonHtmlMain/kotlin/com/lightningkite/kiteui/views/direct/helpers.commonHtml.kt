package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.Event
import com.lightningkite.signal.*
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
            set(value) {
                set(this@vprop, value)
                invokeAllListeners()
            }
        override suspend fun set(value: V) {
            set(this@vprop, value)
            invokeAllListeners()
        }
    }
}
fun <V> HtmlElementLike.vread(
    eventName: String,
    get: HtmlElementLike.() -> V
): Readable<V> {
    return object : ImmediateReadable<V>, BaseListenable() {
        init {
            addEventListener(eventName) {
                invokeAllListeners()
            }
        }

        override val value: V
            get() = get(this@vread)
    }
}

fun HtmlElementLike.vevent(eventName: String): Listenable {
    return object: BaseListenable() {
        init {
            addEventListener(eventName) {
                invokeAllListeners()
            }
        }
    }
}

expect fun HtmlElementLike.resizeObserver(): Listenable
expect fun HtmlElementLike.mutationObserver(recursive: Boolean): Listenable
