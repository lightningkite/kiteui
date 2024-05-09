package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ViewDsl
import ViewWriter
import org.w3c.dom.HTMLIFrameElement

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NWebView = HTMLIFrameElement

@ViewDsl
actual inline fun ViewWriter.webViewActual(crossinline setup: WebView.() -> Unit): Unit =
    themedElement<NWebView>("iframe") { setup(WebView(this)) }

actual inline var WebView.url: String
    get() = native.src
    set(value) {
        native.src = value
    }
actual inline var WebView.permitJs: Boolean
    get() = TODO()
    set(value) {
        TODO()
    }
actual inline var WebView.content: String
    get() = TODO()
    set(value) {
        TODO()
    }