package com.lightningkite.kiteui.views

actual class RContext : RContextHelper() {
    override val darkMode: Boolean? = null
    actual fun split(): RContext {
        return RContext().also { it.addons.putAll(addons) }
    }
}