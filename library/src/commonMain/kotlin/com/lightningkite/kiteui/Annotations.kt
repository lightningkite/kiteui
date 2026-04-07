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
    level = RequiresOptIn.Level.ERROR,
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


/**
 * Marks APIs that bypass the compile-time modifier ordering system.
 *
 * KiteUI enforces a canonical modifier order through the [ElementWriter][com.lightningkite.kiteui.views.ElementWriter]
 * type hierarchy:
 *
 * `alignment → weight → shownWhen → sizing → theme → scrolling → element`
 *
 * APIs marked with [UnsafeModifier] allow you to circumvent this ordering, which can lead to:
 * - Layout inconsistencies (e.g., applying alignment after sizing)
 * - Theme inheritance issues (e.g., applying themes after scrolling wrappers)
 * - Subtle rendering bugs that vary by platform
 *
 * ## Common Use Cases
 *
 * **Converting to unrestricted modifier access:**
 * ```kotlin
 * @UnsafeModifier
 * fun ElementWriter.withUnsafeModifiers(): ViewWriter
 * ```
 *
 * **Applying modifiers inside element setup (backwards compatibility):**
 * ```kotlin
 * @UnsafeModifier
 * fun Element.applyDynamicTheme { ... }
 * ```
 *
 * **Producing elements with unrestricted modifiers:**
 * ```kotlin
 * @UnsafeModifier
 * fun ElementWriter.produceAtMostOneUnsafe(action: ViewWriter.() -> Unit): Element?
 * ```
 *
 * ## When to Use
 *
 * Only use [UnsafeModifier] APIs when:
 * - You're maintaining legacy code that applies modifiers inside elements
 * - You need dynamic modifier ordering based on runtime conditions
 * - You're implementing low-level framework code that needs fine-grained control
 *
 * **Prefer safe alternatives:** Use the modifier system which enforces correct ordering:
 * ```kotlin
 * centered.card.col { /* content */ }  // Safe - enforced order
 * ```
 *
 * @see com.lightningkite.kiteui.views.ElementWriter
 * @see com.lightningkite.kiteui.views.ViewWriter
 */
@Suppress("ExperimentalAnnotationRetention")
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn("Applying modifiers in the wrong order can lead to subtle bugs.", RequiresOptIn.Level.WARNING)
annotation class UnsafeModifier
