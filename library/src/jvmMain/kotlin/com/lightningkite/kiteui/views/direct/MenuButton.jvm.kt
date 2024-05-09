package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.HTMLElement
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.ViewDsl
import ViewWriter

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NMenuButton = HTMLElement

@ViewDsl
actual fun ViewWriter.menuButtonActual(setup: MenuButton.() -> Unit) {
}

actual fun MenuButton.opensMenu(action: ViewWriter.() -> Unit) {
}
actual var MenuButton.enabled: Boolean
    get() = true
    set(value) { }
actual var MenuButton.requireClick: Boolean
    get() = true
    set(value) { }
actual var MenuButton.preferredDirection: PopoverPreferredDirection
    get() = PopoverPreferredDirection.belowLeft
    set(value) {}