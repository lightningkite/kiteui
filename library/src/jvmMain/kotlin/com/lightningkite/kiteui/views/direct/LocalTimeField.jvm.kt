package com.lightningkite.kiteui.views.direct

import androidx.compose.runtime.Composable
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RViewWithAction
import com.lightningkite.reactive.core.MutableReactiveValue
import kotlinx.datetime.LocalTime

actual class LocalTimeField actual constructor(context: RContext) :
    RViewWithAction(context) {
    actual val content: MutableReactiveValue<LocalTime?>
        get() = TODO("Not yet implemented")
    actual var range: ClosedRange<LocalTime>?
        get() = TODO("Not yet implemented")
        set(value) {}

    @Composable
    override fun compose() {
        TODO("Not yet implemented")
    }
}