package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.ElementWithSecondaryAction
import com.lightningkite.kiteui.views.NativeContainerElement
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

expect class Link(context: ElementContext) : ElementWithSecondaryAction, NativeContainerElement {
    var to: (() -> Page)?
    var onNavigator: PageNavigator
    var newTab: Boolean
    var resetsStack: Boolean

    override var enabled: Boolean

    /** Action that is triggered any time the link is clicked */
    override var action: Action?

    /** Action that is triggered only when the link actually navigates */
    override var secondaryAction: Action?
}

fun Link.onClick(
    label: String = "onClick",
    icon: Icon = Icon.send,
    frequencyCap: Duration? = 500.milliseconds,
    action: suspend () -> Unit
) {
    this.action = Action(label, icon, frequencyCap = frequencyCap) { action() }
}

/** Action that is triggered only when the link actually navigates (alias for [secondaryAction][Link.secondaryAction])*/
var Link.onNavigateAction: Action? by Link::secondaryAction

fun Link.onNavigate(
    label: String = "onNavigate",
    icon: Icon = Icon.send,
    frequencyCap: Duration? = 500.milliseconds,
    action: suspend () -> Unit
) {
    onNavigateAction = Action(label, icon, frequencyCap = frequencyCap) { action() }
}