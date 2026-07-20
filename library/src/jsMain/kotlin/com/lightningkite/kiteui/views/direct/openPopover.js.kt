package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.Element

public actual fun Element.openPopover(
    preferredDirection: PopoverPreferredDirection,
    createMenu: Frame.() -> Unit
) {
    val floating = FloatingInfoHolder(this)
    floating.menuGenerator = createMenu
    floating.open()
    floating.block()
}