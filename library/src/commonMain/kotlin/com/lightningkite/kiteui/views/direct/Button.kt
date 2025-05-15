package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.RViewWithAction


expect class Button(context: RContext) : RViewWithAction {
    var enabled: Boolean
    fun onLongClick(action: (suspend () -> Unit)?)
}

fun Button.onClick(label: String? = null, icon: Icon? = null, action: suspend ()->Unit) {
    this.action = Action(label ?: "Press", icon ?: Icon.send, action = action)
}