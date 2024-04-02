package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.views.*

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NSwapView = FrameLayout

@ViewDsl
actual inline fun ViewWriter.swapViewActual(crossinline setup: SwapView.() -> Unit) = element(FrameLayout()) {
    extensionViewWriter = this@swapViewActual.newViews()
    handleTheme(this, viewDraws = false,) {
        setup(SwapView(this))
    }
}

@ViewDsl
actual inline fun ViewWriter.swapViewDialogActual(crossinline setup: SwapView.() -> Unit): Unit = element(FrameLayout()) {
    extensionViewWriter = this@swapViewDialogActual.newViews()
    handleTheme(this, viewDraws = false,) {
        hidden = true
        setup(SwapView(this))
    }
}

actual fun SwapView.swap(transition: ScreenTransition, createNewView: ViewWriter.() -> Unit): Unit {
    native.extensionViewWriter!!.rootCreated = null
    native.withoutAnimation {
        createNewView(native.extensionViewWriter!!)
    }
    native.clearNViews()
    native.extensionViewWriter!!.rootCreated?.let {
        native.addNView(it)
        native.hidden = false
        native.informParentOfSizeChange()
    } ?: run {
        native.hidden = true
        native.informParentOfSizeChange()
    }
}