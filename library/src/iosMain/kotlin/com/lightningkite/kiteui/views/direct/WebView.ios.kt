package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import platform.WebKit.WKWebView
import platform.WebKit.*
import platform.Foundation.*
import kotlinx.cinterop.*
import platform.CoreGraphics.CGRectZero

public actual class WebView actual constructor(context: ElementContext) : NativeElement(context) {
    override val native: WKWebView

    private val webViewConfig = WKWebViewConfiguration().apply {
        preferences = WKPreferences().apply {
            javaScriptEnabled = true
        }
    }

    init {
        native = WKWebView(frame = CGRectZero.readValue(), configuration = webViewConfig)
    }

    public actual inline var url: String
        get() = native.URL?.absoluteString ?: ""
        set(value) {
            NSURL.URLWithString(value)?.let {
                val request = NSURLRequest.requestWithURL(it)
                native.loadRequest(request)
            }
        }

    public actual inline var permitJs: Boolean
        get() = native.configuration.preferences.javaScriptEnabled
        set(value) {
            native.configuration.preferences.javaScriptEnabled = value
        }

    // WKWebView has no synchronous API to read back the loaded HTML, so cache the last-set value.
    private var contentCache: String = ""

    // Not inline: the accessors read private cached state, which an inline public accessor cannot do.
    public actual var content: String
        get() = contentCache
        set(value) {
            contentCache = value
            native.loadHTMLString(value, baseURL = null)
        }
}
