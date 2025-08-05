package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.views.*
import kotlinx.coroutines.launch


@InternalKiteUi
public actual class ExternalLink public actual constructor(context: RContext) : RView(context) {
    init {
        themeChoice += ClickableSemantic
        native.tag = "a"
        native.classes.add("kiteui-stack")
        native.classes.add("clickable")
    }
    override fun internalAddChild(index: Int, view: RView) {
        super.internalAddChild(index, view)
        Frame.internalAddChildStack(this, index, view)
    }

    public actual inline var to: String?
        get() = native.attributes.href
        set(value) {
            native.attributes.href = value
        }
    public actual inline var enabled: Boolean
        get() = native.attributes.disabled != true
        set(value) {
            native.attributes.disabled = !value
        }
    public actual inline var newTab: Boolean
        get() = native.attributes.target == "_blank"
        set(value) {
            native.attributes.target = if (value) "_blank" else "_self"
        }
    public actual fun onNavigate(action: suspend () -> Unit): Unit {
        native.addEventListener("click") {
            launch { action() }
        }
    }
}
