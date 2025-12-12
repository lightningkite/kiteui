package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.RView

actual fun RView.openPopover(
    preferredDirection: PopoverPreferredDirection,
    anchor: RView?,
    createMenu: Frame.() -> Unit
) {
    val floating = FloatingInfoHolder(this, anchor)
    floating.preferredDirection = preferredDirection
    floating.menuGenerator = createMenu
    floating.open()
    floating.block()
}
