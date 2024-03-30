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

actual var ContainingView.vertical: Boolean
    get() = native.classList.contains("kiteui-col")
    set(value) {
        if (native.classList.contains("kiteui-col")) {
            native.classList.remove("kiteui-col")
            native.classList.add("kiteui-row")
        } else {
            native.classList.add("kiteui-col")
            native.classList.remove("kiteui-row")
        }
    }