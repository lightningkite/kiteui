package com.lightningkite.mppexampleapp.widgets

import com.lightningkite.kiteui.views.FutureElement
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView

public actual class Code public actual constructor(context: RContext): RView(context) {
    init {
        native.tag = "pre"
    }
    val inner = FutureElement().apply {
        tag = "code"
        classes.add("language-kotlin")
        content = Typography.nbsp.toString()
    }.also { native.appendChild(it) }
    public actual var content: String
        get() = inner.innerHtmlUnsafe ?: ""
        set(value) {
            inner.innerHtmlUnsafe = if(value.isEmpty()) Typography.nbsp.toString() else value
            native.runHighlighter()
        }
}

internal expect fun FutureElement.runHighlighter()