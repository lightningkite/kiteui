package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.views.DynamicCSS
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NContainingView = HTMLElement

@ViewDsl
actual inline fun ViewWriter.stackActual(crossinline setup: ContainingView.() -> Unit): Unit =
    themedElementBackIfChanged<HTMLDivElement>("div") {
        classList.add("kiteui-stack")
        setup(ContainingView(this))
    }

@ViewDsl
actual inline fun ViewWriter.colActual(crossinline setup: ContainingView.() -> Unit): Unit = themedElementBackIfChanged<HTMLDivElement>("div") {
    classList.add("kiteui-col")
    setup(ContainingView(this))
}

@ViewDsl
actual inline fun ViewWriter.rowActual(crossinline setup: ContainingView.() -> Unit): Unit = themedElementBackIfChanged<HTMLDivElement>("div") {
    classList.add("kiteui-row")
    setup(ContainingView(this))
}

@ViewDsl
actual fun ViewWriter.rowCollapsingToColumnActual(
    breakpoint: Dimension,
    setup: ContainingView.() -> Unit
) = themedElementBackIfChanged<HTMLDivElement>("div") {
    classList.add(DynamicCSS.rowCollapsingToColumn(breakpoint), "rowCollapsing")
    setup(ContainingView(this))
}

actual var ContainingView.vertical: Boolean
    get() = native.classList.contains("kiteui-col")
    set(value) {
        val className = if (value) "kiteui-col" else "kiteui-row"
        val oppositeClassName = if (!value) "kiteui-col" else "kiteui-row"
        native.classList.remove(oppositeClassName)
        native.classList.add(className)
    }