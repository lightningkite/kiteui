package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.dom.Event
import com.lightningkite.signal.*
import com.lightningkite.kiteui.views.HtmlElementLike

@InternalKiteUi
public fun <V> HtmlElementLike.vprop(
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

        public override var value: V
            get() = get(this@vprop)
            set(value) {
                set(this@vprop, value)
                invokeAllListeners()
            }
        public override suspend fun set(value: V) {
            set(this@vprop, value)
            invokeAllListeners()
        }
    }
}
@InternalKiteUi
public fun <V> HtmlElementLike.vread(
    eventName: String,
    get: HtmlElementLike.() -> V
): Readable<V> {
    return object : ImmediateReadable<V>, BaseListenable() {
        init {
            addEventListener(eventName) {
                invokeAllListeners()
            }
        }

        public override val value: V
            get() = get(this@vread)
    }
}

@InternalKiteUi
public fun HtmlElementLike.vevent(eventName: String): Listenable {
    return object: BaseListenable() {
        init {
            addEventListener(eventName) {
                invokeAllListeners()
            }
        }
    }
}

public expect fun HtmlElementLike.resizeObserver(): Listenable
public expect fun HtmlElementLike.mutationObserver(recursive: Boolean): Listenable
