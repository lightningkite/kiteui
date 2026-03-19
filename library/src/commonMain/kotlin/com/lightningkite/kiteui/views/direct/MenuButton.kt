package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeContainerElement


expect class MenuButton(context: ElementContext) : NativeContainerElement {

    fun opensMenu(createMenu: Frame.() -> Unit)
    var enabled: Boolean
    var requireClick: Boolean
    var preferredDirection: PopoverPreferredDirection
}