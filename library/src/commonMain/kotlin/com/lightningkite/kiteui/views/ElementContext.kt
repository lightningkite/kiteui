package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.exceptions.ExceptionHandler
import com.lightningkite.kiteui.exceptions.ExceptionHandlersTree
import com.lightningkite.kiteui.exceptions.ExceptionMessage
import com.lightningkite.reactive.core.Release
import kotlinx.coroutines.CoroutineDispatcher

private const val DISPATCHER_KEY = "ssrDispatcher"

/**
 * Custom CoroutineDispatcher to use for Elements created with this context.
 * If set, this dispatcher will be used instead of Dispatchers.Main.immediate.
 * This is primarily useful for SSR where we need synchronous reactive scope execution.
 */
public var ElementContext.ssrDispatcher: CoroutineDispatcher?
    get() = addons[DISPATCHER_KEY] as? CoroutineDispatcher
    set(value) { addons[DISPATCHER_KEY] = value }

public expect class ElementContext: ElementContextCommonCode {
    public fun split(): ElementContext

    public val darkMode: Boolean?
    public var immersiveMode: Boolean

    public companion object
}

public abstract class ElementContextCommonCode(parent: ElementContext?) {
    public val addons: ChainMap<String, Any?> = parent?.addons?.child() ?: ChainMap()
    public val exceptionHandlers: ExceptionHandlersTree = ExceptionHandlersTree(parent = parent?.exceptionHandlers)
}

public fun ElementContext.handleException(exception: Exception, metadata: ExceptionHandler.Metadata? = null): Release? = exceptionHandlers.handle(this, exception, metadata)
public fun ElementContext.exceptionMessage(exception: Exception, metadata: ExceptionHandler.Metadata? = null): ExceptionMessage? = exceptionHandlers.message(this, exception, metadata)

// by Claude - scoped key-value store with parent chain for lazy lookup.
// Reads check local first, then walk up the parent chain.
// Explicit set() writes to local, shadowing the parent for that subtree.
// getOrPut() writes new defaults to the root so they're shared across all children.
// Not a MutableMap — the parent chain semantics don't match the MutableMap contract.
public class ChainMap<K, V>(
    public val parent: ChainMap<K, V>? = null
) {
    public val local = HashMap<K, V>()
    public val root: ChainMap<K, V> = parent?.root ?: this

    public fun containsKey(key: K): Boolean = local.containsKey(key) || (parent?.containsKey(key) == true)
    public operator fun get(key: K): V? = if (local.containsKey(key)) local[key] else parent?.get(key)
    public operator fun set(key: K, value: V) { local[key] = value }

    /** Returns existing value if found anywhere in the chain; otherwise writes [defaultValue] to the root and returns it. */
    public fun getOrPut(key: K, defaultValue: () -> V): V {
        get(key)?.let { return it }
        if (containsKey(key)) {
            @Suppress("UNCHECKED_CAST")
            return get(key) as V
        }
        val value = defaultValue()
        root.local[key] = value
        return value
    }

    public fun getOrPutLocal(key: K, defaultValue: () -> V): V {
        get(key)?.let { return it }
        if (containsKey(key)) {
            @Suppress("UNCHECKED_CAST")
            return get(key) as V
        }
        val value = defaultValue()
        local[key] = value
        return value
    }

    public fun child(): ChainMap<K, V> = ChainMap(this)
}
