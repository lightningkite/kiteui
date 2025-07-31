package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.RView


public expect class Link(context: RContext) : RView {
    var enabled: Boolean
    var to: (() -> Page)?
    var onNavigator: PageNavigator
    var newTab: Boolean
    var resetsStack: Boolean
    fun onClick(action: suspend () -> Unit)
    fun onNavigate(action: suspend () -> Unit)
}