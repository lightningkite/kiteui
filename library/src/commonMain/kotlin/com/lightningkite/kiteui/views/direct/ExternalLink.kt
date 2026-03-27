package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeContainerElement
import com.lightningkite.kiteui.views.NativeInteractiveContainerElement


expect class ExternalLink(context: ElementContext) : NativeInteractiveContainerElement {
    var to: String?
    var newTab: Boolean
    fun onNavigate(action: suspend () -> Unit)
}