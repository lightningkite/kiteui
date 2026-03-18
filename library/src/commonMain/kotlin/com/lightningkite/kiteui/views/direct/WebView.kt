package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext

import com.lightningkite.kiteui.views.RView


expect class WebView(context: ElementContext) : RView {

    var url: String
    var permitJs: Boolean
    var content: String
}