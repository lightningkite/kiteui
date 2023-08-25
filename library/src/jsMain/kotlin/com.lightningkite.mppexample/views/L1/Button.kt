package com.lightningkite.mppexample

import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLParagraphElement


@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias Button = HTMLButtonElement

actual inline fun ViewContext.button(setup: Button.() -> Unit): Unit = element<HTMLButtonElement>("button") {
    setup()
}

actual fun Button.onClick(action: ()->Unit) {
    addEventListener("click", {
        action()
    })
}

actual var Button.text: String
    get() = throw NotImplementedError()
    set(value) {
        innerText = value
    }

