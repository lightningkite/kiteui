@file:MustUseReturnValues

package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.models.Edges
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.reflect.KProperty

/**
 * A value stored on [ElementContext], used as an extension property.
 *
 * ```kotlin
 * public var ElementContext.accentLevel: Int by contextAddon(0)
 *
 * col {
 *     context.accentLevel = 2
 *     text { }  // sees accentLevel == 2
 * }
 * ```
 * The [scope] decides which contexts see a value that was set. If nothing has set it, reading returns the default
 * from [init].
 *
 * Values are stored by property name. Two addon properties with the same name share a value.
 *
 * Setting a property doesn't notify anyone. To get updates, store a [Reactive] in it.
 */
public class ContextAddon<T>(public val scope: Scope = Scope.Cascading, public val init: Init<T>) {
    /** Decides which contexts see a value that was set. */
    public enum class Scope {
        /**
         * One value for the whole app. Setting it anywhere replaces it everywhere.
         *
         * It is stored on the [root][ElementContext.root]. Use it for app-wide values like `pageNavigator`.
         */
        Global,

        /**
         * Seen by the context that set it and everything inside it. This is the default.
         *
         * A context inside can set its own value. Only that context and the ones inside it see the new value.
         */
        Cascading,

        /**
         * Seen only by the context that set it.
         *
         * Containers inside it see the default. Leaf elements share their container's context, so they do see it.
         */
        Local,
    }

    /** Makes the default value of an addon. */
    public fun interface Init<out T> {
        /** Returns the default. Called when the property is read before it is set. */
        public fun get(context: ElementContext, property: KProperty<*>): T

        /** A fixed default. */
        public data class Value<T>(val value: T) : Init<T> {
            override fun get(context: ElementContext, property: KProperty<*>): T = value
        }

        /**
         * A default made by [init] on the first read, using the context being read.
         *
         * The result is saved on that context, so everything inside it shares it.
         */
        public data class Lazy<T>(val init: (ElementContext) -> T) : Init<T> {
            override fun get(context: ElementContext, property: KProperty<*>): T = init(context)
        }

        /** No default. Reading before it is set throws [IllegalStateException]. */
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

/** Returns an addon with the fixed default [init]. See [ContextAddon.Scope] for [scope]. */
public fun <T> contextAddon(init: T, scope: ContextAddon.Scope = ContextAddon.Scope.Cascading): ContextAddon<T> = ContextAddon(scope, init)

/**
 * Returns an addon with no default. Reading it before it is set throws [IllegalStateException].
 *
 * See [ContextAddon.Scope] for [scope].
 */
public fun <T> lateInitContextAddon(scope: ContextAddon.Scope = ContextAddon.Scope.Cascading): ContextAddon<T> =
    ContextAddon(scope, ContextAddon.Init.LateInit)

/**
 * Returns an addon whose default is made by [init] on the first read, from the context being read.
 *
 * The result is saved on that context, so everything inside it shares it. For [Global][ContextAddon.Scope.Global],
 * [init] gets the root context. See [ContextAddon.Scope] for [scope].
 */
public fun <T> lazyContextAddon(scope: ContextAddon.Scope = ContextAddon.Scope.Cascading, init: (ElementContext) -> T): ContextAddon<T> =
    ContextAddon(scope, ContextAddon.Init.Lazy(init))

public var ElementContext.safeInsets: Reactive<Edges> by contextAddon(Constant(Edges.ZERO), ContextAddon.Scope.Global)

/**
 * The dispatcher elements use instead of `Dispatchers.Main.immediate`, or `null` for the default.
 *
 * SSR sets it so reactive code runs synchronously.
 */
public var ElementContext.ssrDispatcher: CoroutineDispatcher? by contextAddon(null, ContextAddon.Scope.Global)
    @InternalKiteUi set
