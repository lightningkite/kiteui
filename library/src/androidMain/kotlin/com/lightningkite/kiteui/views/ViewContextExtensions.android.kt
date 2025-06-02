package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.views.l2.overlayFrame

actual fun ViewWriter.overlayWriter(modal: Boolean, body: RView.() -> Unit) {
    body(overlayFrame ?: return)
}