package com.lightningkite.kiteui.views

expect class RContext: RContextHelper {
    fun split(): RContext
}
abstract class RContextHelper {
    val addons = HashMap<String, Any?>()
    abstract val darkMode: Boolean?
}