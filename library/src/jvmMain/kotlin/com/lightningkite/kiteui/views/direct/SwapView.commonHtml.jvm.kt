package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter

@InternalKiteUi
public actual fun SwapView.nativeSwap(
    transition: ScreenTransition,
    createNewView: ViewWriter.() -> ViewModifiable?
) {
    clearChildren()
    createNewView()
}