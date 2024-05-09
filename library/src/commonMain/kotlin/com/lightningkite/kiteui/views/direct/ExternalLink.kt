package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RView
import kotlin.jvm.JvmInline
import kotlin.contracts.*


expect class ExternalLink(context: RContext) : RView {

    var to: String
    var newTab: Boolean
    fun onNavigate(action: suspend () -> Unit)
}