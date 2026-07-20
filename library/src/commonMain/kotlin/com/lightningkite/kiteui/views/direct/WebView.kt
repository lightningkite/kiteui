package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext

import com.lightningkite.kiteui.views.NativeElement


public expect class WebView(context: ElementContext) : NativeElement {
    public var url: String
    public var permitJs: Boolean
    public var content: String
}