package com.lightningkite.kiteui.models

import com.lightningkite.kiteui.Platform
import com.lightningkite.kiteui.probablyAppleUser
import com.lightningkite.kiteui.views.l2.LabelSemantic
import kotlin.jvm.JvmInline
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Represents a [Theme] along with rendering instructions for background and padding.
 *
 * This class combines a theme with flags that control whether the background should be drawn
 * and whether padding should be applied when rendering UI elements.
 *
 * @property theme The theme containing visual styling information.
 * @property drawBackground Whether to render the background color/paint of the theme.
 * @property padding Whether to apply padding around the content.
 */
public data class ThemeAndBack(val theme: Theme, val drawBackground: Boolean, val padding: Boolean) {
    /**
     * Applies a semantic modifier to this theme, producing a new [ThemeAndBack].
     *
     * @param semantic The semantic to apply.
     * @return A new [ThemeAndBack] with the semantic applied.
     */
    public operator fun get(semantic: Semantic): ThemeAndBack = this + semantic

    /**
     * Combines this [ThemeAndBack] with a [ThemeDerivation], producing a new [ThemeAndBack].
     *
     * The result respects the background and padding flags from both the current state
     * and the derivation result.
     *
     * @param other The theme derivation to apply.
     * @return A new [ThemeAndBack] with the derivation applied.
     */
    public operator fun plus(other: ThemeDerivation): ThemeAndBack {
        val b = other(theme)
        return if (drawBackground || b.drawBackground) {
            if (padding || b.padding) {
                b.theme.withBack
            } else {
                b.theme.withBackNoPadding
            }
        } else {
            if (padding || b.padding) {
                b.theme.withoutBackButPadding
            } else {
                b.theme.withoutBack
            }
        }
    }
}

/**
 * Represents a transformation or modification to a [Theme].
 *
 * Theme derivations are composable operations that can be applied to themes to create
 * variations with different visual properties. They are the foundation for semantic theming,
 * allowing UI elements to request theme modifications like "important", "disabled", etc.
 *
 * Derivations can be chained together using the [plus] operator to create complex
 * theme transformations.
 */
public interface ThemeDerivation {
    /**
     * Applies this derivation to a theme, producing a [ThemeAndBack].
     *
     * @param theme The base theme to derive from.
     * @return The resulting theme with background and padding flags.
     */
    public operator fun invoke(theme: Theme): ThemeAndBack

    //    ThemeDerivation
    public companion object {
        /**
         * A no-op derivation that returns the theme unchanged without background.
         */
        @Deprecated("Just use 'None' object directly", ReplaceWith("None")) public val none: None get() = None
    }

    /**
     * A derivation that does nothing, returning the theme without background.
     */
    public data object None : ThemeDerivation {
        override fun invoke(theme: Theme): ThemeAndBack = theme.withoutBack
        override fun plus(other: ThemeDerivation): ThemeDerivation = other
    }

    /**
     * A derivation that replaces the current theme with a specific theme.
     *
     * @property theme The theme to set.
     */
    public data class Set(val theme: Theme) : ThemeDerivation {
        override fun invoke(theme: Theme): ThemeAndBack = this.theme.withBack
    }

    /**
     * A derivation that sets a theme as a base (with background but no padding).
     *
     * @property theme The theme to set as base.
     */
    public data class SetAsBase(val theme: Theme) : ThemeDerivation {
        override fun invoke(theme: Theme): ThemeAndBack = this.theme.withBackNoPadding
    }

    /**
     * A derivation that chains two derivations together.
     *
     * @property left The first derivation to apply.
     * @property right The second derivation to apply.
     */
    public data class Chain(val left: ThemeDerivation, val right: ThemeDerivation) : ThemeDerivation {
        override fun invoke(theme: Theme): ThemeAndBack {
            return left(theme) + right
        }
    }

    /**
     * Chains this derivation with another derivation.
     *
     * Set and SetAsBase derivations replace any previous derivations.
     * Other derivations are chained together.
     *
     * @param other The derivation to chain.
     * @return The combined derivation.
     */
    public operator fun plus(other: ThemeDerivation): ThemeDerivation {
        return when (other) {
            is Set -> other
            is SetAsBase -> other
            else -> Chain(this, other)
        }
    }
}


/**
 * Creates a [ThemeDerivation] from a lambda function.
 *
 * @param action The transformation function to apply.
 * @return A new theme derivation.
 */
public inline fun ThemeDerivation(crossinline action: (Theme) -> ThemeAndBack): ThemeDerivation {
    return object : ThemeDerivation {
        override fun invoke(theme: Theme): ThemeAndBack = action(theme)
    }
}

/**
 * Creates a [ThemeDerivation] that sets a specific theme.
 *
 * @param theme The theme to set.
 * @return A Set derivation.
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun ThemeDerivation(theme: Theme): ThemeDerivation = ThemeDerivation.Set(theme)


/**
 * Base class for semantic theme modifiers.
 *
 * Semantics represent meaningful UI states or roles (like "important", "disabled", "hover")
 * that can be applied to themes. Each semantic defines how it transforms a theme through
 * the [default] function.
 *
 * Semantics can be overridden per-theme using [SemanticOverrides], allowing customization
 * of how each semantic behaves in different theme contexts.
 *
 * @property key A unique identifier for this semantic.
 */
public abstract class Semantic(public val key: String) : ThemeDerivation {
    /**
     * Defines the default transformation this semantic applies to a theme.
     *
     * This should be overridden by subclasses to implement the specific visual changes
     * for this semantic (e.g., changing colors for "hover", adjusting opacity for "disabled").
     *
     * @param theme The base theme to transform.
     * @return The transformed theme with background/padding flags.
     */
    public open fun default(theme: Theme): ThemeAndBack = theme.withoutBack

    /**
     * Applies this semantic to a theme using any configured overrides.
     *
     * @param theme The theme to apply to.
     * @return The resulting theme with this semantic applied.
     */
    override fun invoke(theme: Theme): ThemeAndBack = theme[this]

    /**
     * Creates a copy of this theme with specified changes, including background rendering.
     *
     * @param cascading Whether theme changes cascade to nested elements.
     * @param font The font styling to apply.
     * @param elevation The elevation/shadow depth.
     * @param cornerRadii The corner radius configuration.
     * @param gap The spacing between elements.
     * @param padding The padding around content.
     * @param foreground The foreground/text color.
     * @param iconOverride The icon color override (null uses foreground, INVALID keeps current).
     * @param outline The outline/border color.
     * @param outlineWidth The outline width.
     * @param separatorOverride The separator color override (null uses foreground alpha, INVALID keeps current).
     * @param background The background color.
     * @param blurBackground The background blur amount.
     * @param transform Visual transformations to apply.
     * @param bodyTransitions Transitions for body/screen changes.
     * @param dialogTransitions Transitions for dialog appearance.
     * @param transitionDuration Duration of transitions.
     * @param semanticOverrides Custom semantic behavior overrides.
     * @return A new [ThemeAndBack] with background and padding enabled.
     */
    public fun Theme.withBack(
        cascading: Boolean = true,
        font: FontAndStyle? = null,
        elevation: Dimension? = null,
        cornerRadii: CornerRadii? = null,
        cornerShape: CornerShape? = null,
        gap: Dimension? = null,
        padding: Edges? = null,
        foreground: Paint? = null,
        iconOverride: Paint? = LinearGradient.INVALID,
        outline: Paint? = null,
        outlineWidth: Dimension? = null,
        separatorOverride: Paint? = LinearGradient.INVALID,
        background: Paint? = null,
        blurBackground: Dimension? = null,
        transform: Transformation? = null,
        bodyTransitions: ScreenTransitions? = null,
        dialogTransitions: ScreenTransitions? = null,
        transitionDuration: Duration? = null,
        semanticOverrides: SemanticOverrides = SemanticOverrides.EMPTY,
    ): ThemeAndBack = copy(
        id = key,
        cascading = cascading,
        font = font,
        elevation = elevation,
        cornerRadii = cornerRadii,
        cornerShape = cornerShape,
        gap = gap,
        padding = padding,
        foreground = foreground,
        iconOverride = iconOverride,
        outline = outline,
        outlineWidth = outlineWidth,
        background = background,
        separatorOverride = separatorOverride,
        blurBackground = blurBackground,
        transform = transform,
        bodyTransitions = bodyTransitions,
        dialogTransitions = dialogTransitions,
        transitionDuration = transitionDuration,
        semanticOverrides = semanticOverrides,
    ).withBack

    /**
     * Creates a copy of this theme with specified changes, without background rendering.
     *
     * @param cascading Whether theme changes cascade to nested elements.
     * @param font The font styling to apply.
     * @param elevation The elevation/shadow depth.
     * @param cornerRadii The corner radius configuration.
     * @param gap The spacing between elements.
     * @param padding The padding around content.
     * @param foreground The foreground/text color.
     * @param iconOverride The icon color override (null uses foreground, INVALID keeps current).
     * @param outline The outline/border color.
     * @param outlineWidth The outline width.
     * @param separatorOverride The separator color override (null uses foreground alpha, INVALID keeps current).
     * @param background The background color.
     * @param blurBackground The background blur amount.
     * @param transform Visual transformations to apply.
     * @param bodyTransitions Transitions for body/screen changes.
     * @param dialogTransitions Transitions for dialog appearance.
     * @param transitionDuration Duration of transitions.
     * @param semanticOverrides Custom semantic behavior overrides.
     * @return A new [ThemeAndBack] without background but retaining other properties.
     */
    public fun Theme.withoutBack(
        cascading: Boolean = true,
        font: FontAndStyle? = null,
        elevation: Dimension? = null,
        cornerRadii: CornerRadii? = null,
        cornerShape: CornerShape? = null,
        gap: Dimension? = null,
        padding: Edges? = null,
        foreground: Paint? = null,
        iconOverride: Paint? = LinearGradient.INVALID,
        outline: Paint? = null,
        outlineWidth: Dimension? = null,
        separatorOverride: Paint? = LinearGradient.INVALID,
        background: Paint? = null,
        blurBackground: Dimension? = null,
        transform: Transformation? = null,
        bodyTransitions: ScreenTransitions? = null,
        dialogTransitions: ScreenTransitions? = null,
        transitionDuration: Duration? = null,
        semanticOverrides: SemanticOverrides = SemanticOverrides.EMPTY,
    ): ThemeAndBack = copy(
        id = key,
        cascading = cascading,
        font = font,
        elevation = elevation,
        cornerRadii = cornerRadii,
        cornerShape = cornerShape,
        gap = gap,
        padding = padding,
        foreground = foreground,
        iconOverride = iconOverride,
        outline = outline,
        outlineWidth = outlineWidth,
        background = background,
        separatorOverride = separatorOverride,
        blurBackground = blurBackground,
        transform = transform,
        bodyTransitions = bodyTransitions,
        dialogTransitions = dialogTransitions,
        transitionDuration = transitionDuration,
        semanticOverrides = semanticOverrides,
    ).withoutBack

    /**
     * Alters this theme with specified changes, creating a derived theme.
     *
     * This function modifies theme properties without specifying background/padding behavior,
     * allowing the caller to choose how to apply the result.
     *
     * @param cascading Whether theme changes cascade to nested elements.
     * @param font The font styling to apply.
     * @param elevation The elevation/shadow depth.
     * @param cornerRadii The corner radius configuration.
     * @param gap The spacing between elements.
     * @param padding The padding around content.
     * @param foreground The foreground/text color.
     * @param iconOverride The icon color override (null uses foreground, INVALID keeps current).
     * @param outline The outline/border color.
     * @param outlineWidth The outline width.
     * @param separatorOverride The separator color override (null uses foreground alpha, INVALID keeps current).
     * @param background The background color.
     * @param blurBackground The background blur amount.
     * @param transform Visual transformations to apply.
     * @param bodyTransitions Transitions for body/screen changes.
     * @param dialogTransitions Transitions for dialog appearance.
     * @param transitionDuration Duration of transitions.
     * @param semanticOverrides Custom semantic behavior overrides.
     * @return A new [Theme] with the specified modifications.
     */
    public fun Theme.alter(
        cascading: Boolean = true,
        font: FontAndStyle? = null,
        elevation: Dimension? = null,
        cornerRadii: CornerRadii? = null,
        cornerShape: CornerShape? = null,
        gap: Dimension? = null,
        padding: Edges? = null,
        foreground: Paint? = null,
        iconOverride: Paint? = LinearGradient.INVALID,
        outline: Paint? = null,
        outlineWidth: Dimension? = null,
        separatorOverride: Paint? = LinearGradient.INVALID,
        background: Paint? = null,
        blurBackground: Dimension? = null,
        transform: Transformation? = null,
        bodyTransitions: ScreenTransitions? = null,
        dialogTransitions: ScreenTransitions? = null,
        transitionDuration: Duration? = null,
        semanticOverrides: SemanticOverrides = SemanticOverrides.EMPTY,
    ): Theme = copy(
        id = key,
        cascading = cascading,
        font = font,
        elevation = elevation,
        cornerRadii = cornerRadii,
        cornerShape = cornerShape,
        gap = gap,
        padding = padding,
        foreground = foreground,
        iconOverride = iconOverride,
        outline = outline,
        outlineWidth = outlineWidth,
        background = background,
        separatorOverride = separatorOverride,
        blurBackground = blurBackground,
        transform = transform,
        bodyTransitions = bodyTransitions,
        dialogTransitions = dialogTransitions,
        transitionDuration = transitionDuration,
        semanticOverrides = semanticOverrides,
    )

    /**
     * Represents an override for a specific semantic's behavior.
     *
     * Allows customizing how a semantic transforms a theme, either for specific
     * semantic instances or for entire semantic types.
     *
     * @param T The type of semantic being overridden.
     * @property key The key identifying which semantic(s) to override.
     * @property derivation The custom transformation function to apply.
     */
    public data class Override<T : Semantic>(
        val key: Key<T>,
        val derivation: (T, Theme) -> ThemeAndBack
    ) {
        /**
         * Creates an override for a specific semantic instance.
         *
         * @param key The semantic instance to override.
         * @param derivation The transformation to apply.
         */
        public constructor(key: T, derivation: (T, Theme) -> ThemeAndBack) : this(
            Key.Instance(key),
            derivation
        )

        /**
         * Creates an override for all semantics of a specific type.
         *
         * @param key The semantic type class to override.
         * @param derivation The transformation to apply.
         */
        public constructor(key: KClass<T>, derivation: (T, Theme) -> ThemeAndBack) : this(
            Key.Type(key),
            derivation
        )

        /**
         * Key for identifying which semantic(s) to override.
         *
         * @param T The type of semantic.
         */
        public sealed interface Key<T : Semantic> {
            /**
             * Key for a specific semantic instance.
             *
             * @property semantic The semantic instance.
             */
            public data class Instance<T : Semantic>(val semantic: T) : Key<T>

            /**
             * Key for all semantics of a specific type.
             *
             * @property type The semantic type class.
             */
            public data class Type<T : Semantic>(val type: KClass<T>) : Key<T>
        }
    }
}

/**
 * Creates a semantic override for this specific semantic instance.
 *
 * @param T The type of semantic.
 * @param derivation The transformation function to apply.
 * @return A new semantic override.
 */
public fun <T : Semantic> T.override(derivation: T.(Theme) -> ThemeAndBack): Semantic.Override<T> = Semantic.Override(this, derivation)

/**
 * Creates a semantic override for all semantics of the specified type.
 *
 * @param T The type of semantic to override.
 * @param derivation The transformation function to apply.
 * @return A new semantic override.
 */
public inline fun <reified T : Semantic> override(noinline derivation: T.(Theme) -> ThemeAndBack): Semantic.Override<T> = Semantic.Override(T::class, derivation)


/**
 * A collection of semantic overrides for customizing theme derivation behavior.
 *
 * This acts as a lookup table mapping an applied semantic to any overrides on that semantic or its type.
 * Multiple override sets can be combined using the [plus] operator.
 *
 * ## Example: Override Specific Semantic Instance
 *
 * ```kotlin
 * val overrides = SemanticOverrides(
 *     ImportantSemantic.override { theme ->
 *         // Make "important" use brand color instead of foreground
 *         theme.withBack(
 *             background = Color.fromHex(0xFF6200EE),
 *             foreground = Color.white
 *         )
 *     }
 * )
 *
 * val customTheme = baseTheme.customize(
 *     newId = "branded",
 *     semanticOverrides = overrides
 * )
 * ```
 *
 * ## Example: Override All Semantics of a Type
 *
 * ```kotlin
 * // Override all HeaderSizeSemantic instances
 * val typographyOverrides = SemanticOverrides(
 *     override<HeaderSizeSemantic> { theme ->
 *         theme.withoutBack(
 *             font = theme.font.copy(
 *                 size = HeaderSizeSemantic.lookup[level - 1].rem * 1.2,
 *                 weight = 700
 *             )
 *         )
 *     }
 * )
 * ```
 *
 * ## Best Practices
 *
 * - **Semantic Theming**: Prefer overriding existing semantics instead of creating custom ones.
 * - **Theme consistency**: Override semantics at the theme level, not per-component
 */
@JvmInline
public value class SemanticOverrides private constructor(
    private val overrides: Map<Semantic.Override.Key<out Semantic>, (Semantic, Theme) -> ThemeAndBack>
) {
    /**
     * Creates semantic overrides from a list of override specifications.
     *
     * @param overrides The list of semantic overrides.
     */
    @Suppress("UNCHECKED_CAST")
    public constructor(overrides: List<Semantic.Override<*>>) : this(
        overrides.associate { it.key to (it.derivation as (Semantic, Theme) -> ThemeAndBack) }
    )

    /**
     * Creates semantic overrides from a variable number of override specifications.
     *
     * @param overrides The semantic overrides.
     */
    public constructor(vararg overrides: Semantic.Override<*>) : this(overrides.toList())

    /**
     * Derives a theme by applying the override for the given semantic, or the default if no override exists.
     *
     * @param T The type of semantic.
     * @param key The semantic to derive with.
     * @param theme The base theme.
     * @return The derived theme with background/padding flags.
     */
    public fun <T : Semantic> derive(key: T, theme: Theme): ThemeAndBack {
        val definition = overrides[Semantic.Override.Key.Instance(key)] ?: overrides[Semantic.Override.Key.Type(key::class)]

        return definition?.invoke(key, theme) ?: key.default(theme)
    }

    /**
     * Combines this override set with another, with the other's overrides taking precedence.
     *
     * @param other The other override set to combine with.
     * @return A new override set containing both sets of overrides.
     */
    public operator fun plus(other: SemanticOverrides): SemanticOverrides = SemanticOverrides(overrides + other.overrides)

    public companion object {
        /**
         * An empty set of semantic overrides.
         */
        public val EMPTY: SemanticOverrides = SemanticOverrides(emptyMap())
    }
}

@Deprecated("Use new SemanticOverrides system instead. Replace `.to` with `.override`.")
public fun semanticOverridesOf(vararg overrides: Pair<Semantic, Semantic.(Theme) -> ThemeAndBack>): SemanticOverrides = SemanticOverrides(overrides.map { Semantic.Override(it.first, it.second) })



/**
 * Semantic for forcing padding to be applied regardless of other settings.
 *
 * This semantic ensures content has padding around it without drawing a background.
 */
public data object ForcePaddingSemantic : Semantic("fpad") {
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBackButPadding
}

/**
 * Semantic for interactive elements like switches and toggles.
 *
 * On Apple platforms, applies the platform-specific accent color (iOS system blue).
 * On other platforms, uses the theme's standard foreground color.
 */
public data object InteractiveSemantic : Semantic("int") {
    override fun default(theme: Theme): ThemeAndBack {
        // iOS switch?
        return if (Platform.probablyAppleUser) {
            theme.withoutBack(
                foreground = if (theme.background.closestColor().perceivedBrightness in 0.1f..0.9f)
                    theme.foreground
                else
                    Color(1f, 0f, 122f / 255f, 255f),
                iconOverride = null,
            )
        } else {
            theme.withoutBack
        }
    }
}

/**
 * Semantic for elements in a loading state.
 *
 * Applies a fading animation effect to the background and outline, and reduces
 * the opacity of foreground elements to indicate content is being loaded.
 */
public data object LoadingSemantic : Semantic("ld") {
    override fun default(theme: Theme): ThemeAndBack = theme.alter(
        background = FadingColor(theme.background.closestColor().highlight(0.1f), theme.background.closestColor().highlight(0.2f)),
        outline = FadingColor(theme.outline.closestColor().highlight(0.1f), theme.outline.closestColor().highlight(0.2f)),
        foreground = theme.foreground.applyAlpha(0.3f),
        iconOverride = theme.iconOverride?.applyAlpha(0.3f),
    ).withBackNoPadding
}

@Deprecated("Renamed", ReplaceWith("LabelSemantic", "com.lightningkite.kiteui.views.l2.LabelSemantic"))
public typealias FieldLabelSemantic = LabelSemantic

/**
 * Semantic for elements that are actively working or processing.
 *
 * Reduces opacity to 50% to indicate the element is busy but still visible.
 */
public data object WorkingSemantic : Semantic("wrk") {
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBack(
        foreground = theme.foreground.applyAlpha(0.5f),
        iconOverride = theme.iconOverride?.applyAlpha(0.5f),
    )
}

/**
 * Semantic for list containers.
 *
 * Applies no modifications to the theme, allowing list items to inherit the parent theme.
 */
public data object ListSemantic : Semantic("lst") {
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBack
}

/**
 * Semantic for inset content areas.
 *
 * Draws a background to visually distinguish inset sections from their surroundings.
 */
public data object InsetSemantic : Semantic("inset") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack
}

/**
 * Semantic for card-style containers.
 *
 * Applies background and styling appropriate for card-based layouts.
 */
public data object CardSemantic : Semantic("crd") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack
}

/**
 * Semantic for grouped content sections.
 *
 * Delegates to [CardSemantic] to provide consistent card-style grouping.
 */
public data object GroupSemantic : Semantic("grp") {
    override fun default(theme: Theme): ThemeAndBack = theme[CardSemantic]
}

/**
 * Semantic for dismiss overlays or backdrop elements.
 *
 * Applies a semi-transparent black background with no padding or corner radius,
 * typically used for modal dismiss areas.
 */
public data object DismissSemantic : Semantic("dsmss") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        cascading = false,
        gap = 0.dp,
        cornerRadii = CornerRadii.AdaptiveToSpacing(0.dp),
        background = Color.black.applyAlpha(0.5f),
    )
}

/**
 * Semantic for input fields.
 *
 * Applies styling typical for form input fields, including outline, corner radius,
 * and non-cascading properties to maintain consistent field appearance.
 */
public data object FieldSemantic : Semantic("fld") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        cascading = false,
        outlineWidth = 1.px,
//        gap = theme.gap / 2,
        cornerRadii = when (val base = theme.cornerRadii) {
            is CornerRadii.AdaptiveToSpacing -> CornerRadii.Fixed(base.value)
            is CornerRadii.Fixed -> base
            is CornerRadii.RatioOfSize -> base
            is CornerRadii.RatioOfSpacing -> CornerRadii.Fixed(theme.gap * base.value)
            is CornerRadii.PerCorner -> base
        }
    )
}

/**
 * Semantic for button elements.
 *
 * Provides basic button styling without background, allowing button-specific
 * components to define their own background behavior.
 */
public data object ButtonSemantic : Semantic("btn") {
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBack
}

/**
 * Semantic for clickable/interactive elements.
 *
 * Applies padding without a background, making the element clickable while
 * maintaining visual consistency with non-interactive content.
 */
public data object ClickableSemantic : Semantic("clk") {
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBackButPadding
}

/**
 * Semantic for hover states.
 *
 * Supported on Web only. Applies visual feedback when the user hovers over an element,
 * including brightened background/outline and increased elevation.
 */
public data object HoverSemantic : Semantic("hov") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        background = theme.background.map { it.highlight(0.2f) },
        outline = theme.background.map { it.highlight(0.2f).highlight(0.1f) },
        elevation = theme.elevation * 2f,
    )
}

/**
 * Semantic for pressed/down states.
 *
 * Supported on Web and iOS. Android uses the standard ripple effect instead.
 * Applies visual feedback when an element is actively pressed, with increased
 * highlight and reduced elevation to simulate depth.
 */
public data object DownSemantic : Semantic("dwn") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        background = theme.background.map { it.highlight(0.3f) },
        outline = theme.background.map { it.highlight(0.3f).highlight(0.1f) },
        elevation = theme.elevation / 2f,
    )
}

/**
 * Semantic for focused states.
 *
 * Supported on Web only. Applies visual feedback when an element has keyboard focus,
 * typically with an enhanced outline to improve accessibility.
 */
public data object FocusSemantic : Semantic("fcs") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        outlineWidth = theme.outlineWidth + 2.dp,
        outline = theme.background.map { it.highlight(1f) },
    )
}

/**
 * Semantic for disabled states.
 *
 * Reduces opacity across all visual elements (foreground, background, outline)
 * to clearly indicate the element is not interactive.
 */
public data object DisabledSemantic : Semantic("dis") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        foreground = theme.foreground.applyAlpha(alpha = 0.25f),
        background = theme.background.applyAlpha(alpha = 0.5f),
        outline = theme.outline.applyAlpha(alpha = 0.25f),
    )
}

/**
 * Semantic for compact/dense layouts.
 *
 * Reduces spacing (gap and padding) by half to create more compact UI arrangements.
 */
public data object CompactSemantic : Semantic("cmp") {
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBack(
        gap = theme.gap / 2,
        padding = theme.padding / 2,
    )
}

/**
 * Semantic for selected states.
 *
 * Delegates to [DownSemantic] to provide consistent pressed-style visual feedback
 * for selected items.
 */
public data object SelectedSemantic : Semantic("sel") {
    override fun default(theme: Theme): ThemeAndBack = theme[DownSemantic]
}

/**
 * Semantic for unselected states in toggle groups.
 *
 * Renders with a transparent background and visible outline to indicate
 * the element is available but not currently selected.
 */
public data object UnselectedSemantic : Semantic("uns") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        background = theme.background.applyAlpha(alpha = 0f),
        outline = theme.background,
        outlineWidth = 2.dp
    )
}

/**
 * Semantic for outer container elements.
 *
 * Applies minimal styling, allowing the outer container to be transparent
 * while nested elements define their own appearance.
 */
public data object OuterSemantic : Semantic("outer") {
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBack
}

/**
 * Semantic for main content areas.
 *
 * Draws a background to distinguish the primary content region from surrounding UI.
 */
public data object MainContentSemantic : Semantic("cnt") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack
}

/**
 * Semantic for toolbar and action bar elements.
 *
 * Delegates to [ImportantSemantic] to create visually prominent bars that
 * stand out from regular content.
 */
public data object BarSemantic : Semantic("bar") {
    override fun default(theme: Theme): ThemeAndBack = theme[ImportantSemantic]
}

/**
 * Semantic for system-level bars (status bar, navigation bar).
 *
 * Delegates to [BarSemantic] to maintain consistent appearance with other bars.
 */
public data object SystemBarSemantic : Semantic("sba") {
    override fun default(theme: Theme): ThemeAndBack = theme[BarSemantic]
}

/**
 * Semantic for navigation elements.
 *
 * Delegates to [BarSemantic] to style navigation consistently with other bar elements.
 */
public data object NavSemantic : Semantic("nav") {
    override fun default(theme: Theme): ThemeAndBack = theme[BarSemantic]
}

/**
 * Semantic for dialog windows.
 *
 * Applies background styling appropriate for modal dialogs that overlay content.
 */
public data object DialogSemantic : Semantic("dlg") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack
}

/**
 * Semantic for popover menus and tooltips.
 *
 * Applies background styling for floating UI elements that appear contextually.
 */
public data object PopoverSemantic : Semantic("pop") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack
}

/**
 * Semantic for important/emphasized elements like primary buttons.
 *
 * Inverts foreground and background colors to create strong visual contrast,
 * making the element stand out prominently.
 */
public data object ImportantSemantic : Semantic("imp") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        background = theme.foreground,
        outline = theme.foreground,
        foreground = theme.background,
    )
}

/**
 * Semantic for critical elements requiring maximum attention.
 *
 * Applies [ImportantSemantic] twice for extra emphasis, creating a double-inverted effect.
 */
public data object CriticalSemantic : Semantic("crt") {
    override fun default(theme: Theme): ThemeAndBack = theme[ImportantSemantic][ImportantSemantic]
}

/**
 * Semantic for warning messages and caution states.
 *
 * Applies an orange color scheme to indicate caution or important information
 * that requires user attention.
 */
public data object WarningSemantic : Semantic("wrn") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        background = Color.fromHex(0xFFe36e24.toInt()),
        outline = Color.fromHex(0xFFe36e24.toInt()).highlight(0.1f),
        foreground = Color.white
    )
}

/**
 * Semantic for dangerous or destructive actions.
 *
 * Applies a red color scheme to warn users about potentially harmful operations
 * like deletion or irreversible changes.
 */
public data object DangerSemantic : Semantic("dgr") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        background = Color.fromHex(0xFFB00020.toInt()),
        outline = Color.fromHex(0xFFB00020.toInt()).highlight(0.1f),
        foreground = Color.white
    )
}

/**
 * Semantic for affirmative/positive actions.
 *
 * Applies a green color scheme to indicate positive actions like confirmation,
 * success, or approval.
 */
public data object AffirmativeSemantic : Semantic("afr") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        background = Color.fromHex(0xFF20a020.toInt()),
        outline = Color.fromHex(0xFF20a020.toInt()).highlight(0.1f),
        foreground = Color.white
    )
}

/**
 * Semantic for header elements.
 *
 * Base semantic for headers without size modification. Use [HeaderSizeSemantic]
 * to specify header levels (H1-H6).
 */
public data object HeaderSemantic : Semantic("hed") {
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBack
}

/**
 * Semantic for header elements with specific size levels.
 *
 * Adjusts font size based on header level (1-6), with level 1 being the largest.
 * Uses a predefined scale where H1 is 2.0rem, H2 is 1.6rem, etc.
 *
 * @property level The header level (1-6).
 */
public data class HeaderSizeSemantic(val level: Int) : Semantic("h$level") {
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBack(
        font = theme.font.copy(size = lookup[level - 1].rem),
    )

    public companion object {
        /**
         * Font size multipliers for each header level (H1-H8).
         */
        public val lookup: Array<Double> = arrayOf(
            2.0,
            1.6,
            1.4,
            1.3,
            1.2,
            1.1,
            1.0,
            0.8
        )
    }
}

/**
 * Semantic for subtext, captions, and secondary text.
 *
 * Reduces font size to 0.8rem and applies 70% opacity to create visually
 * de-emphasized text for supplementary information.
 */
public data object SubtextSemantic : Semantic("sub") {
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBack(
        font = theme.font.copy(size = 0.8.rem),
        foreground = theme.foreground.applyAlpha(0.7f)
    )
}

/**
 * Semantic for error messages and validation feedback.
 *
 * Uses the danger semantic's background color (red) as foreground color
 * to highlight errors without drawing a background.
 */
public data object ErrorSemantic : Semantic("err") {
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBack(
        foreground = theme[DangerSemantic].theme.background.closestColor().highlight(0.2f)
    )
}

/**
 * Semantic for invalid input or validation errors in fields.
 *
 * Applies a red outline to indicate the field contains invalid data.
 */
public data object InvalidSemantic : Semantic("ivd") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        outlineWidth = 1.px,
        outline = Color.red
    )
}

/**
 * Semantic for emphasized text (typically italicized).
 *
 * Applies italic styling to text for emphasis without changing color or size.
 */
public data object EmphasizedSemantic : Semantic("emf") {
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBack(
        font = theme.font.copy(italic = true)
    )
}

/**
 * Semantic for embedded content sections.
 *
 * Slightly darkens (or lightens for dark themes) the background to create
 * a subtle inset effect for embedded content.
 */
public data object EmbeddedSemantic : Semantic("ebd") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        background = theme.background.closestColor().highlight(-0.1f)
    )
}

/**
 * Semantic for print-friendly styling.
 *
 * Applies high-contrast black-on-white styling optimized for printing,
 * with visible outlines to define boundaries.
 */
public data object PrintSemantic : Semantic("print") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        background = Color.white,
        foreground = Color.black,
        outline = theme.background,
        outlineWidth = 2.px,
    )
}


/**
 * H1 header semantic (largest header, 2.0rem).
 */
public val H1Semantic: HeaderSizeSemantic = HeaderSizeSemantic(1)

/**
 * H2 header semantic (1.6rem).
 */
public val H2Semantic: HeaderSizeSemantic = HeaderSizeSemantic(2)

/**
 * H3 header semantic (1.4rem).
 */
public val H3Semantic: HeaderSizeSemantic = HeaderSizeSemantic(3)

/**
 * H4 header semantic (1.3rem).
 */
public val H4Semantic: HeaderSizeSemantic = HeaderSizeSemantic(4)

/**
 * H5 header semantic (1.2rem).
 */
public val H5Semantic: HeaderSizeSemantic = HeaderSizeSemantic(5)

/**
 * H6 header semantic (smallest header, 1.1rem).
 */
public val H6Semantic: HeaderSizeSemantic = HeaderSizeSemantic(6)

// ================================
// Markdown-specific semantics (by Claude)
// ================================

/**
 * Semantic for markdown blockquotes.
 * Applies a subtle highlight background with increased left padding for indentation.
 */
public data object BlockquoteSemantic : Semantic("mdq") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        background = theme.background.closestColor().highlight(-0.05f),
        padding = Edges(left = theme.gap * 2, top = theme.gap, right = theme.gap, bottom = theme.gap),
    )
}

/**
 * Semantic for markdown code blocks.
 * Applies a monospace font with contrasting background and rounded corners.
 */
public data object CodeBlockSemantic : Semantic("mdc") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        font = theme.font.copy(font = systemDefaultFixedWidthFont),
        background = theme.background.closestColor().highlight(-0.1f),
        cornerRadii = CornerRadii.Fixed(0.25.rem),
    )
}

/**
 * Semantic for inline code spans.
 * Applies a slightly smaller monospace font without background.
 */
public data object InlineCodeSemantic : Semantic("mdic") {
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBack(
        font = theme.font.copy(font = systemDefaultFixedWidthFont, size = theme.font.size * 0.9),
    )
}

/**
 * Semantic for markdown horizontal rules/thematic breaks.
 * Renders as a separator line with no padding.
 */
public data object HorizontalRuleSemantic : Semantic("mdhr") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        background = theme.separator,
        padding = Edges(0.px),
    )
}

/**
 * Semantic for list markers (bullets, numbers).
 * Applies slightly dimmed foreground color.
 */
public data object ListMarkerSemantic : Semantic("mdlm") {
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBack(
        foreground = theme.foreground.applyAlpha(0.7f),
    )
}

/**
 * Semantic for table headers in markdown.
 * Applies bold styling for header cells.
 */
public data object TableHeaderSemantic : Semantic("mdth") {
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBack(
        font = theme.font.copy(weight = 700),
    )
}

/**
 * Semantic for table cells in markdown.
 * Provides padding and borders for table cells.
 */
public data object TableCellSemantic : Semantic("mdtd") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        outlineWidth = 1.px,
        outline = theme.separator,
    )
}

/**
 * Represents visual transformations that can be applied to UI elements.
 *
 * @property translationX Horizontal translation offset.
 * @property translationY Vertical translation offset.
 * @property translationZ Depth translation offset (3D).
 * @property rotationX Rotation around the X-axis (degrees).
 * @property rotationY Rotation around the Y-axis (degrees).
 * @property rotation Rotation around the Z-axis (degrees).
 * @property scaleX Horizontal scale factor (1.0 = normal).
 * @property scaleY Vertical scale factor (1.0 = normal).
 */
public data class Transformation(
    val translationX: Double = 0.0,
    val translationY: Double = 0.0,
    val translationZ: Double = 0.0,
    val rotationX: Double = 0.0,
    val rotationY: Double = 0.0,
    val rotation: Double = 0.0,
    val scaleX: Double = 1.0,
    val scaleY: Double = 1.0,
)

/**
 * Represents a visual shader effect that can be applied to elements.
 *
 * Currently supports blur effects, with potential for future shader types.
 */
public sealed interface ShaderEffect {
    /**
     * A blur shader effect.
     *
     * @property amount The amount of blur to apply.
     */
    public data class Blur(val amount: Dimension) : ShaderEffect
}

/**
 * Represents a complete visual theme for UI elements.
 *
 * A theme encapsulates all visual styling properties including colors, typography,
 * spacing, shadows, and animations. Themes can be derived from each other and
 * customized through semantic overrides to create consistent visual hierarchies.
 *
 * Themes are immutable and identified by a unique [id]. Equality is based solely
 * on the ID, making theme comparison efficient.
 *
 * ## Using Semantics
 *
 * When a [Semantic] is applies to a theme it chains the semantic's `key` into the theme's `id`.
 * This means themes can be identified by the sum of their composed parts.
 *
 * ```kotlin
 * // Apply semantic to get themed styling
 * button {
 *     // Get important button styling
 *     themeChoice = ImportantSemantic
 *     text("Save")
 * }
 *
 * // Chain semantics for state changes
 * button {
 *     reactiveScope {
 *         themeChoice = if (enabled) {
 *             ImportantSemantic
 *         } else {
 *             ImportantSemantic + DisabledSemantic
 *         }
 *     }
 * }
 * ```
 *
 * @property id Unique identifier for this theme.
 * @property font The default font and styling for text.
 * @property elevation The shadow/elevation depth for elements.
 * @property cornerRadii The corner radius configuration for rounded corners.
 * @property gap The default spacing between elements.
 * @property padding The default padding around content.
 * @property foreground The primary foreground color (text, icons).
 * @property iconOverride Optional override color for icons (null uses foreground).
 * @property outline The outline/border color.
 * @property outlineWidth The width of outlines/borders.
 * @property separatorOverride Optional override color for separators (null uses foreground with alpha).
 * @property background The background color/paint.
 * @property blurBackground Amount of background blur (supported on Web and partially iOS).
 * @property transform Visual transformations applied to elements.
 * @property bodyTransitions Transitions used for screen/body changes.
 * @property dialogTransitions Transitions used for dialog appearance.
 * @property transitionDuration Duration for animations and transitions.
 * @property derivedFrom The theme this was derived from, if any.
 * @property derivationId An identifier for the derivation, if any.
 * @property revert The theme to revert to for non-cascading changes.
 * @property semanticOverrides Custom semantic behavior for this theme.
 */
public class Theme(
    public val id: String,

    public val font: FontAndStyle = FontAndStyle(systemDefaultFont),

    public val elevation: Dimension = 1.px,
    public val cornerRadii: CornerRadii = CornerRadii.RatioOfSpacing(1f),
    public val cornerShape: CornerShape = CornerShape.Circular,

    public val gap: Dimension = 1.rem,
    public val padding: Edges = Edges(gap),

    public val foreground: Paint = Color.black,
    public val iconOverride: Paint? = null,
    public val outline: Paint = Color.black,
    public val outlineWidth: Dimension = 0.px,
    public val separatorOverride: Paint? = null,
    public val background: Paint = Color.white,

    /**
     * Supported on Web and partially on iOS.
     */
    public val blurBackground: Dimension = 0.px,
    public val transform: Transformation? = null,

    public val bodyTransitions: ScreenTransitions = ScreenTransitions.Fade,
    public val dialogTransitions: ScreenTransitions = ScreenTransitions.Fade,
    public val transitionDuration: Duration = 0.25.seconds,

    public val derivedFrom: Theme? = null,
    public val derivationId: String? = null,
    public val revert: Theme? = null,

    public val semanticOverrides: SemanticOverrides = SemanticOverrides.EMPTY,
) {
    init {
        if (Debugger.checkIdCollisions) Debugger.checkAndRegister(this)
    }

    /**
     * The icon color, using [iconOverride] if set, otherwise [foreground].
     */
    public val icon: Paint get() = iconOverride ?: foreground

    /**
     * The separator color, using [separatorOverride] if set, otherwise foreground with 50% opacity.
     */
    public val separator: Paint get() = separatorOverride ?: foreground.applyAlpha(0.5f)

    /**
     * Creates a [ThemeAndBack] with specified background and padding flags.
     *
     * @param back Whether to draw the background.
     * @param padding Whether to apply padding.
     * @return A new [ThemeAndBack] instance.
     */
    public fun with(back: Boolean, padding: Boolean): ThemeAndBack = if (back) {
        if (padding) withBack
        else withBackNoPadding
    } else {
        if (padding) withoutBackButPadding
        else withoutBack
    }

    /**
     * This theme with background drawn and padding applied.
     */
    public val withBack: ThemeAndBack = ThemeAndBack(this, drawBackground = true, padding = true)

    /**
     * This theme with background drawn but no padding.
     */
    public val withBackNoPadding: ThemeAndBack = ThemeAndBack(this, drawBackground = true, padding = false)

    /**
     * This theme without background and no padding.
     */
    public val withoutBack: ThemeAndBack = ThemeAndBack(this, drawBackground = false, padding = false)

    /**
     * This theme without background but with padding applied.
     */
    public val withoutBackButPadding: ThemeAndBack = ThemeAndBack(this, drawBackground = false, padding = true)

    private val themeCache = HashMap<Semantic, ThemeAndBack>()

    /**
     * Applies a semantic to this theme, using cached results for performance.
     *
     * @param semantic The semantic to apply.
     * @return The resulting theme with background/padding flags.
     */
    public operator fun get(semantic: Semantic): ThemeAndBack = themeCache.getOrPut(semantic) {
        semanticOverrides.derive(semantic, this)
    }

    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?): Boolean {
        return other is Theme && this.id == other.id
    }

    /**
     * Field-by-field comparison of the properties that affect rendered output, ignoring [id],
     * provenance ([derivedFrom]/[derivationId]/[revert]), and [semanticOverrides] (a map of
     * closures, which aren't meaningfully comparable). Used only by [Debugger]'s id-collision
     * check - [equals] intentionally stays id-only for lookup performance; this is the
     * "would these two themes render the same?" check that backs it.
     */
    internal fun structurallyEquals(other: Theme): Boolean =
        font == other.font &&
        elevation == other.elevation &&
        cornerRadii == other.cornerRadii &&
        cornerShape == other.cornerShape &&
        gap == other.gap &&
        padding == other.padding &&
        foreground == other.foreground &&
        iconOverride == other.iconOverride &&
        outline == other.outline &&
        outlineWidth == other.outlineWidth &&
        separatorOverride == other.separatorOverride &&
        background == other.background &&
        blurBackground == other.blurBackground &&
        transform == other.transform &&
        bodyTransitions == other.bodyTransitions &&
        dialogTransitions == other.dialogTransitions &&
        transitionDuration == other.transitionDuration

    /**
     * Creates a customized copy of this theme with all specified properties.
     *
     * Unlike [copy], this creates a completely new theme that doesn't inherit
     * unspecified properties from the original. All parameters default to this
     * theme's values, and semantic overrides are merged.
     *
     * Unlike [copy], [newId] is used exactly as given - it is **not** chained onto this theme's
     * id. That makes `customize` the right tool for minting an independent, fully-named theme
     * variant (e.g. a branded reskin), but it also means the caller is responsible for [newId]
     * being unique across the app: a colliding id will silently alias with whatever theme
     * registered it first (see [Theme.Debugger] for a way to catch that in testing). When you're
     * deriving a variant of a theme rather than authoring a new one, prefer [copy] or a
     * [Semantic], which chain ids for you and can't collide with unrelated themes.
     *
     * @param newId The unique identifier for the new theme.
     * @param font The font styling.
     * @param elevation The elevation depth.
     * @param cornerRadii The corner radius configuration.
     * @param gap The element spacing.
     * @param padding The content padding.
     * @param foreground The foreground color.
     * @param iconOverride The icon color override.
     * @param outline The outline color.
     * @param outlineWidth The outline width.
     * @param separatorOverride The separator color override.
     * @param background The background color.
     * @param blurBackground The background blur amount.
     * @param transform Visual transformations.
     * @param bodyTransitions Screen transition style.
     * @param dialogTransitions Dialog transition style.
     * @param transitionDuration Transition duration.
     * @param semanticOverrides Semantic behavior overrides.
     * @return A new customized theme.
     */
    public fun customize(
        newId: String,
        font: FontAndStyle = this.font,
        elevation: Dimension = this.elevation,
        cornerRadii: CornerRadii = this.cornerRadii,
        cornerShape: CornerShape = this.cornerShape,
        gap: Dimension = this.gap,
        padding: Edges = this.padding,
        foreground: Paint = this.foreground,
        iconOverride: Paint? = this.iconOverride,
        outline: Paint = this.outline,
        outlineWidth: Dimension = this.outlineWidth,
        separatorOverride: Paint? = this.separatorOverride,
        background: Paint = this.background,
        blurBackground: Dimension = this.blurBackground,
        transform: Transformation? = this.transform,
        bodyTransitions: ScreenTransitions = this.bodyTransitions,
        dialogTransitions: ScreenTransitions = this.dialogTransitions,
        transitionDuration: Duration = this.transitionDuration,
        semanticOverrides: SemanticOverrides = SemanticOverrides.EMPTY,
    ): Theme = Theme(
        id = newId,
        font = font,
        elevation = elevation,
        cornerRadii = cornerRadii,
        cornerShape = cornerShape,
        gap = gap,
        padding = padding,
        foreground = foreground,
        iconOverride = iconOverride,
        outline = outline,
        outlineWidth = outlineWidth,
        separatorOverride = separatorOverride,
        background = background,
        blurBackground = blurBackground,
        transform = transform,
        bodyTransitions = bodyTransitions,
        dialogTransitions = dialogTransitions,
        transitionDuration = transitionDuration,
        semanticOverrides = this.semanticOverrides + semanticOverrides
    )

    /**
     * Creates a derived theme with only specified properties changed.
     *
     * This method creates a new theme that inherits all unspecified properties from
     * the current theme. The provided `id` is chained with the theme's current `id`.
     *
     * The [cascading] parameter controls whether changes propagate to nested elements.
     * When false, a [revert] theme is created that can restore the original theme's
     * appearance for nested content.
     *
     * Note: [iconOverride] and [separatorOverride] use [LinearGradient.INVALID] as a
     * sentinel value to distinguish between "set to null" and "keep current value".
     *
     * @param id The suffix for the derived theme's ID (will be prefixed with current theme ID).
     * @param cascading Whether changes cascade to nested elements.
     * @param font The font styling override.
     * @param elevation The elevation override.
     * @param cornerRadii The corner radius override.
     * @param gap The spacing override.
     * @param padding The padding override.
     * @param foreground The foreground color override.
     * @param iconOverride The icon color override (INVALID = keep current, null = use foreground).
     * @param outline The outline color override.
     * @param outlineWidth The outline width override.
     * @param separatorOverride The separator color override (INVALID = keep current, null = use foreground).
     * @param background The background color override.
     * @param blurBackground The background blur override.
     * @param transform The transformation override.
     * @param bodyTransitions The body transition override.
     * @param dialogTransitions The dialog transition override.
     * @param transitionDuration The transition duration override.
     * @param semanticOverrides Additional semantic overrides to merge.
     * @return A new derived theme.
     */
    public fun copy(
        id: String,
        cascading: Boolean = true,
        font: FontAndStyle? = null,
        elevation: Dimension? = null,
        cornerRadii: CornerRadii? = null,
        cornerShape: CornerShape? = null,
        gap: Dimension? = null,
        padding: Edges? = null,
        foreground: Paint? = null,
        iconOverride: Paint? = LinearGradient.INVALID,
        outline: Paint? = null,
        outlineWidth: Dimension? = null,
        separatorOverride: Paint? = LinearGradient.INVALID,
        background: Paint? = null,
        blurBackground: Dimension? = null,
        transform: Transformation? = null,
        bodyTransitions: ScreenTransitions? = null,
        dialogTransitions: ScreenTransitions? = null,
        transitionDuration: Duration? = null,
        semanticOverrides: SemanticOverrides = SemanticOverrides.EMPTY,
    ): Theme = Theme(
        id = "${this.id}-$id",
        derivedFrom = derivedFrom,
        derivationId = derivationId,
        font = font ?: this.font,
        elevation = elevation ?: this.elevation,
        cornerRadii = cornerRadii ?: this.cornerRadii,
        cornerShape = cornerShape ?: this.cornerShape,
        gap = gap ?: this.gap,
        padding = padding ?: this.padding,
        foreground = foreground ?: this.foreground,
        iconOverride = if(iconOverride == LinearGradient.INVALID) this.iconOverride else iconOverride,
        outline = outline ?: this.outline,
        outlineWidth = outlineWidth ?: this.outlineWidth,
        separatorOverride = if(separatorOverride == LinearGradient.INVALID) this.separatorOverride else separatorOverride,
        background = background ?: this.background,
        blurBackground = blurBackground ?: this.blurBackground,
        transform = transform ?: this.transform,
        bodyTransitions = bodyTransitions ?: this.bodyTransitions,
        dialogTransitions = dialogTransitions ?: this.dialogTransitions,
        transitionDuration = transitionDuration ?: this.transitionDuration,
        semanticOverrides = this.semanticOverrides + semanticOverrides,
        revert = if (!cascading) (this.revert ?: this) else this.revert?.copy(
            id = id,
            cascading = cascading,
            font = font,
            elevation = elevation,
            cornerRadii = cornerRadii,
            cornerShape = cornerShape,
            gap = gap,
            padding = padding,
            foreground = foreground,
            iconOverride = iconOverride,
            outline = outline,
            outlineWidth = outlineWidth,
            separatorOverride = separatorOverride,
            background = background,
            blurBackground = blurBackground,
            transform = transform,
            bodyTransitions = bodyTransitions,
            dialogTransitions = dialogTransitions,
            transitionDuration = transitionDuration,
            semanticOverrides = semanticOverrides,
        )
    )

    public companion object {
        /**
         * A placeholder theme used during initialization or when no theme is available.
         */
        public val placeholder: Theme = Theme("placeholder")

        private var randomGenId: Int = 0

        /**
         * Generates a random theme for testing or demonstration purposes.
         *
         * Picks one of the built-in factories and hands it a random accent on a light or dark
         * canvas. The randomness is deliberately confined to the accent and the polarity: each
         * built-in theme's elevation, corner and type language is the thing that makes it that
         * design system, so scrambling those would only produce a theme that is no longer any of
         * them. Use [randomElevationAndCorners] explicitly if that is what you want.
         *
         * @param random The random number generator to use.
         * @return A randomly generated theme.
         */
        public fun random(random: Random = Random): Theme {
            val id = "rand${randomGenId++}"
            val hue = random.nextFloat().turns
            val saturation = random.nextFloat() * 0.5f + 0.25f
            val value = random.nextFloat() * 0.5f + 0.25f
            val accent = HSVColor(hue = hue, saturation = saturation, value = value).toRGB()
            val secondary =
                HSVColor(hue = hue + Angle.halfTurn, saturation = 1f - saturation, value = 1f - value).toRGB()
            // The built-in factories all take (id, background, accent, ...), so a random theme is a
            // random accent handed to one of them on a light or a dark canvas.
            val lightCanvas = Color.fromHexString("#FAFAFA")
            val darkCanvas = Color.fromHexString("#121212")
            return listOf(
                { Theme.material(id, lightCanvas, accent, secondary) },
                { Theme.material(id, darkCanvas, accent, secondary) },
                { Theme.material3(id, lightCanvas, accent, secondary) },
                { Theme.material3(id, darkCanvas, accent, secondary) },
                { Theme.clean(id, lightCanvas, accent) },
                { Theme.clean(id, Color.black, accent) },
                { Theme.shadCnLike(id, lightCanvas, accent) },
                { Theme.shadCnLike(id, darkCanvas, accent) },
                { Theme.flat(id = id, hue = hue, saturation = 0.15f, baseBrightness = 0.8f) },
                { Theme.flat(id = id, hue = hue, saturation = 0.5f) },
            ).random(random)().randomTitleFontSettings()
        }
    }

    /**
     * Returns the theme's ID as its string representation.
     *
     * @return The theme's unique identifier.
     */
    override fun toString(): String = id

    /**
     * Debug-only tooling for catching id collisions between [Theme]s.
     *
     * [Theme.equals]/[Theme.hashCode] are id-only by design (see [Theme] KDoc) - this is a
     * deliberate performance tradeoff, not something to work around. It relies on every theme's
     * id being unique, which [themeCache], the web `t-{id}` CSS class registry, and CSS sub-theme
     * diffing all assume too: if two structurally different themes ever share an id, one silently
     * aliases onto the other's cached styling. Uniqueness is normally guaranteed by deriving
     * themes through semantics ([copy], the `Semantic.withBack`/`withoutBack`/`alter` helpers),
     * which chain the parent id into the child id. [customize] is the one common way to opt out
     * of that chaining (see its KDoc) - it's where a collision is most likely to originate.
     */
    public object Debugger {
        /**
         * When true, every constructed [Theme] is checked against previously constructed themes
         * that share its [id]; a colliding id whose visual properties differ throws immediately,
         * naming the id. Off by default: the check holds a strong reference to one [Theme] per
         * distinct id ever constructed for the life of the process, which is fine for a debugging
         * session but not something to leave on in production.
         */
        public var checkIdCollisions: Boolean = false

        private val seen = HashMap<String, Theme>()

        internal fun checkAndRegister(theme: Theme) {
            val prior = seen[theme.id]
            if (prior == null) {
                seen[theme.id] = theme
            } else check(prior.structurallyEquals(theme)) {
                "Theme id collision: two structurally different Themes both use id '${theme.id}'. " +
                    "Themes must be derived through semantic derivation (Theme.copy, semantics, " +
                    "the withBack/withoutBack/alter helpers) so ids stay unique - look for a " +
                    "manually-assigned or non-chained id, e.g. from Theme.customize."
            }
        }

        /** Clears the collision registry, e.g. between test cases. */
        public fun reset() {
            seen.clear()
        }
    }
}