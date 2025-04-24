package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.views.l2.overlayFrame

actual fun ViewWriter.overlayWriter(body: RView.() -> Unit) {
    body(overlayFrame ?: return)
}