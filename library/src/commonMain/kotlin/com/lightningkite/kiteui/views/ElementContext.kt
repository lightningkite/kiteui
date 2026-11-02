package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.exceptions.ExceptionHandler
import com.lightningkite.kiteui.exceptions.ExceptionHandlersTree
import com.lightningkite.kiteui.exceptions.ExceptionMessage
import com.lightningkite.reactive.core.Release
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.coroutines.CoroutineContext

/**
 * Holds values that elements share, such as the page navigator, the frame dialogs open in, and exception handlers.
 * It also holds platform handles, such as the activity on Android and the view controller on iOS.
 *
 * Every element has one. In UI code, read it as `context`.
 *
 * Each container (col, row, frame...) gets its own [child] context. Leaf elements (text, buttons) share their
 * container's context. A value set inside a container is seen by everything inside it. Nothing outside it sees
 * the value.
 * ```kotlin
 * button { onClick { context.pageNavigator.navigate(SettingsPage) } }
 * col {
 *     context.accentLevel = 2   // seen by everything inside this col
 *     text { }
 * }
 * text { }                      // does not see accentLevel = 2
 * ```
 * Add your own values with [ContextAddon] properties.
 *
 * It is also a coroutine context element. Coroutines an element launches carry it.
 */
public expect class ElementContext: ElementContextCommonCode {
    /**
     * Returns a new context inside this one.
     *
     * It sees this context's values. Values set on it stay inside it. Containers call this for their children.
     */
    public fun child(): ElementContext

    /**
     * Returns `true` if the platform is in dark mode, `false` if light, or `null` if unknown.
     *
     * Always `null` on web and desktop. Android and iOS read the system setting.
     */
    public val darkMode: Boolean?

    /**
     * Set to `true` to hide the system status bar. Only iOS uses it.
     *
     * Your view controller's `prefersStatusBarHidden` must read this property.
     */
    public var immersiveMode: Boolean

    /** The top context of the app. Every context is inside it. */
    public val root: ElementContext

    /** The key of [ElementContext] in a coroutine context. */
    public companion object : CoroutineContext.Key<ElementContext>
}

/** Shared code for [ElementContext]. Use [ElementContext] instead. */
public abstract class ElementContextCommonCode(
    /** The context this one was made from with [ElementContext.child], or `null` for the root. */
    public val parent: ElementContext?,
) : CoroutineContext.Element {
    override val key: CoroutineContext.Key<*> get() = ElementContext

    /**
     * Holds the values of addon properties, by name.
     *
     * Use [ContextAddon] properties instead of reading this directly.
     */
    public val addons: ChainMap<String, Any?> = parent?.addons?.child() ?: ChainMap()

    /** The exception handlers added on this context. If none of them handle an error, [parent]'s handlers get it. */
    public val exceptionHandlers: ExceptionHandlersTree = ExceptionHandlersTree(parent = parent?.exceptionHandlers)
}

/** Gives [exception] to this context's exception handlers. Returns the cleanup to run later if one handles it, or `null`. */
public fun ElementContext.handleException(exception: Exception, metadata: ExceptionHandler.Metadata? = null): Release? = exceptionHandlers.handle(this, exception, metadata)

/** Gives [exception] to this context's exception handlers. Returns the message to show if one handles it, or `null`. */
public fun ElementContext.exceptionMessage(exception: Exception, metadata: ExceptionHandler.Metadata? = null): ExceptionMessage? = exceptionHandlers.message(this, exception, metadata)

/** A map that also reads from its [parent] for keys it doesn't have. Addon properties are stored in it. */
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
    /** Returns the value at [key] in this map only, ignoring [parent]. Returns [default] if it isn't set here. */
    @Suppress("UNCHECKED_CAST")
    public fun getLocalOrElse(key: K, default: () -> V): V {
        val local = local
        return if (local != null && local.containsKey(key)) local[key] as V else default()
    }
    public operator fun set(key: K, value: V) {
        val local = local ?: HashMap<K, V>().also { local = it }
        local[key] = value
    }

    /**
     * Returns the value at [key], looking in this map first and then in [parent]. Returns [default] if no map has it.
     *
     * A key set to `null` counts as set.
     */
    @Suppress("UNCHECKED_CAST")
    public fun getOrElse(key: K, default: () -> V): V {
        var map: ChainMap<K, V>? = this
        while (map != null) {
            val local = map.local
            if (local != null && local.containsKey(key)) return local[key] as V
            map = map.parent
        }
        return default()
    }

    /** Returns the value at [key] if any map has it. Otherwise saves [defaultValue] on the [root] map and returns it. */
    public fun getOrPut(key: K, defaultValue: () -> V): V = getOrElse(key) { defaultValue().also { root[key] = it } }

    public fun child(): ChainMap<K, V> = ChainMap(this)
}
