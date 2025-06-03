package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.Edges
import com.lightningkite.readable.Readable

expect class RContext: RContextHelper {
    fun split(): RContext
    override val darkMode: Boolean?
    var immersiveMode: Boolean
    val safeInsets: Readable<Edges>
}
abstract class RContextHelper {
    val addons = HashMap<String, Any?>()  // TODO: Use record
    abstract val darkMode: Boolean?
}