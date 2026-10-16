package com.lightningkite.kiteui.views

import kotlinx.browser.document
import org.w3c.dom.HTMLElement

internal actual fun captureWebFocus(): Any? = document.activeElement

internal actual fun restoreWebFocus(token: Any?) {
    (token as? HTMLElement)?.focus()
}
