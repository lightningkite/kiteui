package com.lightningkite.mppexampleapp.widgets

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.FutureElement
import com.lightningkite.kiteui.views.NativeElement

actual class Code actual constructor(context: ElementContext) : NativeElement(context) {
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
            inner.innerHtmlUnsafe = value.ifEmpty { Typography.nbsp.toString() }
            native.runHighlighter()
        }
}

internal expect fun FutureElement.runHighlighter()