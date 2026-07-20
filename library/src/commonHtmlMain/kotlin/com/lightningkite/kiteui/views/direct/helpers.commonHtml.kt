package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.FutureElement
import com.lightningkite.reactive.core.*

public fun <V> FutureElement.vprop(
    eventName: String,
    get: FutureElement.() -> V,
    set: FutureElement.(V) -> Unit
): MutableReactiveValue<V> {
    return object : MutableReactiveValue<V>, BaseListenable() {
        init {
            this@vprop.addEventListener(eventName) {
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
public fun <V> FutureElement.vread(
    eventName: String,
    get: FutureElement.() -> V
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

public fun FutureElement.vevent(eventName: String): Listenable {
    return object: BaseListenable() {
        init {
            addEventListener(eventName) {
                invokeAllListeners()
            }
        }
    }
}

public expect fun FutureElement.resizeObserver(): Listenable
public expect fun FutureElement.mutationObserver(recursive: Boolean): Listenable
