package com.lightningkite.kiteui.views.direct

import androidx.compose.runtime.Composable
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter

actual class SwapView actual constructor(context: RContext) :
    RView(context) {
    actual fun swap(
        transition: ScreenTransition,
        createNewView: ViewWriter.() -> ViewModifiable?
    ) {
    }

    @Composable
    override fun compose() {
        TODO("Not yet implemented")
    }
}