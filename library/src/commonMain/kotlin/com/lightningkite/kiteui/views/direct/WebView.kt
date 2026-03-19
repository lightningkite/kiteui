package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext

import com.lightningkite.kiteui.views.NativeElement


expect class WebView(context: ElementContext) : NativeElement {

    var url: String
    var permitJs: Boolean
    var content: String
}