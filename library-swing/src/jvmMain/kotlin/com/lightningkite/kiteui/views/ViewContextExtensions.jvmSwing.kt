package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.ScreenTransitions

actual fun ViewWriter.overlayWriter(
    modal: Boolean,
    transition: ScreenTransitions,
    body: ViewWriter.(remove: () -> Unit) -> Unit
) {
    // TODO: Implement proper overlay support
    body { }
}
