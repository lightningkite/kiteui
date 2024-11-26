package com.lightningkite.mppexampleapp.widgets

import com.lightningkite.kiteui.views.FutureElement
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView

actual class Code actual constructor(context: RContext): RView(context) {
    init {
        native.tag = "pre"
    }
    val inner = FutureElement().apply {
        tag = "code"
        classes.add("language-kotlin")
        content = Typography.nbsp.toString()
    }.also { native.appendChild(it) }
    actual var content: String
        get() = inner.innerHtmlUnsafe ?: ""
        set(value) {
            inner.innerHtmlUnsafe = if(value.isEmpty()) Typography.nbsp.toString() else value
            native.runHighlighter()
        }
}

internal expect fun FutureElement.runHighlighter()