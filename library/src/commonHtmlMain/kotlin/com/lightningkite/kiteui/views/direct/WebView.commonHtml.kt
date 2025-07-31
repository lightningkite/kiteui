package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.src


public actual class WebView public actual constructor(context: RContext): RView(context) {
    init {
        native.tag = "iframe"

    }
    public actual inline var url: String
        get() = native.attributes.src ?: ""
        set(value) {
            native.attributes.src = value
        }
    public actual var permitJs: Boolean = true
    public actual inline var content: String
        get() = TODO()
        set(value) {
            TODO()
        }
}
