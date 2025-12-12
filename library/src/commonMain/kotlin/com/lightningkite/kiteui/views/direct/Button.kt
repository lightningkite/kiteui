package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.RViewWithSecondaryAction
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

expect class Button(context: RContext) : RViewWithSecondaryAction {
    var enabled: Boolean
}

fun Button.onClick(label: String? = null, icon: Icon? = null, frequencyCap: Duration? = 500.milliseconds, action: suspend ()->Unit) {
    this.action = Action(label ?: "Press", icon ?: Icon.send, frequencyCap = frequencyCap) { action() }
}

fun Button.onLongClick(label: String? = null, icon: Icon? = null, frequencyCap: Duration? = 500.milliseconds, action: suspend () -> Unit) {
    this.secondaryAction = Action(label ?: "Long Press", icon ?: Icon.info, frequencyCap = frequencyCap) { action() }
}