package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeContainerElement
import com.lightningkite.kiteui.views.NativeInteractiveContainerElement


expect class MenuButton(context: ElementContext) : NativeInteractiveContainerElement {
    fun opensMenu(createMenu: Frame.() -> Unit)
    var requireClick: Boolean
    var preferredDirection: PopoverPreferredDirection
}