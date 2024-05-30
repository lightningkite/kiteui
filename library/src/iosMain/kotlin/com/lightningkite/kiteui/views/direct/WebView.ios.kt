package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewDsl

import platform.UIKit.UIView
import platform.WebKit.WKWebView

actual class WebView actual constructor(context: RContext): RView(context) {
    override val native = WKWebView()
    actual inline var url: String
        get() = TODO()
        set(value) {}
    actual inline var permitJs: Boolean
        get() = TODO()
        set(value) {}
    actual inline var content: String
        get() = TODO()
        set(value) {}
}