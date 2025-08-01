package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.Edges
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

expect class RContext: RContextHelper {
    fun split(): RContext
    override val darkMode: Boolean?
    var immersiveMode: Boolean
}
public abstract class RContextHelper {
    public val addons: HashMap<String, Any?> = HashMap<String, Any?>()  // TODO: Use record
    public abstract val darkMode: Boolean?
}