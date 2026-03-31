package com.lightningkite.kiteui

@Repeatable
@Target(AnnotationTarget.CLASS)
annotation class Routable(val path: String)

@Target(AnnotationTarget.CLASS)
annotation class FallbackRoute

@Target(AnnotationTarget.PROPERTY)
annotation class QueryParameter(val name: String = "")

@Target(AnnotationTarget.PROPERTY)
annotation class Hash

@Suppress("ExperimentalAnnotationRetention")
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This may change, use it at your own risk"
)
annotation class InternalKiteUi

@Suppress("ExperimentalAnnotationRetention")
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This method is meant to be overridden but not used directly in client code."
)
/**
 * Methods marked as [OverrideOnly] are intended to be extended and overridden but not used directly in client code,
 * they are essentially of `protected` visibility.
 *
 * For example, element lifecycle methods like `Element.onStartup` and `Element.onShutdown` are useful hooks for
 * components so they can control and bind resources to their own lifetime, but should _not_ be
 * called outside internal code as it could cause lifecycle bugs.
 * */
annotation class OverrideOnly

@Suppress("ExperimentalAnnotationRetention")
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This may change, use it at your own risk"
)
annotation class ExperimentalKiteUi

@Suppress("ExperimentalAnnotationRetention")
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This hasn't been tested thoroughly enough to recommend use.  Use at your own risk."
)
annotation class Untested


@Suppress("ExperimentalAnnotationRetention")
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn("Applying modifiers in the wrong order can lead to subtle bugs.", RequiresOptIn.Level.WARNING)
annotation class UnsafeModifierOrdering
