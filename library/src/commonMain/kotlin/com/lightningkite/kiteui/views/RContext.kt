package com.lightningkite.kiteui.views

expect class RContext: RContextHelper {
    fun split(): RContext
    override val darkMode: Boolean?
    var immersiveMode: Boolean
}
abstract class RContextHelper {
    val addons = HashMap<String, Any?>()  // TODO: Use record
    abstract val darkMode: Boolean?
}