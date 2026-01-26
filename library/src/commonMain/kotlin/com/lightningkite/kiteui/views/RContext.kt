package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.Edges
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.coroutines.CoroutineDispatcher

private const val DISPATCHER_KEY = "ssrDispatcher"

/**
 * Custom CoroutineDispatcher to use for RViews created with this context.
 * If set, this dispatcher will be used instead of Dispatchers.Main.immediate.
 * This is primarily useful for SSR where we need synchronous reactive scope execution.
 * - by Claude
 */
var RContext.ssrDispatcher: CoroutineDispatcher?
    get() = addons[DISPATCHER_KEY] as? CoroutineDispatcher
    set(value) { addons[DISPATCHER_KEY] = value }

expect class RContext: RContextHelper {
    fun split(): RContext
    override val darkMode: Boolean?
    var immersiveMode: Boolean
    companion object
}
abstract class RContextHelper {
    val addons = HashMap<String, Any?>()  // TODO: Use record
    abstract val darkMode: Boolean?
}