package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.*


actual class MenuButton actual constructor(context: RContext): RView(context) {
    private var _openMenu: (() -> Unit)? = null
    override val driverActions get() = super.driverActions + menuDriverActions(_openMenu)
    val floating = FloatingInfoHolder(this)
    init {
        themeChoice += ClickableSemantic
        native.tag = "button"
        native.classes.add("kiteui-stack")
        native.classes.add("clickable")
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
    override fun internalAddChild(index: Int, view: RView) {
        super.internalAddChild(index, view)
        Frame.internalAddChildStack(this, index, view)
    }

    actual inline var enabled: Boolean
        get() = native.attributes.disabled != true
        set(value) {
            native.attributes.disabled = !value
        }
    actual var requireClick: Boolean = false
    actual var preferredDirection: PopoverPreferredDirection by floating::preferredDirection
    actual fun opensMenu(createMenu: Frame.() -> Unit) {
        floating.menuGenerator = createMenu
        _openMenu = { floating.open(); floating.block() }
    }
}

