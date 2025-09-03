package com.lightningkite.kiteui.views.direct

import androidx.compose.runtime.Composable
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RViewWithSecondaryAction

actual class Button actual constructor(context: RContext) :
    RViewWithSecondaryAction(context) {
    actual var enabled: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}

    @Composable
    override fun compose() {
        TODO("Not yet implemented")
    }
}