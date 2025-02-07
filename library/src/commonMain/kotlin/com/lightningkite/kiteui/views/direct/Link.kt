package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.ScreenNavigator
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.RView


expect class Link(context: RContext) : RView {
    var enabled: Boolean
    var to: (() -> Page)?
    var onNavigator: ScreenNavigator
    var newTab: Boolean
    var resetsStack: Boolean
    fun onNavigate(action: suspend () -> Unit)
}