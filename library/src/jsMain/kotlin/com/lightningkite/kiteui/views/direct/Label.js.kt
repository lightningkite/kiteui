package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ViewDsl
import ViewWriter
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NLabel = HTMLElement

@ViewDsl
actual inline fun ViewWriter.labelActual(crossinline setup: Label.() -> Unit): Unit = themedElementBackIfChanged<HTMLDivElement>("div") {
    classList.add("kiteui-label")
    textElement("span") {
    }
    setup(Label(this))
}

actual inline var Label.content: String
    get() = (native.firstElementChild as? HTMLElement)?.innerText ?: ""
    set(value) {
        (native.firstElementChild as? HTMLElement)?.innerText = value
    }