package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.*


public actual class MenuButton public actual constructor(context: RContext): RView(context) {
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

    public actual inline var enabled: Boolean
        get() = native.attributes.disabled != true
        set(value) {
            native.attributes.disabled = !value
        }
    public actual var requireClick: Boolean = false
    public actual var preferredDirection: PopoverPreferredDirection by floating::preferredDirection
    public actual fun opensMenu(createMenu: Frame.() -> Unit) {
        floating.menuGenerator = createMenu
    }
}

