package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.RView


expect class MenuButton(context: RContext) : RView {

    fun opensMenu(createMenu: Frame.() -> Unit)
    var enabled: Boolean
    var requireClick: Boolean
    var preferredDirection: PopoverPreferredDirection
}