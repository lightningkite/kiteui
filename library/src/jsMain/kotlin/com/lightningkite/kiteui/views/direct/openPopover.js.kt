package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.RView

@InternalKiteUi
public actual fun RView.openPopover(
    preferredDirection: PopoverPreferredDirection,
    createMenu: Frame.() -> Unit
) {
    val floating = FloatingInfoHolder(this)
    floating.menuGenerator = createMenu
    floating.open()
    floating.block()
}