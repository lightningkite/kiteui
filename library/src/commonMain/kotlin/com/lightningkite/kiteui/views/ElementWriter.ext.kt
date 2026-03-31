package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.UnsafeModifierOrdering

private class BeforeSetup(
    val wraps: ElementWriter,
    val action: Element.() -> Unit
) : ViewWriter, ElementWriter by wraps {
    @OptIn(OverrideOnly::class)
    override fun willAddChild(element: Element) {
        wraps.willAddChild(element)
        action(element)
    }
}

fun ElementWriter.beforeSetup(setup: Element.() -> Unit): ElementWriter = BeforeSetup(this, setup)
fun ElementWriter.CanAddScrolling.beforeSetup(setup: Element.() -> Unit): ElementWriter.CanAddScrolling = BeforeSetup(this, setup)
fun ElementWriter.CanAddDynamicTheme.beforeSetup(setup: Element.() -> Unit): ElementWriter.CanAddDynamicTheme = BeforeSetup(this, setup)
fun ElementWriter.CanAddTheme.beforeSetup(setup: Element.() -> Unit): ElementWriter.CanAddTheme = BeforeSetup(this, setup)
fun ElementWriter.CanAddSizing.beforeSetup(setup: Element.() -> Unit): ElementWriter.CanAddSizing = BeforeSetup(this, setup)
fun ElementWriter.CanAddShownWhen.beforeSetup(setup: Element.() -> Unit): ElementWriter.CanAddShownWhen = BeforeSetup(this, setup)
fun ElementWriter.CanAddWeight.beforeSetup(setup: Element.() -> Unit): ElementWriter.CanAddWeight = BeforeSetup(this, setup)
fun ElementWriter.CanAddAlignment.beforeSetup(setup: Element.() -> Unit): ElementWriter.CanAddAlignment = BeforeSetup(this, setup)
fun ViewWriter.beforeSetup(setup: Element.() -> Unit): ViewWriter = BeforeSetup(this, setup)


private class Split(parent: ElementWriter) : ViewWriter, ElementWriter by parent {
    override val context: ElementContext = parent.context.split()
}

fun ElementWriter.split(): ElementWriter = Split(this)
fun ElementWriter.CanAddScrolling.split(): ElementWriter.CanAddScrolling = Split(this)
fun ElementWriter.CanAddSizing.split(): ElementWriter.CanAddSizing = Split(this)
fun ElementWriter.CanAddTheme.split(): ElementWriter.CanAddTheme = Split(this)
fun ElementWriter.CanAddShownWhen.split(): ElementWriter.CanAddShownWhen = Split(this)
fun ElementWriter.CanAddWeight.split(): ElementWriter.CanAddWeight = Split(this)
fun ElementWriter.CanAddAlignment.split(): ElementWriter.CanAddAlignment = Split(this)
fun ViewWriter.split(): ViewWriter = Split(this)


@UnsafeModifierOrdering
inline fun ElementWriter.produceAtMostOneUnsafe(action: ViewWriter.() -> Unit): Element? {
    var output: Element? = null
    @OptIn(OverrideOnly::class)
    val writer = object : ElementWriter by this, ViewWriter {
        override fun addChild(element: Element) {
            if (output != null) throw IllegalStateException("Produced more than one view at this layer, but only one was expected.")
            this@produceAtMostOneUnsafe.addChild(element)
            output = element
        }
    }
    action(writer)
    return output
}

@UnsafeModifierOrdering
inline fun ElementWriter.produceExactlyOneUnsafe(action: ViewWriter.() -> Unit): Element =
    produceAtMostOneUnsafe(action) ?: throw IllegalStateException("Produced no views at this layer, but expected one.")


inline fun ViewWriter.produceAtMostOneView(action: ViewWriter.() -> Unit): Element? =
    @OptIn(UnsafeModifierOrdering::class)
    produceAtMostOneUnsafe(action)

inline fun ViewWriter.produceExactlyOneView(action: ViewWriter.() -> Unit): Element =
    produceAtMostOneView(action) ?: throw IllegalStateException("Produced no views at this layer, but expected one.")


inline fun ElementWriter.produceAtMostOne(action: ElementWriter.() -> Unit): Element? =
    @OptIn(UnsafeModifierOrdering::class)
    produceAtMostOneUnsafe(action)

inline fun ElementWriter.produceExactlyOne(action: ElementWriter.() -> Unit): Element =
    produceAtMostOne(action) ?: throw IllegalStateException("Produced no views at this layer, but expected one.")