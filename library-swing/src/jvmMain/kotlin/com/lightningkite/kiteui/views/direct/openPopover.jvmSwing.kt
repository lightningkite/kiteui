package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.RView

actual fun RView.openPopover(
    preferredDirection: PopoverPreferredDirection,
    createMenu: Frame.() -> Unit
) {
    // TODO: Implement popover using JPopupMenu or custom popup window
}
