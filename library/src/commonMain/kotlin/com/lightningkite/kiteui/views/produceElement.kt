package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.UnsafeModifier

@UnsafeModifier
public inline fun ElementWriter.produceAtMostOneUnsafe(action: ViewWriter.() -> Unit): Element? {
    var output: Element? = null
    val writer = object : ElementWriter by this, ViewWriter {
        @OverrideOnly
        override fun addChild(element: Element) {
            if (output != null) throw IllegalStateException("Produced more than one element at this layer, but only one was expected.")
            this@produceAtMostOneUnsafe.addChild(element)
            output = element
        }
    }
    action(writer)
    return output
}

@UnsafeModifier
public inline fun ElementWriter.produceExactlyOneUnsafe(action: ViewWriter.() -> Unit): Element =
    produceAtMostOneUnsafe(action) ?: throw IllegalStateException("Produced no elements at this layer, but expected one.")


public inline fun ViewWriter.produceAtMostOneView(action: ViewWriter.() -> Unit): Element? =
    @OptIn(UnsafeModifier::class)
    produceAtMostOneUnsafe(action)

public inline fun ViewWriter.produceExactlyOneView(action: ViewWriter.() -> Unit): Element =
    produceAtMostOneView(action) ?: throw IllegalStateException("Produced no elements at this layer, but expected one.")


public inline fun ElementWriter.produceAtMostOneElement(action: ElementWriter.() -> Unit): Element? =
    @OptIn(UnsafeModifier::class)
    produceAtMostOneUnsafe(action)

public inline fun ElementWriter.produceExactlyOneElement(action: ElementWriter.() -> Unit): Element =
    produceAtMostOneElement(action) ?: throw IllegalStateException("Produced no elements at this layer, but expected one.")
