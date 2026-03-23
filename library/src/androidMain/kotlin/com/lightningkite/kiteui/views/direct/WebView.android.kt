package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.*
import android.webkit.WebView as AndroidWebView

actual class WebView actual constructor(context: ElementContext): NativeElement(context) {
    override val native = AndroidWebView(context.activity).apply {
    }
    actual var url: String
        get() {
            return native.url ?: ""
        }
        set(value) {
            native.loadUrl(value)
        }
    actual var permitJs: Boolean
        get() {
            return native.settings.javaScriptEnabled
        }
        set(value) {
            native.settings.javaScriptEnabled = value
        }
    actual var content: String
        get() {
            return native.tag as? String ?: ""
        }
        set(value) {
            native.tag = value
            native.loadData(value, null, "utf8")
        }
}