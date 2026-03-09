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
    var addons: ChainMap<String, Any?> = ChainMap()
    abstract val darkMode: Boolean?
}

// by Claude - scoped key-value store with parent chain for lazy lookup.
// Reads check local first, then walk up the parent chain.
// Explicit set() writes to local, shadowing the parent for that subtree.
// getOrPut() writes new defaults to the root so they're shared across all children.
// Not a MutableMap — the parent chain semantics don't match the MutableMap contract.
class ChainMap<K, V>(
    private val parent: ChainMap<K, V>? = null
) {
    private val local = HashMap<K, V>()
    private val root: ChainMap<K, V> get() = parent?.root ?: this

    operator fun get(key: K): V? = if (local.containsKey(key)) local[key] else parent?.get(key)
    operator fun set(key: K, value: V) { local[key] = value }
    fun containsKey(key: K): Boolean = local.containsKey(key) || (parent?.containsKey(key) == true)

    /** Returns existing value if found anywhere in the chain; otherwise writes [defaultValue] to the root and returns it. */
    fun getOrPut(key: K, defaultValue: () -> V): V {
        get(key)?.let { return it }
        if (containsKey(key)) {
            @Suppress("UNCHECKED_CAST")
            return get(key) as V
        }
        val value = defaultValue()
        root.local[key] = value
        return value
    }

    fun child(): ChainMap<K, V> = ChainMap(this)
}