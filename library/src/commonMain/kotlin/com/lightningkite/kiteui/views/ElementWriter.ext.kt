package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.UnsafeModifier

private class BeforeSetup(
    val wraps: ElementWriter,
    val action: Element.() -> Unit
) : ViewWriter, ElementWriter by wraps {
    @OverrideOnly
    override fun willAddChild(element: Element) {
        wraps.willAddChild(element)
        action(element)
    }
}

public fun ElementWriter.beforeSetup(setup: Element.() -> Unit): ElementWriter = BeforeSetup(this, setup)
public fun ElementWriter.CanAddScrolling.beforeSetup(setup: Element.() -> Unit): ElementWriter.CanAddScrolling = BeforeSetup(this, setup)
public fun ElementWriter.CanAddTheme.beforeSetup(setup: Element.() -> Unit): ElementWriter.CanAddTheme = BeforeSetup(this, setup)
public fun ElementWriter.CanAddSizing.beforeSetup(setup: Element.() -> Unit): ElementWriter.CanAddSizing = BeforeSetup(this, setup)
public fun ElementWriter.CanAddShownWhen.beforeSetup(setup: Element.() -> Unit): ElementWriter.CanAddShownWhen = BeforeSetup(this, setup)
public fun ElementWriter.CanAddWeight.beforeSetup(setup: Element.() -> Unit): ElementWriter.CanAddWeight = BeforeSetup(this, setup)
public fun ElementWriter.CanAddAlignment.beforeSetup(setup: Element.() -> Unit): ElementWriter.CanAddAlignment = BeforeSetup(this, setup)
public fun ViewWriter.beforeSetup(setup: Element.() -> Unit): ViewWriter = BeforeSetup(this, setup)


private class Split(parent: ElementWriter) : ViewWriter, ElementWriter by parent {
    override val context: ElementContext = parent.context.split()
}

public fun ElementWriter.split(): ElementWriter = Split(this)
public fun ElementWriter.CanAddScrolling.split(): ElementWriter.CanAddScrolling = Split(this)
public fun ElementWriter.CanAddSizing.split(): ElementWriter.CanAddSizing = Split(this)
public fun ElementWriter.CanAddTheme.split(): ElementWriter.CanAddTheme = Split(this)
public fun ElementWriter.CanAddShownWhen.split(): ElementWriter.CanAddShownWhen = Split(this)
public fun ElementWriter.CanAddWeight.split(): ElementWriter.CanAddWeight = Split(this)
public fun ElementWriter.CanAddAlignment.split(): ElementWriter.CanAddAlignment = Split(this)
public fun ViewWriter.split(): ViewWriter = Split(this)


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


/**
 * Creates a [ViewWriter] which creates and writes the [inject] element each time a new element is created,
 * then uses the [inject] to write the element.
 *
 * Used by modifiers which add hidden elements to function. This way hidden elements are lazy, they are not
 * added until the writer is used, and they can be re-applied on different elements.
 * */
@InternalKiteUi
public inline fun <T : ContainerElement> ElementWriter.lazyInjectModifierWriter(crossinline setup: T.() -> Unit = {}, crossinline inject: () -> T): ViewWriter =
    object : ViewWriter, ElementWriter by this {
        var current: T? = null

        @OverrideOnly
        override fun willAddChild(element: Element) {
            val e = this@lazyInjectModifierWriter.write(inject(), setup)
            current = e
            e.willAddChild(element)
        }

        @OverrideOnly
        override fun addChild(element: Element) {
            current?.addChild(element) ?: throw IllegalStateException("addChild called on $element before willAddChild!")
        }
    }