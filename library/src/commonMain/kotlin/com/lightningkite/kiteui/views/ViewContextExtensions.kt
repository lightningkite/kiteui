@file:MustUseReturnValues

package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.models.Edges
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.reflect.KProperty

/**
 * Delegates an [ElementContext] extension property to a value stored on the context, so the property behaves like
 * a value the elements inside a container share.
 *
 * ```kotlin
 * public var ElementContext.accentLevel: Int by contextAddon(0)
 * ```
 *
 * Setting the property stores the value on that context, where it is seen by everything inside that container and
 * nothing outside it. Reading it returns the value from the nearest context that set it; if none did, the [init]
 * default is made once and stored on the root, so the whole tree shares it. A property is read when it is used, and
 * setting it later doesn't notify anything, so store a [Reactive][com.lightningkite.reactive.core.Reactive] for a
 * value that changes.
 *
 * Values are keyed by the property's name, so two addon properties with the same name share a value.
 *
 * @param global `true` to keep one value for the whole element tree: setting the property from anywhere sets it
 * on the root.
 */
public class ContextAddon<T>(public val global: Boolean = false, public val init: Init<T>) {
    /** Makes an addon's value when it is read before anything has set it. */
    public fun interface Init<out T> {
        /** Returns the default value of [property], read from [context]. */
        public fun get(context: ElementContext, property: KProperty<*>): T

        /** Defaults to [value]. */
        public data class Value<T>(val value: T) : Init<T> {
            override fun get(context: ElementContext, property: KProperty<*>): T = value
        }

        /** Defaults to the result of [init], called with the context of the first read. */
        public data class Lazy<T>(val init: (ElementContext) -> T) : Init<T> {
            override fun get(context: ElementContext, property: KProperty<*>): T = init(context)
        }

        /** Has no default: reading the addon before it is set throws [IllegalStateException]. */
        public data object LateInit : Init<Nothing> {
            override fun get(context: ElementContext, property: KProperty<*>): Nothing =
                throw IllegalStateException("late-init addon '${property.name}' has not been initialized.")
        }
    }

    /** Creates an addon that defaults to [value]. */
    public constructor(global: Boolean, value: T) : this(global, Init.Value(value))

    private val ElementContext.targetAddons get() = if (this@ContextAddon.global) addons.root else addons

    @Suppress("UNCHECKED_CAST")
    public operator fun getValue(thisRef: ElementContext, property: KProperty<*>): T =
        thisRef.targetAddons.getOrPut(property.name) { init.get(thisRef, property) } as T

    public operator fun setValue(thisRef: ElementContext, property: KProperty<*>, value: T) {
        thisRef.targetAddons[property.name] = value
    }
}

/** Returns a [ContextAddon] that defaults to [init]. */
public fun <T> contextAddon(init: T): ContextAddon<T> = ContextAddon(global = false, init)

/** Returns a [ContextAddon] with no default, which throws [IllegalStateException] if read before it is set. */
public fun <T> lateInitContextAddon(): ContextAddon<T> = ContextAddon(global = false, ContextAddon.Init.LateInit)

/** Returns a [ContextAddon] that defaults to the result of [init], called once per element tree on the first read. */
public fun <T> lazyContextAddon(init: (ElementContext) -> T): ContextAddon<T> = ContextAddon(global = false, ContextAddon.Init.Lazy(init))

public var ElementContext.safeInsets: Reactive<Edges> by ContextAddon(global = true, Constant(Edges.ZERO))

/**
 * Custom CoroutineDispatcher to use for Elements created with this context.
 * If set, this dispatcher will be used instead of Dispatchers.Main.immediate.
 * This is primarily useful for SSR where we need synchronous reactive scope execution.
 */
public var ElementContext.ssrDispatcher: CoroutineDispatcher? by ContextAddon(global = true, null)
    @InternalKiteUi set
