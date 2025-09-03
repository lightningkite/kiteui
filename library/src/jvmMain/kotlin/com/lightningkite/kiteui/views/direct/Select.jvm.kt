package com.lightningkite.kiteui.views.direct

import androidx.compose.runtime.Composable
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.core.MutableReactive
import com.lightningkite.reactive.core.Reactive

actual class Select actual constructor(context: RContext) :
    RView(context) {
    actual var enabled: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}

    actual fun <T> bind(
        edits: MutableReactive<T>,
        data: Reactive<List<T>>,
        render: (T) -> String
    ) {
    }

    @Composable
    override fun compose() {
        TODO("Not yet implemented")
    }
}