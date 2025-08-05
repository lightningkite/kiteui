package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import android.webkit.WebView as AndroidWebView

@InternalKiteUi
public actual class WebView public actual constructor(context: RContext): RView(context) {
    override val native: AndroidWebView = AndroidWebView(context.activity).apply {
    }
    public actual var url: String
        get() {
            return native.url ?: ""
        }
        set(value) {
            native.loadUrl(value)
        }
    public actual var permitJs: Boolean
        get() {
            return native.settings.javaScriptEnabled
        }
        set(value) {
            native.settings.javaScriptEnabled = value
        }
    public actual var content: String
        get() {
            return native.tag as? String ?: ""
        }
        set(value) {
            native.tag = value
            native.loadData(value, null, "utf8")
        }
}