package com.lightningkite.kiteui.views.direct

import androidx.compose.runtime.Composable
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.core.MutableReactiveValue

actual class ToggleButton actual constructor(context: RContext) :
    RView(context) {
    actual var enabled: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}
    actual val checked: MutableReactiveValue<Boolean>
        get() = TODO("Not yet implemented")

    @Composable
    override fun compose() {
        TODO("Not yet implemented")
    }
}