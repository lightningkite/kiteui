package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.reactive.context.CoroutineScopeHelpers

interface ElementWriter : CoroutineScopeHelpers {
    val context: RContext
    fun willAddChild(view: RView)
    fun addChild(view: RView)

    class Split(parent: ElementWriter): ElementWriter by parent {
        override val context: RContext = parent.context.split()
    }

    class BeforeSetup(
        val base: ElementWriter,
        val setup: RView.() -> Unit
    ) : ElementWriter by base {
        override fun willAddChild(view: RView) {
            base.willAddChild(view)
            view.setup()
        }
    }
}



// canonical oops: positioning.weight.shownWhen.theme.scrolling.element

interface CanDoElement : ElementWriter
interface CanDoScrolling : CanDoElement
interface CanDoTheme : CanDoScrolling
interface CanDoShownWhen : CanDoTheme
interface CanDoWeight : CanDoShownWhen
interface ViewWriter2 : CanDoWeight

private fun ViewWriter2.align(align: Align): CanDoWeight = this
private fun CanDoWeight.weight(weight: Float): CanDoShownWhen = this
private fun CanDoShownWhen.shownWhen(predicate: () -> Boolean): CanDoTheme = this
private fun CanDoTheme.themed(theme: ThemeDerivation): CanDoTheme = this
private fun CanDoScrolling.scrolling(): CanDoElement = this

private fun ElementWriter.element(): Unit = TODO()

private fun ElementWriter.applyModifiersUnsafe(modifiers: (ViewWriter2) -> ElementWriter): ElementWriter {
    return modifiers(object : ViewWriter2, ElementWriter by this {})
}

private fun ViewWriter2.test() {
    weight(4f)
        .shownWhen { true }
        .themed(ThemeDerivation.none)
        .scrolling()
        .applyModifiersUnsafe {
            it.shownWhen { true }
        }
        .element()
}