package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.navigation.ScreenStack
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RView
import kotlin.jvm.JvmInline
import kotlin.contracts.*


expect class Link(context: RContext) : RView {
    var to: () -> Screen
    var navigator: ScreenStack
    var newTab: Boolean
    var resetsStack: Boolean
    fun onNavigate(action: suspend () -> Unit)
}