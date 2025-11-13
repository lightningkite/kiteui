package com.lightningkite.kiteui.views

actual val RView.areAnimationsEnabled: Boolean get() = false
actual inline fun RView.withoutAnimation(action: () -> Unit) {
    action()
}