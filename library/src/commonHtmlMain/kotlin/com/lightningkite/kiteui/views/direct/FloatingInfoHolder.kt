package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.ContainerElement
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.ElementWriter

expect class FloatingInfoHolder(source: Element) {
    var preferredDirection: PopoverPreferredDirection
    var menuGenerator: Frame.() -> Unit

    /**
     * The ARIA role to expose on the popover container. Defaults to null (no role). Set this to
     * "menu" for menus, "tooltip" for tooltips, etc. `aria-modal="true"` is only applied when this
     * is "dialog"; anchored popovers (menus/tooltips) are not modal dialogs.
     */
    var popoverRole: String?

    fun open()
    fun block()
    fun close()
}