package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.src


actual class WebView actual constructor(context: RContext): RView(context) {
    init {
        native.tag = "iframe"

    }
    actual inline var url: String
        get() = native.attributes.src ?: ""
        set(value) {
            native.attributes.src = value
        }
    actual var permitJs: Boolean = true
    actual inline var content: String
        get() = TODO()
        set(value) {
            TODO()
        }
}
