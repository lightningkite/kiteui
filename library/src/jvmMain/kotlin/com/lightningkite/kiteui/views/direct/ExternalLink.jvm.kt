package com.lightningkite.kiteui.views.direct

import androidx.compose.runtime.Composable
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView

actual class ExternalLink actual constructor(context: RContext) :
    RView(context) {
    actual var enabled: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}
    actual var to: String?
        get() = TODO("Not yet implemented")
        set(value) {}
    actual var newTab: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}

    actual fun onNavigate(action: suspend () -> Unit) {
    }

    @Composable
    override fun compose() {
        TODO("Not yet implemented")
    }
}