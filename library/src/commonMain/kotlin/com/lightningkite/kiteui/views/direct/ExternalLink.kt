package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext

import com.lightningkite.kiteui.views.RView


expect class ExternalLink(context: ElementContext) : RView {
    var enabled: Boolean
    var to: String?
    var newTab: Boolean
    fun onNavigate(action: suspend () -> Unit)
}