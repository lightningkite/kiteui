package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.AiDriver

public actual class MenuButton actual constructor(context: ElementContext): NativeInteractiveContainerElement(context) {
    override val driverActions: AiDriver.Actions get() = super.driverActions + menuDriverActions()
    override val native: FrameLayout = FrameLayout(context.activity).apply {
        isClickable = true
    }
    public actual var role: String = "dialog"

    public actual fun opensMenu(createMenu: Frame.() -> Unit) {
        native.setOnClickListener {
            openPopover(preferredDirection, createMenu = createMenu)
            @Suppress("DEPRECATION")
            native.announceForAccessibility("Menu opened")
        }
    }

    public actual var requireClick: Boolean = true
    public actual var preferredDirection: PopoverPreferredDirection = PopoverPreferredDirection.belowLeft

    override fun nativeApplyTheme(theme: ThemeAndBack): Unit = applyThemeWithRipple(theme)
}
