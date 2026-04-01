package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.UnsafeModifier

private inline fun ElementWriter.beforeSetupImpl(crossinline setup: Element.() -> Unit): ViewWriter =
    object : ViewWriter, ElementWriter by this {
        @OverrideOnly
        override fun willAddChild(element: Element) {
            this@beforeSetupImpl.willAddChild(element)
            setup(element)
        }
    }

fun ElementWriter.beforeSetup(setup: Element.() -> Unit): ElementWriter = beforeSetupImpl(setup)
fun ElementWriter.CanAddScrolling.beforeSetup(setup: Element.() -> Unit): ElementWriter.CanAddScrolling = beforeSetupImpl(setup)
fun ElementWriter.CanAddTheme.beforeSetup(setup: Element.() -> Unit): ElementWriter.CanAddTheme = beforeSetupImpl(setup)
fun ElementWriter.CanAddSizing.beforeSetup(setup: Element.() -> Unit): ElementWriter.CanAddSizing = beforeSetupImpl(setup)
fun ElementWriter.CanAddShownWhen.beforeSetup(setup: Element.() -> Unit): ElementWriter.CanAddShownWhen = beforeSetupImpl(setup)
fun ElementWriter.CanAddWeight.beforeSetup(setup: Element.() -> Unit): ElementWriter.CanAddWeight = beforeSetupImpl(setup)
fun ElementWriter.CanAddAlignment.beforeSetup(setup: Element.() -> Unit): ElementWriter.CanAddAlignment = beforeSetupImpl(setup)
fun ViewWriter.beforeSetup(setup: Element.() -> Unit): ViewWriter = beforeSetupImpl(setup)


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


@UnsafeModifier
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

@UnsafeModifier
inline fun ElementWriter.produceExactlyOneUnsafe(action: ViewWriter.() -> Unit): Element =
    produceAtMostOneUnsafe(action) ?: throw IllegalStateException("Produced no views at this layer, but expected one.")


inline fun ViewWriter.produceAtMostOneView(action: ViewWriter.() -> Unit): Element? =
    @OptIn(UnsafeModifier::class)
    produceAtMostOneUnsafe(action)

inline fun ViewWriter.produceExactlyOneView(action: ViewWriter.() -> Unit): Element =
    produceAtMostOneView(action) ?: throw IllegalStateException("Produced no views at this layer, but expected one.")


inline fun ElementWriter.produceAtMostOne(action: ElementWriter.() -> Unit): Element? =
    @OptIn(UnsafeModifier::class)
    produceAtMostOneUnsafe(action)

inline fun ElementWriter.produceExactlyOne(action: ElementWriter.() -> Unit): Element =
    produceAtMostOne(action) ?: throw IllegalStateException("Produced no views at this layer, but expected one.")