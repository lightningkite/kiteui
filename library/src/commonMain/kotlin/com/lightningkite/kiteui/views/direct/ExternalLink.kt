package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RView
import kotlin.jvm.JvmInline
import kotlin.contracts.*


public expect class ExternalLink(context: RContext) : RView {
    public var enabled: Boolean
    public var to: String?
    public var newTab: Boolean
    public fun onNavigate(action: suspend () -> Unit)
}