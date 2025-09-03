package com.lightningkite.kiteui.views.direct

import androidx.compose.runtime.Composable
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView

actual class ProgressBar actual constructor(context: RContext) :
    RView(context) {
    actual var ratio: Float
        get() = TODO("Not yet implemented")
        set(value) {}

    @Composable
    override fun compose() {
        TODO("Not yet implemented")
    }
}