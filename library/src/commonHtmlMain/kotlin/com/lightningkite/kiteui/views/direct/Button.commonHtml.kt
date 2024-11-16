package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*

actual class Button actual constructor(context: RContext): RViewWithAction(context) {
    init {
        native.tag = "button"
        native.classes.add("kiteui-stack")
        native.classes.add("clickable")
    }

    override fun internalAddChild(index: Int, view: RView) {
        super.internalAddChild(index, view)
        Stack.internalAddChildStack(this, index, view)
    }

    override fun hasAlternateBackedStates(): Boolean = true

    init {
        native.addEventListener("click") {
            action?.startAction(this)
        }
    }

    actual inline var enabled: Boolean
        get() = native.attributes.disabled != true
        set(value) {
            native.attributes.disabled = !value
        }
}
