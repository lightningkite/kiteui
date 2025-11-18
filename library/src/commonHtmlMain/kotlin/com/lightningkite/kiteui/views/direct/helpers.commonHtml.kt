package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.Event
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.HtmlElementLike
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

fun <V> HtmlElementLike.vprop(
    eventName: String,
    get: HtmlElementLike.() -> V,
    set: HtmlElementLike.(V) -> Unit
): MutableReactiveValue<V> {
    return object : MutableReactiveValue<V>, BaseListenable() {
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
    }
}
fun <V> HtmlElementLike.vread(
    eventName: String,
    get: HtmlElementLike.() -> V
): Reactive<V> {
    return object : ReactiveValue<V>, BaseListenable() {
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
