@file:MustUseReturnValues

package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.OverrideOnly

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


private class WithChildContext(parent: ElementWriter) : ViewWriter, ElementWriter by parent {
    override val context: ElementContext = parent.context.child()
}

/** Returns a writer whose next element gets its own [child][ElementContext.child] context. Values it sets stay inside it. */
public fun ElementWriter.withChildContext(): ElementWriter = WithChildContext(this)
/** Returns a writer whose next element gets its own [child][ElementContext.child] context. Values it sets stay inside it. */
public fun ElementWriter.CanAddScrolling.withChildContext(): ElementWriter.CanAddScrolling = WithChildContext(this)
/** Returns a writer whose next element gets its own [child][ElementContext.child] context. Values it sets stay inside it. */
public fun ElementWriter.CanAddSizing.withChildContext(): ElementWriter.CanAddSizing = WithChildContext(this)
/** Returns a writer whose next element gets its own [child][ElementContext.child] context. Values it sets stay inside it. */
public fun ElementWriter.CanAddTheme.withChildContext(): ElementWriter.CanAddTheme = WithChildContext(this)
/** Returns a writer whose next element gets its own [child][ElementContext.child] context. Values it sets stay inside it. */
public fun ElementWriter.CanAddShownWhen.withChildContext(): ElementWriter.CanAddShownWhen = WithChildContext(this)
/** Returns a writer whose next element gets its own [child][ElementContext.child] context. Values it sets stay inside it. */
public fun ElementWriter.CanAddWeight.withChildContext(): ElementWriter.CanAddWeight = WithChildContext(this)
/** Returns a writer whose next element gets its own [child][ElementContext.child] context. Values it sets stay inside it. */
public fun ElementWriter.CanAddAlignment.withChildContext(): ElementWriter.CanAddAlignment = WithChildContext(this)
/** Returns a writer whose next element gets its own [child][ElementContext.child] context. Values it sets stay inside it. */
public fun ViewWriter.withChildContext(): ViewWriter = WithChildContext(this)


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