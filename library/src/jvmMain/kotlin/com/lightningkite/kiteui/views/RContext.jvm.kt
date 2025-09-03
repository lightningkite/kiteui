package com.lightningkite.kiteui.views

actual class RContext : RContextHelper() {
    actual fun split(): RContext {
        TODO("Not yet implemented")
    }

    actual override val darkMode: Boolean?
        get() = TODO("Not yet implemented")
    actual var immersiveMode: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}
}