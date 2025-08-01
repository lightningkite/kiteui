package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.RView


public expect class MenuButton(context: RContext) : RView {

    public fun opensMenu(createMenu: Frame.() -> Unit)
    public var enabled: Boolean
    public var requireClick: Boolean
    public var preferredDirection: PopoverPreferredDirection
}