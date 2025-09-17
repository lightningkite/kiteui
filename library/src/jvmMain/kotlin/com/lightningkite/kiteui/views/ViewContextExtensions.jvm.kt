package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.ScreenTransitions

actual fun ViewWriter.overlayWriter(
    modal: Boolean,
    transition: ScreenTransitions,
    body: RView.(remove: () -> Unit) -> Unit
) {
}