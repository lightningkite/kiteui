package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.ElementWithSecondaryAction
import com.lightningkite.kiteui.views.NativeContainerElement
import com.lightningkite.kiteui.views.NativeContainerElementWithAction
import com.lightningkite.kiteui.views.NativeContainerElementWithSecondaryAction
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.NativeInteractiveContainerElement
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

expect class ExternalLink(context: ElementContext) : ElementWithSecondaryAction, NativeContainerElement {
    var to: String?
    var newTab: Boolean

    override var enabled: Boolean

    /** Action that is triggered any time the link is clicked */
    override var action: Action?

    /** Action that is triggered only when the link actually navigates */
    override var secondaryAction: Action?
}

fun ExternalLink.onClick(
    label: String = "onClick",
    icon: Icon = Icon.send,
    frequencyCap: Duration? = 500.milliseconds,
    action: suspend () -> Unit
) {
    this.action = Action(label, icon, frequencyCap = frequencyCap) { action() }
}

/** Action that is triggered only when the link actually navigates (alias for [secondaryAction][ExternalLink.secondaryAction])*/
var ExternalLink.onNavigateAction: Action? by ExternalLink::secondaryAction

fun ExternalLink.onNavigate(
    label: String = "onNavigate",
    icon: Icon = Icon.send,
    frequencyCap: Duration? = 500.milliseconds,
    action: suspend () -> Unit
) {
    onNavigateAction = Action(label, icon, frequencyCap = frequencyCap) { action() }
}