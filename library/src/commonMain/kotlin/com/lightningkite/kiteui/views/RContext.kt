package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.reactive.Readable

expect class RContext: RContextHelper {
    fun split(): RContext
    override val darkMode: Boolean?
}
abstract class RContextHelper {
    val addons = HashMap<String, Any?>()
    abstract val darkMode: Boolean?
    abstract val keyboardVisible: Readable<Boolean>
}