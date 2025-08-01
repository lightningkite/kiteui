package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.RViewWithSecondaryAction

public expect class Button(context: RContext) : RViewWithSecondaryAction {
    public var enabled: Boolean
}

public fun Button.onClick(label: String? = null, icon: Icon? = null, action: suspend ()->Unit) {
    this.action = Action(label ?: "Press", icon ?: Icon.send, action = action)
}

fun Button.onLongClick(label: String? = null, icon: Icon? = null, action: suspend () -> Unit) {
    this.secondaryAction = Action(label ?: "Long Press", icon ?: Icon.info, action = action)
}