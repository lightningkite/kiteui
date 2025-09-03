package com.lightningkite.kiteui.views.direct

import androidx.compose.runtime.Composable
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView

actual class DismissBackground actual constructor(context: RContext) :
    RView(context) {
    actual fun onClick(action: suspend () -> Unit) {
    }

    @Composable
    override fun compose() {
        TODO("Not yet implemented")
    }
}