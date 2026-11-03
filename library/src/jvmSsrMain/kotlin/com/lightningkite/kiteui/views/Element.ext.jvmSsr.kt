package com.lightningkite.kiteui.views

public actual val Element.areAnimationsEnabled: Boolean get() = false
public actual inline fun Element.withoutAnimation(action: () -> Unit) { action() }