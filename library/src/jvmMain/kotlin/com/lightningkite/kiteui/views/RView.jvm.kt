package com.lightningkite.kiteui.views

public actual val RView.areAnimationsEnabled: Boolean get() = false
public actual inline fun RView.withoutAnimation(action: () -> Unit) {
    action()
}