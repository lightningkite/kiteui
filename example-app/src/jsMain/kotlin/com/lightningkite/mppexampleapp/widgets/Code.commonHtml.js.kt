package com.lightningkite.mppexampleapp.widgets

import com.lightningkite.kiteui.views.FutureElement
import kotlinx.browser.window

public actual fun FutureElement.runHighlighter() {
    onElement { element ->
        window.setTimeout({
            js("hljs.highlightAll()")
        }, 100)
    }
}