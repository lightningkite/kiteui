package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewWriter

public expect fun RView.openPopover(preferredDirection: PopoverPreferredDirection, createMenu: Frame.() -> Unit)