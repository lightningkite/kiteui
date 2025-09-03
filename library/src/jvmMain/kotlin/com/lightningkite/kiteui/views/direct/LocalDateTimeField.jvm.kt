package com.lightningkite.kiteui.views.direct

import androidx.compose.runtime.Composable
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RViewWithAction
import com.lightningkite.reactive.core.MutableReactiveValue
import kotlinx.datetime.LocalDateTime

actual class LocalDateTimeField actual constructor(context: RContext) :
    RViewWithAction(context) {
    actual val content: MutableReactiveValue<LocalDateTime?>
        get() = TODO("Not yet implemented")
    actual var range: ClosedRange<LocalDateTime>?
        get() = TODO("Not yet implemented")
        set(value) {}

    @Composable
    override fun compose() {
        TODO("Not yet implemented")
    }
}