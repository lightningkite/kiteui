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
 * Setting the property stores the value on that context, and [scope] determines which other contexts see it. If no
 * context it can see has set a value, reading returns the default from [init]. A [lazy][Init.Lazy] default is made
 * from the context being read and stored on it, as if set there; under [Scope.Global] it is made from the root.
 *
 * The property is read each time it is used, and setting it doesn't notify anything, so store a
 * [Reactive][com.lightningkite.reactive.core.Reactive] for a value that changes.
 *
 * Values are keyed by the property's name, so two addon properties with the same name share a value.
 */
public class ContextAddon<T>(public val scope: Scope = Scope.Cascading, public val init: Init<T>) {
    /** Determines which contexts see a value set on a context. */
    public enum class Scope {
        /**
         * A value set on any context is seen by every context in the element tree.
         *
         * The value is stored on the [root][ElementContext.root], so setting it from anywhere replaces it everywhere.
         * This is not only the expected behavior for system-wide configuration, it also is much more efficient
         * for lookups since the `root` can be checked without walking the context tree each time.
         */
        Global,

        /**
         * A value set on a context is seen by that context and every context inside it.
         *
         * Reading returns the value set on the nearest context, starting from this one and going up through its
         * parents. A value set on a context further in replaces it only for that context and the ones inside it.
         */
        Cascading,

        /**
         * A value set on a context is seen only by that context.
         *
         * Contexts inside it read the default until they set their own.
         */
        Local,
    }

    /** Makes an addon's value when it is read before anything has set it. */
    public fun interface Init<out T> {
        /** Returns the default value of [property], read from [context]. */
        public fun get(context: ElementContext, property: KProperty<*>): T

        /** Defaults to [value]. */
        public data class Value<T>(val value: T) : Init<T> {
            override fun get(context: ElementContext, property: KProperty<*>): T = value
        }

        /** Defaults to the result of [init], called with the context being read the first time it has no value. */
        public data class Lazy<T>(val init: (ElementContext) -> T) : Init<T> {
            override fun get(context: ElementContext, property: KProperty<*>): T = init(context)
        }

        /** Has no default: reading the addon before it is set throws [IllegalStateException]. */
        public data object LateInit : Init<Nothing> {
            override fun get(context: ElementContext, property: KProperty<*>): Nothing =
                throw IllegalStateException("late-init addon '${property.name}' has not been initialized.")
        }
    }

    public constructor(mode: Scope, value: T) : this(mode, Init.Value(value))

    @Suppress("UNCHECKED_CAST")
    public operator fun getValue(thisRef: ElementContext, property: KProperty<*>): T = when (scope) {
        Scope.Global -> thisRef.root.addons.getLocalOrElse(property.name) { default(thisRef.root, property) }
        Scope.Cascading -> thisRef.addons.getOrElse(property.name) { default(thisRef, property) }
        Scope.Local -> thisRef.addons.getLocalOrElse(property.name) { default(thisRef, property) }
    } as T

    // Claude: a Value default is the same object on every read, so only made defaults are stored.
    private fun default(context: ElementContext, property: KProperty<*>): T {
        val value = init.get(context, property)
        if (init !is Init.Value) context.addons[property.name] = value
        return value
    }

    public operator fun setValue(thisRef: ElementContext, property: KProperty<*>, value: T) {
        val target = if (scope == Scope.Global) thisRef.addons.root else thisRef.addons
        target[property.name] = value
    }
}

/** Returns a [ContextAddon] that defaults to [init]; see [ContextAddon.Scope] for [mode]. */
public fun <T> contextAddon(init: T, mode: ContextAddon.Scope = ContextAddon.Scope.Cascading): ContextAddon<T> = ContextAddon(mode, init)

/**
 * Returns a [ContextAddon] with no default, which throws [IllegalStateException] if read before it is set; see
 * [ContextAddon.Scope] for [mode].
 */
public fun <T> lateInitContextAddon(mode: ContextAddon.Scope = ContextAddon.Scope.Cascading): ContextAddon<T> =
    ContextAddon(mode, ContextAddon.Init.LateInit)

/**
 * Returns a [ContextAddon] that defaults to the result of [init], called on the first read that needs a default; see
 * [ContextAddon.Scope] for [mode].
 */
public fun <T> lazyContextAddon(mode: ContextAddon.Scope = ContextAddon.Scope.Cascading, init: (ElementContext) -> T): ContextAddon<T> =
    ContextAddon(mode, ContextAddon.Init.Lazy(init))

public var ElementContext.safeInsets: Reactive<Edges> by contextAddon(Constant(Edges.ZERO), ContextAddon.Scope.Global)

/**
 * Custom CoroutineDispatcher to use for Elements created with this context.
 * If set, this dispatcher will be used instead of Dispatchers.Main.immediate.
 * This is primarily useful for SSR where we need synchronous reactive scope execution.
 */
public var ElementContext.ssrDispatcher: CoroutineDispatcher? by contextAddon(null, ContextAddon.Scope.Global)
    @InternalKiteUi set
