package com.lightningkite.kiteui.views

import kotlinx.coroutines.CoroutineDispatcher

private const val DISPATCHER_KEY = "ssrDispatcher"

/**
 * Custom CoroutineDispatcher to use for RViews created with this context.
 * If set, this dispatcher will be used instead of Dispatchers.Main.immediate.
 * This is primarily useful for SSR where we need synchronous reactive scope execution.
 */
var ElementContext.ssrDispatcher: CoroutineDispatcher?
    get() = addons[DISPATCHER_KEY] as? CoroutineDispatcher
    set(value) { addons[DISPATCHER_KEY] = value }

expect class ElementContext: ElementContextCommonCode {
    fun split(): ElementContext
    override val darkMode: Boolean?
    var immersiveMode: Boolean
    companion object
}

abstract class ElementContextCommonCode(parent: ElementContext?) {
    val addons: ChainMap<String, Any?> = parent?.addons?.child() ?: ChainMap()

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
    private val root: ChainMap<K, V> = parent?.root ?: this

    fun containsKey(key: K): Boolean = local.containsKey(key) || (parent?.containsKey(key) == true)
    operator fun get(key: K): V? = if (local.containsKey(key)) local[key] else parent?.get(key)
    operator fun set(key: K, value: V) { local[key] = value }
    fun getLocal(key: K): V? = local[key]

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

    fun getOrPutLocal(key: K, defaultValue: () -> V): V = local.getOrPut(key, defaultValue)

    fun child(): ChainMap<K, V> = ChainMap(this)
}