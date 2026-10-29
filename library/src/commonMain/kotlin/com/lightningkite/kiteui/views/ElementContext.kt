package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.exceptions.ExceptionHandler
import com.lightningkite.kiteui.exceptions.ExceptionHandlersTree
import com.lightningkite.kiteui.exceptions.ExceptionMessage
import com.lightningkite.reactive.core.Release
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Holds the values an element shares with the elements inside it, such as the page navigator, the overlay frame
 * and exception handlers, along with the platform's handles (the activity on Android, the view controller on iOS).
 *
 * Every container has its own context, made with [split], and the elements inside it use that context. A value set
 * on a context, usually through a [ContextAddon] property such as `context.pageNavigator`, is seen by everything
 * inside that container and nothing outside it; reading a value finds the nearest context, from this one up through
 * its [parent]s, that set it. Set values where everything that reads them can see them, such as at the root of the
 * app, as `appBase` does.
 */
public expect class ElementContext: ElementContextCommonCode {
    /**
     * Returns a child context that reads this one's [addons] and [exceptionHandlers] until it sets its own.
     *
     * Each container makes one for its children, so a value set inside a container is seen by everything in it
     * and nothing outside it.
     */
    public fun split(): ElementContext

    /**
     * Whether the platform is in dark mode, or `null` if it doesn't say.
     *
     * Always `null` on the web and desktop.
     */
    public val darkMode: Boolean?

    /**
     * Whether the app asks to hide the system status bar.
     *
     * Only iOS acts on it, and only once the app's view controller is set up for it; see the iOS `ElementContext`.
     */
    public var immersiveMode: Boolean

    /** The context at the top of this one's [parent]s, shared by the whole element tree; this context if it has no parent. */
    public val root: ElementContext

    public companion object
}

/** The platform-independent part of [ElementContext]; use [ElementContext] itself. */
public abstract class ElementContextCommonCode(
    /** The context this one was [split][ElementContext.split] from, or `null` for the root. */
    public val parent: ElementContext?,
) {
    /**
     * The values set on this context, falling back to its [parent]'s for any it hasn't set.
     *
     * Prefer [ContextAddon] properties to using this directly; they key values by property name.
     */
    public val addons: ChainMap<String, Any?> = parent?.addons?.child() ?: ChainMap()

    /** The exception handlers added on this context, falling back to its [parent]'s for errors none of them handle. */
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
    // Claude: most containers never set anything, so the map is made on first write.
    private var local: HashMap<K, V>? = null
    public val root: ChainMap<K, V> = parent?.root ?: this

    public fun containsKey(key: K): Boolean = local?.containsKey(key) == true || (parent?.containsKey(key) == true)

    @OptIn(ExperimentalStdlibApi::class)
    public operator fun get(key: K): V? {
        val local = local
        return if (local != null) local.getOrElseIfMissing(key) { parent?.get(key) } else parent?.get(key)
    }
    public operator fun set(key: K, value: V) { (local ?: HashMap<K, V>().also { local = it })[key] = value }

    /** Returns existing value if found anywhere in the chain; otherwise writes [defaultValue] to the root and returns it. */
    public fun getOrPut(key: K, defaultValue: () -> V): V {
        get(key)?.let { return it }
        if (containsKey(key)) {
            @Suppress("UNCHECKED_CAST")
            return get(key) as V
        }
        val value = defaultValue()
        root[key] = value
        return value
    }

    public fun getOrPutLocal(key: K, defaultValue: () -> V): V {
        get(key)?.let { return it }
        if (containsKey(key)) {
            @Suppress("UNCHECKED_CAST")
            return get(key) as V
        }
        val value = defaultValue()
        set(key, value)
        return value
    }

    public fun child(): ChainMap<K, V> = ChainMap(this)
}
