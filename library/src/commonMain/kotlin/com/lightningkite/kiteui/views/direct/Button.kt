package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.RViewWithAction
import kotlin.jvm.JvmInline
import kotlin.contracts.*


public expect class Button(context: RContext) : RViewWithAction {
    public var enabled: Boolean
}

public fun Button.onClick(label: String? = null, icon: Icon? = null, action: suspend ()->Unit) {
    this.action = Action(label ?: "Press", icon ?: Icon.send, action = action)
}