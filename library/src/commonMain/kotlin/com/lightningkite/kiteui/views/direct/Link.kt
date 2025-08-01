package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.RView


public expect class Link(context: RContext) : RView {
    public var enabled: Boolean
    public var to: (() -> Page)?
    public var onNavigator: PageNavigator
    public var newTab: Boolean
    public var resetsStack: Boolean
    public fun onClick(action: suspend () -> Unit)
    public fun onNavigate(action: suspend () -> Unit)
}