package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeContainerElement
import com.lightningkite.kiteui.views.NativeInteractiveContainerElement


expect class Link(context: ElementContext) : NativeInteractiveContainerElement {
    var to: (() -> Page)?
    var onNavigator: PageNavigator
    var newTab: Boolean
    var resetsStack: Boolean
    fun onClick(action: suspend () -> Unit)
    fun onNavigate(action: suspend () -> Unit)
}