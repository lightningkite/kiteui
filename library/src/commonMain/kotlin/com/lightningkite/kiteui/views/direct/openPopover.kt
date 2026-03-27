package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.Element

expect fun Element.openPopover(preferredDirection: PopoverPreferredDirection, createMenu: Frame.() -> Unit)