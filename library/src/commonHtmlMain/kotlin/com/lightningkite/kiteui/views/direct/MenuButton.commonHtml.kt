package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.*


actual class MenuButton actual constructor(context: ElementContext): NativeInteractiveContainerElement(context) {
    private var _openMenu: (() -> Unit)? = null
    override val driverActions get() = super.driverActions + menuDriverActions(click = _openMenu)
    val floating = FloatingInfoHolder(this)
    init {
        themeChoice += ClickableSemantic
        native.tag = "button"
        native.classes.add("kiteui-stack")
        native.classes.add("clickable")
        native.setAttribute("aria-haspopup", "dialog")
        native.setAttribute("aria-expanded", "false")
        native.addEventListener("click") {
            floating.open()
            floating.block()
        }
        native.addEventListener("mouseenter") {
            if(!requireClick) {
                floating.open()
            }
        }
    }
    override fun nativeAddChild(index: Int, element: Element) {
        super.nativeAddChild(index, element)
        Frame.internalAddChildStack(this, index, element)
    }

    actual var requireClick: Boolean = false
    actual var preferredDirection: PopoverPreferredDirection by floating::preferredDirection
    actual fun opensMenu(createMenu: Frame.() -> Unit) {
        floating.menuGenerator = createMenu
        _openMenu = { floating.open(); floating.block() }
    }
}

