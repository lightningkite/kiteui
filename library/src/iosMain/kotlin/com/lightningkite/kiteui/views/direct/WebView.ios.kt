package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ViewDsl

import platform.UIKit.UIView

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NWebView = UIView

@ViewDsl
actual inline fun ViewWriter.webViewActual(crossinline setup: WebView.() -> Unit): Unit = todo("webView")
actual inline var WebView.url: String
    get() = TODO()
    set(value) {}
actual inline var WebView.permitJs: Boolean
    get() = TODO()
    set(value) {}
actual inline var WebView.content: String
    get() = TODO()
    set(value) {}