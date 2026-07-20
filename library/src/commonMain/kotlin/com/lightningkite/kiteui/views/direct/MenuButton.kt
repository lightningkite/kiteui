package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeContainerElement
import com.lightningkite.kiteui.views.NativeInteractiveContainerElement


public expect class MenuButton(context: ElementContext) : NativeInteractiveContainerElement {
    public fun opensMenu(createMenu: Frame.() -> Unit)
    public var requireClick: Boolean
    public var preferredDirection: PopoverPreferredDirection
}