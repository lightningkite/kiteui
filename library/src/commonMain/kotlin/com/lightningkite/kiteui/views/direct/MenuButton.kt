package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewWriter


expect class MenuButton(context: RContext) : RView {

    fun opensMenu(createMenu: Stack.() -> Unit)
    var enabled: Boolean
    var requireClick: Boolean
    var preferredDirection: PopoverPreferredDirection
}