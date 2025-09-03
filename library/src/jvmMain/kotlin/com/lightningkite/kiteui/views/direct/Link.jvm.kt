package com.lightningkite.kiteui.views.direct

import androidx.compose.runtime.Composable
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView

actual class Link actual constructor(context: RContext) :
    RView(context) {
    actual var enabled: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}
    actual var to: (() -> Page)?
        get() = TODO("Not yet implemented")
        set(value) {}
    actual var onNavigator: PageNavigator
        get() = TODO("Not yet implemented")
        set(value) {}
    actual var newTab: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}
    actual var resetsStack: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}

    actual fun onClick(action: suspend () -> Unit) {
    }

    actual fun onNavigate(action: suspend () -> Unit) {
    }

    @Composable
    override fun compose() {
        TODO("Not yet implemented")
    }
}