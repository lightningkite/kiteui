package com.lightningkite.kiteui.models

import com.lightningkite.kiteui.Platform
import com.lightningkite.kiteui.probablyAppleUser
import kotlin.jvm.JvmInline
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class ThemeAndBack(val theme: Theme, val drawBackground: Boolean, val padding: Boolean) {
    operator fun get(semantic: Semantic): ThemeAndBack = this + semantic
    operator fun plus(other: ThemeDerivation): ThemeAndBack {
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

interface ThemeDerivation {
    operator fun invoke(theme: Theme): ThemeAndBack

    //    ThemeDerivation
    companion object {
        inline operator fun invoke(crossinline action: (Theme) -> ThemeAndBack): ThemeDerivation {
            return object : ThemeDerivation {
                override fun invoke(theme: Theme): ThemeAndBack = action(theme)
            }
        }

        @Suppress("NOTHING_TO_INLINE")
        inline operator fun invoke(theme: Theme): ThemeDerivation = Set(theme)
        val none = None
    }

    data object None : ThemeDerivation {
        override fun invoke(theme: Theme): ThemeAndBack = theme.withoutBack
        override fun plus(other: ThemeDerivation): ThemeDerivation = other
    }

    data class Set(val theme: Theme) : ThemeDerivation {
        override fun invoke(theme: Theme): ThemeAndBack = this.theme.withBack
    }

    data class SetAsBase(val theme: Theme) : ThemeDerivation {
        override fun invoke(theme: Theme): ThemeAndBack = this.theme.withBackNoPadding
    }

    data class Chain(val left: ThemeDerivation, val right: ThemeDerivation) : ThemeDerivation {
        override fun invoke(theme: Theme): ThemeAndBack {
            return left(theme) + right
        }
    }

    operator fun plus(other: ThemeDerivation): ThemeDerivation {
        return when (other) {
            is Set -> other
            is SetAsBase -> other
            else -> Chain(this, other)
        }
    }
}

abstract class Semantic(val key: String) : ThemeDerivation {
    open fun default(theme: Theme): ThemeAndBack = theme.withoutBack
    override fun invoke(theme: Theme): ThemeAndBack = theme[this]

    fun Theme.withBack(
        cascading: Boolean = true,
        font: FontAndStyle? = null,
        elevation: Dimension? = null,
        cornerRadii: CornerRadii? = null,
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
    ) = copy(
        id = key,
        cascading = cascading,
        font = font,
        elevation = elevation,
        cornerRadii = cornerRadii,
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

    fun Theme.withoutBack(
        cascading: Boolean = true,
        font: FontAndStyle? = null,
        elevation: Dimension? = null,
        cornerRadii: CornerRadii? = null,
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
    ) = copy(
        id = key,
        cascading = cascading,
        font = font,
        elevation = elevation,
        cornerRadii = cornerRadii,
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

    fun Theme.alter(
        cascading: Boolean = true,
        font: FontAndStyle? = null,
        elevation: Dimension? = null,
        cornerRadii: CornerRadii? = null,
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
    ) = copy(
        id = key,
        cascading = cascading,
        font = font,
        elevation = elevation,
        cornerRadii = cornerRadii,
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

    data class Override<T : Semantic>(
        val key: Key<T>,
        val derivation: (T, Theme) -> ThemeAndBack
    ) {
        constructor(key: T, derivation: (T, Theme) -> ThemeAndBack) : this(
            Key.Instance(key),
            derivation
        )

        constructor(key: KClass<T>, derivation: (T, Theme) -> ThemeAndBack) : this(
            Key.Type(key),
            derivation
        )

        sealed interface Key<T : Semantic> {
            data class Instance<T : Semantic>(val semantic: T) : Key<T>

            data class Type<T : Semantic>(val type: KClass<T>) : Key<T>
        }
    }
}

fun <T : Semantic> T.override(derivation: T.(Theme) -> ThemeAndBack) = Semantic.Override(this, derivation)
inline fun <reified T : Semantic> override(noinline derivation: T.(Theme) -> ThemeAndBack) = Semantic.Override(T::class, derivation)


@JvmInline
value class SemanticOverrides private constructor(
    private val overrides: Map<Semantic.Override.Key<out Semantic>, (Semantic, Theme) -> ThemeAndBack>
) {
    @Suppress("UNCHECKED_CAST")
    public constructor(overrides: List<Semantic.Override<*>>) : this(
        overrides.associate { it.key to (it.derivation as (Semantic, Theme) -> ThemeAndBack) }
    )

    public constructor(vararg overrides: Semantic.Override<*>) : this(overrides.toList())

    fun <T : Semantic> derive(key: T, theme: Theme): ThemeAndBack {
        val definition = overrides[Semantic.Override.Key.Instance(key)] ?: overrides[Semantic.Override.Key.Type(key::class)]

        return definition?.invoke(key, theme) ?: key.default(theme)
    }

    operator fun plus(other: SemanticOverrides) = SemanticOverrides(overrides + other.overrides)

    public companion object {
        val EMPTY: SemanticOverrides = SemanticOverrides(emptyMap())
    }
}


fun overrideNotNull(vararg overrides: Semantic.Override<*>?): SemanticOverrides =
    if (overrides.isEmpty()) SemanticOverrides.EMPTY
    else SemanticOverrides(overrides.toList().filterNotNull())

private fun List<Semantic.Override<*>>.toSemanticOverrides(): SemanticOverrides = SemanticOverrides(this)
fun Map<Semantic, Semantic.(Theme) -> ThemeAndBack>.toSemanticOverrides(): SemanticOverrides = SemanticOverrides(map { Semantic.Override(it.key, it.value) })


data object ForcePaddingSemantic : Semantic("fpad") {
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBackButPadding
}

data object InteractiveSemantic : Semantic("int") {
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

data object LoadingSemantic : Semantic("ld") {
    override fun default(theme: Theme): ThemeAndBack = theme.alter(
        background = FadingColor(theme.background.closestColor().highlight(0.1f), theme.background.closestColor().highlight(0.2f)),
        outline = FadingColor(theme.outline.closestColor().highlight(0.1f), theme.outline.closestColor().highlight(0.2f)),
        foreground = theme.foreground.applyAlpha(0.3f),
        iconOverride = theme.iconOverride?.applyAlpha(0.3f),
    ).withBackNoPadding
}

data object FieldLabelSemantic: Semantic("flabel") {
    override fun default(theme: Theme): ThemeAndBack = theme[SubtextSemantic]
}
data object WorkingSemantic : Semantic("wrk") {
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBack(
        foreground = theme.foreground.applyAlpha(0.5f),
        iconOverride = theme.iconOverride?.applyAlpha(0.5f),
    )
}

data object ListSemantic : Semantic("lst") {
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBack
}

data object InsetSemantic : Semantic("inset") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack
}

data object CardSemantic : Semantic("crd") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack
}

data object GroupSemantic : Semantic("grp") {
    override fun default(theme: Theme): ThemeAndBack = theme[CardSemantic]
}

data object DismissSemantic : Semantic("dsmss") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        cascading = false,
        gap = 0.dp,
        cornerRadii = CornerRadii.Constant(0.dp),
        background = Color.black.applyAlpha(0.5f),
    )
}

data object FieldSemantic : Semantic("fld") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        cascading = false,
        outlineWidth = 1.px,
//        gap = theme.gap / 2,
        cornerRadii = when (val base = theme.cornerRadii) {
            is CornerRadii.Constant -> CornerRadii.ForceConstant(base.value)
            is CornerRadii.ForceConstant -> base
            is CornerRadii.RatioOfSize -> base
            is CornerRadii.RatioOfSpacing -> CornerRadii.ForceConstant(theme.gap * base.value)
            is CornerRadii.PerCorner -> base
        }
    )
}

data object ButtonSemantic : Semantic("btn") {
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBack
}

data object ClickableSemantic : Semantic("clk") {
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBackButPadding
}

/**
 * Supported on Web only.
 */
data object HoverSemantic : Semantic("hov") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        background = theme.background.map { it.highlight(0.2f) },
        outline = theme.background.map { it.highlight(0.2f).highlight(0.1f) },
        elevation = theme.elevation * 2f,
    )
}

/**
 * Supported on Web and iOS.  Android will use the standard ripple effect instead.
 */
data object DownSemantic : Semantic("dwn") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        background = theme.background.map { it.highlight(0.3f) },
        outline = theme.background.map { it.highlight(0.3f).highlight(0.1f) },
        elevation = theme.elevation / 2f,
    )
}

/**
 * Supported on Web only.
 */
data object FocusSemantic : Semantic("fcs") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        outlineWidth = theme.outlineWidth + 2.dp,
        outline = theme.background.map { it.highlight(1f) },
    )
}

data object DisabledSemantic : Semantic("dis") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        foreground = theme.foreground.applyAlpha(alpha = 0.25f),
        background = theme.background.applyAlpha(alpha = 0.5f),
        outline = theme.outline.applyAlpha(alpha = 0.25f),
    )
}

data object CompactSemantic : Semantic("cmp") {
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBack(
        gap = theme.gap / 2,
        padding = theme.padding / 2,
    )
}

data object SelectedSemantic : Semantic("sel") {
    override fun default(theme: Theme): ThemeAndBack = theme[DownSemantic]
}

data object UnselectedSemantic : Semantic("uns") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        background = theme.background.applyAlpha(alpha = 0f),
        outline = theme.background,
        outlineWidth = 2.dp
    )
}

data object OuterSemantic : Semantic("outer") {
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBack
}

data object MainContentSemantic : Semantic("cnt") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack
}

data object BarSemantic : Semantic("bar") {
    override fun default(theme: Theme): ThemeAndBack = theme[ImportantSemantic]
}

data object SystemBarSemantic : Semantic("sba") {
    override fun default(theme: Theme): ThemeAndBack = theme[BarSemantic]
}

data object NavSemantic : Semantic("nav") {
    override fun default(theme: Theme): ThemeAndBack = theme[BarSemantic]
}

data object DialogSemantic : Semantic("dlg") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack
}

data object PopoverSemantic : Semantic("pop") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack
}

data object ImportantSemantic : Semantic("imp") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        background = theme.foreground,
        outline = theme.foreground,
        foreground = theme.background,
    )
}

data object CriticalSemantic : Semantic("crt") {
    override fun default(theme: Theme): ThemeAndBack = theme[ImportantSemantic][ImportantSemantic]
}

data object WarningSemantic : Semantic("wrn") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        background = Color.fromHex(0xFFe36e24.toInt()),
        outline = Color.fromHex(0xFFe36e24.toInt()).highlight(0.1f),
        foreground = Color.white
    )
}

data object DangerSemantic : Semantic("dgr") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        background = Color.fromHex(0xFFB00020.toInt()),
        outline = Color.fromHex(0xFFB00020.toInt()).highlight(0.1f),
        foreground = Color.white
    )
}

data object AffirmativeSemantic : Semantic("afr") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        background = Color.fromHex(0xFF20a020.toInt()),
        outline = Color.fromHex(0xFF20a020.toInt()).highlight(0.1f),
        foreground = Color.white
    )
}

data object HeaderSemantic : Semantic("hed") {
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBack
}

data class HeaderSizeSemantic(val level: Int) : Semantic("h$level") {
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBack(
        font = theme.font.copy(size = lookup[level - 1].rem),
    )

    companion object {
        val lookup = arrayOf(
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

data object SubtextSemantic : Semantic("sub") {
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBack(
        font = theme.font.copy(size = 0.8.rem),
        foreground = theme.foreground.applyAlpha(0.7f)
    )
}

data object ErrorSemantic : Semantic("err") {
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBack(
        foreground = theme[DangerSemantic].theme.background.closestColor().highlight(0.2f)
    )
}

data object InvalidSemantic : Semantic("ivd") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        outlineWidth = 1.px,
        outline = Color.red
    )
}

data object EmphasizedSemantic : Semantic("emf") {
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBack(
        font = theme.font.copy(italic = true)
    )
}

data object EmbeddedSemantic : Semantic("ebd") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        background = theme.background.closestColor().highlight(-0.1f)
    )
}

data object PrintSemantic : Semantic("print") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        background = Color.white,
        foreground = Color.black,
        outline = theme.background,
        outlineWidth = 2.px,
    )
}


val H1Semantic = HeaderSizeSemantic(1)
val H2Semantic = HeaderSizeSemantic(2)
val H3Semantic = HeaderSizeSemantic(3)
val H4Semantic = HeaderSizeSemantic(4)
val H5Semantic = HeaderSizeSemantic(5)
val H6Semantic = HeaderSizeSemantic(6)

data class Transformation(
    val translationX: Double = 0.0,
    val translationY: Double = 0.0,
    val translationZ: Double = 0.0,
    val rotationX: Double = 0.0,
    val rotationY: Double = 0.0,
    val rotation: Double = 0.0,
    val scaleX: Double = 1.0,
    val scaleY: Double = 1.0,
)

sealed interface ShaderEffect {
    data class Blur(val amount: Dimension) : ShaderEffect
}

class Theme(
    val id: String,

    val font: FontAndStyle = FontAndStyle(systemDefaultFont),

    val elevation: Dimension = 1.px,
    val cornerRadii: CornerRadii = CornerRadii.RatioOfSpacing(1f),

    val gap: Dimension = 1.rem,
    val padding: Edges = Edges(gap),

    val foreground: Paint = Color.black,
    val iconOverride: Paint? = null,
    val outline: Paint = Color.black,
    val outlineWidth: Dimension = 0.px,
    val separatorOverride: Paint? = null,
    val background: Paint = Color.white,

    /**
     * Supported on Web and partially on iOS.
     */
    val blurBackground: Dimension = 0.px,
    val transform: Transformation? = null,

    val bodyTransitions: ScreenTransitions = ScreenTransitions.Fade,
    val dialogTransitions: ScreenTransitions = ScreenTransitions.Fade,
    val transitionDuration: Duration = 0.25.seconds,

    val derivedFrom: Theme? = null,
    val derivationId: String? = null,
    val revert: Theme? = null,

    val semanticOverrides: SemanticOverrides = SemanticOverrides.EMPTY,
) {
    val icon: Paint get() = iconOverride ?: foreground
    val separator: Paint get() = separatorOverride ?: foreground.applyAlpha(0.5f)

    fun with(back: Boolean, padding: Boolean) = if (back) {
        if (padding) withBack
        else withBackNoPadding
    } else {
        if (padding) withoutBackButPadding
        else withoutBack
    }

    val withBack = ThemeAndBack(this, drawBackground = true, padding = true)
    val withBackNoPadding = ThemeAndBack(this, drawBackground = true, padding = false)
    val withoutBack = ThemeAndBack(this, drawBackground = false, padding = false)
    val withoutBackButPadding = ThemeAndBack(this, drawBackground = false, padding = true)

    private val themeCache = HashMap<Semantic, ThemeAndBack>()

    operator fun get(semantic: Semantic): ThemeAndBack = themeCache.getOrPut(semantic) {
        semanticOverrides.derive(semantic, this)
    }

    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?): Boolean {
        return other is Theme && this.id == other.id
    }

    fun customize(
        newId: String,
        font: FontAndStyle = this.font,
        elevation: Dimension = this.elevation,
        cornerRadii: CornerRadii = this.cornerRadii,
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

    fun copy(
        id: String,
        cascading: Boolean = true,
        font: FontAndStyle? = null,
        elevation: Dimension? = null,
        cornerRadii: CornerRadii? = null,
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

    companion object {
        val placeholder = Theme("placeholder")

        private var randomGenId: Int = 0

        fun random(random: Random = Random): Theme {
            val id = "rand${randomGenId++}"
            val hue = random.nextFloat().turns
            val saturation = random.nextFloat() * 0.5f + 0.25f
            val value = random.nextFloat() * 0.5f + 0.25f
            return listOf(
                Theme.material(
                    id = id,
                    primary = HSVColor(hue = hue, saturation = saturation, value = value).toRGB(),
                    secondary = HSVColor(
                        hue = hue + Angle.halfTurn,
                        saturation = 1f - saturation,
                        value = 1f - value
                    ).toRGB(),
                ).randomElevationAndCorners().randomTitleFontSettings(),
                Theme.material(
                    id = id,
                    foreground = Color.white,
                    background = Color.gray(0.2f),
                    primary = HSVColor(hue = hue, saturation = saturation, value = value).toRGB(),
                    secondary = HSVColor(
                        hue = hue + Angle.halfTurn,
                        saturation = 1f - saturation,
                        value = 1f - value
                    ).toRGB(),
                ).randomElevationAndCorners().randomTitleFontSettings(),
                Theme.material3(
                    id = id,
                    primary = HSVColor(hue = hue, saturation = saturation, value = value).toRGB(),
                    secondary = HSVColor(
                        hue = hue + Angle.halfTurn,
                        saturation = 1f - saturation,
                        value = 1f - value
                    ).toRGB(),
                    backgroundAdjust = Random.nextFloat() * 0.15f,
                ).randomElevationAndCorners().randomTitleFontSettings(),
                Theme.material3(
                    id = id,
                    foreground = Color.white,
                    backgroundAdjust = Random.nextFloat() * 0.5f,
                    primary = HSVColor(hue = hue, saturation = saturation, value = value).toRGB(),
                    secondary = HSVColor(
                        hue = hue + Angle.halfTurn,
                        saturation = 1f - saturation,
                        value = 1f - value
                    ).toRGB(),
                ).randomElevationAndCorners().randomTitleFontSettings(),
                Theme.flat(id = id, hue = hue, saturation = 0.15f, baseBrightness = 0.8f)
                    .copy(id = id, cornerRadii = CornerRadii.Constant(Random.nextDouble().rem)).randomTitleFontSettings(),
                Theme.flat(id = id, hue = hue, saturation = 0.5f)
                    .copy(id = id, cornerRadii = CornerRadii.Constant(Random.nextDouble().rem)).randomTitleFontSettings(),
            ).random(random)
        }
    }

    override fun toString(): String = id
}