package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.FutureElement
import com.lightningkite.reactive.core.*

/**
 * Exposes a DOM property as a [MutableReactiveValue], notifying listeners when the user changes it
 * (via [eventName]) and when it is written programmatically.
 *
 * Writing a value the element already holds is a no-op: no DOM write and no notification. That
 * matches Android and iOS, where the equivalent setters compare before notifying, so a checkbox or
 * toggle does not report a change it did not make and a two-way binding cannot feed itself.
 *
 * The comparison is trustworthy because [get] reads the live DOM *property* (`element.value`,
 * `element.checked`), not the HTML attribute. The property tracks what the user actually sees, so
 * after typing into a field the guard compares against the typed text and a write that resets the
 * field still goes through. Comparing against the attribute would not work: the attribute keeps its
 * original value once the user edits, and the reset would be skipped as a redundant write.
 *
 * Where the element normalizes what it stores - a range input clamping to min/max, a number input
 * rejecting non-numeric text - [get] returns the normalized value, so a write of the un-normalized
 * value still differs and is applied.
 */
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

        private fun assign(value: V) {
            if (get(this@vprop) == value) return
            set(this@vprop, value)
            invokeAllListeners()
        }

        override var value: V
            get() = get(this@vprop)
            set(value) = assign(value)
        override suspend fun set(value: V) = assign(value)
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
