package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import platform.WebKit.WKWebView
import platform.WebKit.*
import platform.Foundation.*
import kotlinx.cinterop.*
import platform.CoreGraphics.CGRectZero

actual class WebView actual constructor(context: RContext) : RView(context) {

    override val native: WKWebView

    private val webViewConfig = WKWebViewConfiguration().apply {
        preferences = WKPreferences().apply {
            javaScriptEnabled = true
        }
    }

    init {
        native = WKWebView(frame = CGRectZero.readValue(), configuration = webViewConfig)
    }

    actual inline var url: String
        get() = native.URL?.absoluteString ?: ""
        set(value) {
            NSURL.URLWithString(value)?.let {
                val request = NSURLRequest.requestWithURL(it)
                native.loadRequest(request)
            }
        }

    actual inline var permitJs: Boolean
        get() = native.configuration.preferences.javaScriptEnabled
        set(value) {
            native.configuration.preferences.javaScriptEnabled = value
        }

    actual inline var content: String
        get() = ""
        set(value) {
            native.loadHTMLString(value, baseURL = null)
        }
}
