package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.direct.Button
import com.lightningkite.kiteui.views.direct.FloatingInfoHolder

@Deprecated(message = "Use hintPopover or menuButton depending on your situation.", level = DeprecationLevel.ERROR)
@ViewModifierDsl3
public actual fun ElementWriter.hasPopover(
    requiresClick: Boolean,
    preferredDirection: PopoverPreferredDirection,
    setup: ViewWriter.() -> Unit
): ElementWriter = beforeSetup {
    val floating = FloatingInfoHolder(this)
    floating.menuGenerator = setup
    floating.preferredDirection = preferredDirection
    if (this is Button || requiresClick)
        native.addEventListener("click") {
            floating.open()
        }
    native.addEventListener("contextmenu") {
        floating.open()
    }
    native.addEventListener("mouseenter") {
        floating.open()
    }
    native.addEventListener("mouseleave") {
        floating.close()
    }
}