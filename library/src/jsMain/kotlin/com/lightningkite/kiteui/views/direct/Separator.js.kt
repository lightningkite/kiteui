package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ViewDsl
import ViewWriter
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NSeparator = HTMLElement

@ViewDsl
actual inline fun ViewWriter.separatorActual(crossinline setup: Separator.() -> Unit): Unit = themedElement<HTMLDivElement>("div") {
    classList.add("kiteui-separator")
    setup(Separator(this))
}