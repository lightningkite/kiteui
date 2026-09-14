package com.lightningkite.mppexampleapp.widgets

import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.FutureElement
import com.lightningkite.kiteui.views.direct.*
import kotlinx.browser.window

actual fun FutureElement.runHighlighter() {
    onElement { element ->
        window.setTimeout({
            js("hljs.highlightAll()")
        }, 100)
    }
}