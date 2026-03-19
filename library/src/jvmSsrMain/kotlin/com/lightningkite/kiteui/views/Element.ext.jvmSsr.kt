package com.lightningkite.kiteui.views

actual val Element.areAnimationsEnabled: Boolean get() = false
actual inline fun Element.withoutAnimation(action: () -> Unit) { action() }