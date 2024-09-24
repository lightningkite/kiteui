package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.AppJob
import com.lightningkite.kiteui.launch
import com.lightningkite.kiteui.launchManualCancel
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.*

actual class Button actual constructor(context: RContext): RView(context) {
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

    actual fun onClick(action: suspend () -> Unit): Unit {
        var virtualDisable: Boolean = false
        native.addEventListener("click") {
            if(!virtualDisable) {
                launch(AppJob, "click") {
                    try {
                        virtualDisable = true
                        action()
                    } finally {
                        virtualDisable = false
                    }
                }
            }
        }
    }

    actual inline var enabled: Boolean
        get() = native.attributes.disabled != true
        set(value) {
            native.attributes.disabled = !value
        }
}
