package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeContainerElementWithSecondaryAction
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

// This is basically just a frame with interactivity
public expect class Button(context: ElementContext) : NativeContainerElementWithSecondaryAction

public fun Button.onClick(label: String? = null, icon: Icon? = null, frequencyCap: Duration? = 500.milliseconds, action: suspend ()->Unit) {
    this.action = Action(label ?: "", icon ?: Icon.send, frequencyCap = frequencyCap) { action() }
}

public fun Button.onLongClick(label: String? = null, icon: Icon? = null, frequencyCap: Duration? = 500.milliseconds, action: suspend () -> Unit) {
    this.secondaryAction = Action(label ?: "", icon ?: Icon.info, frequencyCap = frequencyCap) { action() }
}