package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.views.ViewWriter

actual fun SwapView.nativeSwap(
    transition: ScreenTransition,
    createNewView: ViewWriter.() -> Unit
) {
}