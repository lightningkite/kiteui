package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.ThemeDerivation
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface ElementWriter {
    val context: ElementContext
    fun willAddChild(element: Element)
    fun addChild(element: Element)

    class Split(parent: ElementWriter): ElementWriter by parent {
        override val context: ElementContext = parent.context.split()
    }

    class BeforeSetup(
        val base: ElementWriter,
        val setup: Element.() -> Unit
    ) : ElementWriter by base {
        override fun willAddChild(element: Element) {
            base.willAddChild(element)
            element.setup()
        }
    }
}

@OptIn(ExperimentalContracts::class, InternalKiteUi::class)
inline fun <T : Element> ElementWriter.write(element: T, setup: T.() -> Unit): T {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    willAddChild(element)
    setup(element)
    element.underlyingNativeElement.startup()
    addChild(element)
    return element
}


// canonical oops: positioning.weight.shownWhen.theme.scrolling.element

interface CanAddScrolling : ElementWriter
interface CanAddTheme : CanAddScrolling
interface CanAddShownWhen : CanAddTheme
interface CanAddWeight : CanAddShownWhen
interface ViewWriter2 : CanAddWeight