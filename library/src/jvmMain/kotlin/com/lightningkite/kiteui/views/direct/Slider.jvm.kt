package com.lightningkite.kiteui.views.direct

import androidx.compose.runtime.Composable
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.core.MutableReactiveValue

actual class Slider actual constructor(context: RContext) :
    RView(context) {
    actual var enabled: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}
    actual val value: MutableReactiveValue<Float>
        get() = TODO("Not yet implemented")
    actual var min: Float
        get() = TODO("Not yet implemented")
        set(value) {}
    actual var max: Float
        get() = TODO("Not yet implemented")
        set(value) {}
    actual var step: Float?
        get() = TODO("Not yet implemented")
        set(value) {}

    @Composable
    override fun compose() {
        TODO("Not yet implemented")
    }
}