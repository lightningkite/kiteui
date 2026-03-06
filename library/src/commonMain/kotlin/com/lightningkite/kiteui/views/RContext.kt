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

// by Claude - map with parent chain for lazy lookup. Reads check parent on miss, explicit writes go to local.
// Used by RContext so that split() contexts inherit parent addons without copying.
// getOrPut writes defaults to the root so they're shared across the whole context tree.
class ChainMap<K, V>(
    private val parent: ChainMap<K, V>? = null
) : MutableMap<K, V> {
    private val local = HashMap<K, V>()
    private val root: ChainMap<K, V> get() = parent?.root ?: this

    override val size: Int get() = keys.size
    override fun isEmpty(): Boolean = local.isEmpty() && (parent?.isEmpty() != false)
    override fun containsKey(key: K): Boolean = local.containsKey(key) || (parent?.containsKey(key) == true)
    override fun containsValue(value: V): Boolean = local.containsValue(value) || (parent?.containsValue(value) == true)
    override fun get(key: K): V? = if (local.containsKey(key)) local[key] else parent?.get(key)
    override fun put(key: K, value: V): V? = local.put(key, value)
    override fun remove(key: K): V? = local.remove(key)
    override fun putAll(from: Map<out K, V>) = local.putAll(from)
    override fun clear() = local.clear()
    override val keys: MutableSet<K> get() = (parent?.keys.orEmpty() + local.keys).toMutableSet()
    override val values: MutableCollection<V> get() = keys.mapNotNull { get(it) }.toMutableList()
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = keys.associateWith { get(it) as V }.entries.map {
            object : MutableMap.MutableEntry<K, V> {
                override val key = it.key
                override val value = it.value
                override fun setValue(newValue: V): V = put(key, newValue) as V
            }
        }.toMutableSet()

    /** Like MutableMap.getOrPut, but writes new defaults to the root so they're shared across all children. */
    fun getOrPutRoot(key: K, defaultValue: () -> V): V {
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