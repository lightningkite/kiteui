package com.lightningkite.kiteui.models

import com.lightningkite.kiteui.Platform
import com.lightningkite.kiteui.probablyAppleUser
import kotlin.random.Random
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

val __defaultBuilder = ThemeBuilder()

abstract class Semantic(val key: String) : ThemeDerivation {
    open fun default(theme: Theme): ThemeAndBack = theme.withoutBack
    override fun invoke(theme: Theme): ThemeAndBack = theme[this]

    object Registry {

    }

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
        derivations: Map<Semantic, Semantic.(theme: Theme) -> ThemeAndBack> = mapOf(),
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
        derivations = derivations,
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
        derivations: Map<Semantic, Semantic.(theme: Theme) -> ThemeAndBack> = mapOf(),
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
        derivations = derivations,
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
        derivations: Map<Semantic, Semantic.(theme: Theme) -> ThemeAndBack> = mapOf(),
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
        derivations = derivations,
    )
}

data object ForcePaddingSemantic : Semantic("fpad") {
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBackButPadding
}

data object InteractiveSemantic : Semantic("int") {
    override fun default(theme: Theme): ThemeAndBack {
        // iOS switch?
        if (Platform.probablyAppleUser) {
            return theme.withoutBack(
                foreground = if (theme.background.closestColor().perceivedBrightness in 0.1f..0.9f)
                    theme.foreground
                else
                    Color(1f, 0f, 122f / 255f, 255f),
                iconOverride = null,
            )
        } else {
            return theme.withoutBack
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
    override fun default(theme: Theme): ThemeAndBack = theme[DangerSemantic]
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

class ThemeBuilder {
    private var inUse = false
    fun __reset(base: Theme, background: Boolean, padding: Boolean, id: String? = null) {
        if (inUse) throw Exception()
        inUse = true
        this.base = base
        derivationId = ""
        this.id = id?.let { base.id + "-" + it } ?: base.id
        font = base.font
        elevation = base.elevation
        cornerRadii = base.cornerRadii
        gap = base.gap
        this.padding = base.padding
        foreground = base.foreground
        iconOverride = base.iconOverride
        outline = base.outline
        outlineWidth = base.outlineWidth
        this.background = base.background
        bodyTransitions = base.bodyTransitions
        dialogTransitions = base.dialogTransitions
        transitionDuration = base.transitionDuration
        derivedFrom = base
        paddingOnImmediateElement = padding
        drawBackgroundOnImmediateElement = background
    }

    var base: Theme = Theme.placeholder
    var derivationId: String = ""

    var id: String = base.id

    var font: FontAndStyle = base.font

    var elevation: Dimension = base.elevation
    var cornerRadii: CornerRadii = base.cornerRadii

    var gap: Dimension = base.gap
    var padding: Edges = base.padding

    var foreground: Paint = base.foreground
    var iconOverride: Paint? = base.iconOverride
    var outline: Paint = base.outline
    var outlineWidth: Dimension = base.outlineWidth
    var background: Paint = base.background

    var bodyTransitions: ScreenTransitions = base.bodyTransitions
    var dialogTransitions: ScreenTransitions = base.dialogTransitions
    var transitionDuration: Duration = base.transitionDuration

    var derivedFrom: Theme? = base
    var revert: Theme? = null

    private var immediateBuilder: (ThemeBuilder.() -> Unit)? = null
    fun justThisElement(setup: ThemeBuilder.() -> Unit) {
        val old = immediateBuilder
        immediateBuilder = {
            old?.invoke(this)
            setup(this)
        }
    }

    fun nonCascading(setup: ThemeBuilder.() -> Unit) = justThisElement(setup)

    private var _derivations: MutableMap<Semantic, Semantic.(theme: Theme) -> ThemeAndBack>? = null
    val derivations by lazy {
        base.derivations.toMutableMap().also { _derivations = it }
    }

    var drawBackgroundOnImmediateElement: Boolean = true
    var paddingOnImmediateElement: Boolean = true

    internal fun build(): Theme {
        inUse = false
        val cascading = Theme(
            id = id,
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
            bodyTransitions = bodyTransitions,
            dialogTransitions = dialogTransitions,
            transitionDuration = transitionDuration,
            derivedFrom = derivedFrom,
            derivationId = derivationId,
            revert = revert,
            derivations = _derivations ?: base.derivations,
        )
        return immediateBuilder?.let {
            __reset(cascading, false, false, id = "immediate")
            this.revert = cascading
            build()
        } ?: cascading
    }

    fun __buildAndSelect(): ThemeAndBack {
        return build().with(drawBackgroundOnImmediateElement, paddingOnImmediateElement)
    }
}

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

    val derivations: Map<Semantic, Semantic.(theme: Theme) -> ThemeAndBack> = mapOf(),
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

    val withBack = ThemeAndBack(this, true, true)
    val withBackNoPadding = ThemeAndBack(this, true, false)
    val withoutBack = ThemeAndBack(this, false, false)
    val withoutBackButPadding = ThemeAndBack(this, false, true)

    private val themeCache = HashMap<Semantic, ThemeAndBack>()
    operator fun get(semantic: Semantic): ThemeAndBack = themeCache.getOrPut(semantic) {
        derivations[semantic]?.invoke(semantic, this) ?: semantic.default(this)
    }

    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?): Boolean {
        return other is Theme && this.id == other.id
    }

    inline fun alter(crossinline alterations: ThemeBuilder.() -> Unit): ThemeAndBack {
        __defaultBuilder.__reset(this, true, true)
        alterations(__defaultBuilder)
        return __defaultBuilder.__buildAndSelect()
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
        derivations: Map<Semantic, Semantic.(theme: Theme) -> ThemeAndBack> = mapOf(),
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
        derivations = this.derivations + derivations
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
        derivations: Map<Semantic, Semantic.(theme: Theme) -> ThemeAndBack> = mapOf(),
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
        derivations = this.derivations + derivations,
        revert = if (!cascading) this else this.revert?.copy(
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
            derivations = derivations,
        )
    )

    @Deprecated("Use the new copy with 'cascading' instead.")
    fun copy(
        id: String,
        font: FontAndStyle = this.font,
        elevation: Dimension = this.elevation,
        cornerRadii: CornerRadii = this.cornerRadii,
        gap: Dimension = this.gap,
        padding: Edges = this.padding,
        foreground: Paint = this.foreground,
        iconOverride: Paint? = this.iconOverride,
        outline: Paint = this.outline,
        outlineWidth: Dimension = this.outlineWidth,
        background: Paint = this.background,
        blurBackground: Dimension = this.blurBackground,
        transform: Transformation? = this.transform,
        bodyTransitions: ScreenTransitions = this.bodyTransitions,
        dialogTransitions: ScreenTransitions = this.dialogTransitions,
        transitionDuration: Duration = this.transitionDuration,
        revert: Boolean,
        derivations: Map<Semantic, Semantic.(theme: Theme) -> ThemeAndBack> = mapOf(),
    ): Theme = Theme(
        id = "${this.id}-$id",
        derivedFrom = derivedFrom,
        derivationId = derivationId,
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
        blurBackground = blurBackground,
        transform = transform,
        bodyTransitions = bodyTransitions,
        dialogTransitions = dialogTransitions,
        transitionDuration = transitionDuration,
        derivations = this.derivations + derivations,
        revert = if (revert) this else null
    )


    @Deprecated("Use new constructor")
    constructor(
        id: String,
        body: FontAndStyle = FontAndStyle(systemDefaultFont),
        title: FontAndStyle = FontAndStyle(systemDefaultFont),
        elevation: Dimension = 1.px,
        cornerRadii: CornerRadii = CornerRadii.RatioOfSpacing(1f),
        gap: Dimension = 1.rem,
        padding: Edges = Edges(gap),
        foreground: Paint = Color.black,
        iconOverride: Paint? = null,
        outline: Paint = Color.black,
        outlineWidth: Dimension = 0.px,
        background: Paint = Color.white,
        blurBackground: Dimension = 0.px,
        transform: Transformation? = null,
        bodyTransitions: ScreenTransitions = ScreenTransitions.Fade,
        dialogTransitions: ScreenTransitions = ScreenTransitions.Fade,
        transitionDuration: Duration = 0.15.seconds,

        derivedFrom: Theme? = null,
        derivationId: String? = null,

        card: (Theme.() -> Theme?)? = null,
        field: (Theme.() -> Theme?)? = null,
        button: (Theme.() -> Theme?)? = null,
        hover: (Theme.() -> Theme?)? = null,
        focus: (Theme.() -> Theme?)? = null,
        dialog: (Theme.() -> Theme?)? = null,
        down: (Theme.() -> Theme?)? = null,
        unselected: (Theme.() -> Theme?)? = null,
        selected: (Theme.() -> Theme?)? = null,
        disabled: (Theme.() -> Theme?)? = null,
        mainContent: (Theme.() -> Theme?)? = null,
        bar: (Theme.() -> Theme?)? = null,
        nav: (Theme.() -> Theme?)? = null,
        important: (Theme.() -> Theme?)? = null,
        critical: (Theme.() -> Theme?)? = null,
        warning: (Theme.() -> Theme?)? = null,
        danger: (Theme.() -> Theme?)? = null,
        affirmative: (Theme.() -> Theme?)? = null,
    ) : this(
        id = id,
        font = body,
        elevation = elevation,
        cornerRadii = cornerRadii,
        gap = gap,
        padding = padding,
        foreground = foreground,
        iconOverride = iconOverride,
        outline = outline,
        outlineWidth = outlineWidth,
        background = background,
        blurBackground = blurBackground,
        transform = transform,
        bodyTransitions = bodyTransitions,
        dialogTransitions = dialogTransitions,
        transitionDuration = transitionDuration,
        derivedFrom = derivedFrom,
        derivationId = derivationId,
        derivations = buildMap<Semantic, Semantic.(Theme) -> ThemeAndBack> {
            put(HeaderSemantic) {
                it.copy(
                    id = "hed",
                    font = title,
                ).withoutBack
            }
            card?.let { put(CardSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
            field?.let { put(FieldSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
            button?.let { put(ButtonSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
            hover?.let { put(HoverSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
            focus?.let { put(FocusSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
            dialog?.let { put(DialogSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
            down?.let { put(DownSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
            unselected?.let { put(UnselectedSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
            selected?.let { put(SelectedSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
            disabled?.let { put(DisabledSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
            mainContent?.let { put(MainContentSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
            bar?.let { put(BarSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
            nav?.let { put(NavSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
            important?.let { put(ImportantSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
            critical?.let { put(CriticalSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
            warning?.let { put(WarningSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
            danger?.let { put(DangerSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
            affirmative?.let { put(AffirmativeSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
        }
    )

    @Deprecated("Use new copy")
    fun copy(
        id: String,
        font: FontAndStyle = this.font,
        elevation: Dimension = this.elevation,
        cornerRadii: CornerRadii = this.cornerRadii,
        gap: Dimension = this.gap,
        padding: Edges = this.padding,
        foreground: Paint = this.foreground,
        iconOverride: Paint? = this.iconOverride,
        outline: Paint = this.outline,
        outlineWidth: Dimension = this.outlineWidth,
        background: Paint = this.background,
        blurBackground: Dimension = this.blurBackground,
        transform: Transformation? = this.transform,
        bodyTransitions: ScreenTransitions = this.bodyTransitions,
        dialogTransitions: ScreenTransitions = this.dialogTransitions,
        transitionDuration: Duration = this.transitionDuration,
        revert: Boolean = false,
        card: (Theme.() -> Theme?)? = null,
        field: (Theme.() -> Theme?)? = null,
        button: (Theme.() -> Theme?)? = null,
        hover: (Theme.() -> Theme?)? = null,
        focus: (Theme.() -> Theme?)? = null,
        dialog: (Theme.() -> Theme?)? = null,
        down: (Theme.() -> Theme?)? = null,
        unselected: (Theme.() -> Theme?)? = null,
        selected: (Theme.() -> Theme?)? = null,
        disabled: (Theme.() -> Theme?)? = null,
        mainContent: (Theme.() -> Theme?)? = null,
        bar: (Theme.() -> Theme?)? = null,
        nav: (Theme.() -> Theme?)? = null,
        important: (Theme.() -> Theme?)? = null,
        critical: (Theme.() -> Theme?)? = null,
        warning: (Theme.() -> Theme?)? = null,
        danger: (Theme.() -> Theme?)? = null,
        affirmative: (Theme.() -> Theme?)? = null,
    ): Theme = copy(
        id = id,
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
        blurBackground = blurBackground,
        transform = transform,
        bodyTransitions = bodyTransitions,
        dialogTransitions = dialogTransitions,
        transitionDuration = transitionDuration,
        revert = revert,
        derivations = derivations + buildMap<Semantic, Semantic.(Theme) -> ThemeAndBack> {
            card?.let { put(CardSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
            field?.let { put(FieldSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
            button?.let { put(ButtonSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
            hover?.let { put(HoverSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
            focus?.let { put(FocusSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
            dialog?.let { put(DialogSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
            down?.let { put(DownSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
            unselected?.let { put(UnselectedSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
            selected?.let { put(SelectedSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
            disabled?.let { put(DisabledSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
            mainContent?.let { put(MainContentSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
            bar?.let { put(BarSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
            nav?.let { put(NavSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
            important?.let { put(ImportantSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
            critical?.let { put(CriticalSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
            warning?.let { put(WarningSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
            danger?.let { put(DangerSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
            affirmative?.let { put(AffirmativeSemantic, { t -> it(t)?.withBack ?: t.withoutBack }) }
        }
    )

    @Deprecated("Use new copy where there is a single font and ID is provided")
    fun copy(
        font: FontAndStyle = this.font,
        title: FontAndStyle? = null,
        body: FontAndStyle? = null,
        elevation: Dimension = this.elevation,
        cornerRadii: CornerRadii = this.cornerRadii,
        gap: Dimension = this.gap,
        padding: Edges = this.padding,
        foreground: Paint = this.foreground,
        iconOverride: Paint? = this.iconOverride,
        outline: Paint = this.outline,
        outlineWidth: Dimension = this.outlineWidth,
        background: Paint = this.background,
        blurBackground: Dimension = this.blurBackground,
        transform: Transformation? = this.transform,
        bodyTransitions: ScreenTransitions = this.bodyTransitions,
        dialogTransitions: ScreenTransitions = this.dialogTransitions,
        transitionDuration: Duration = this.transitionDuration,
        revert: Boolean = false,
    ): Theme {
        val addedId = "cp${
            run {
                var out = 0
                out = out * 31 + font.hashCode()
                out = out * 31 + elevation.hashCode()
                out = out * 31 + cornerRadii.hashCode()
                out = out * 31 + gap.hashCode()
                out = out * 31 + foreground.hashCode()
                out = out * 31 + iconOverride.hashCode()
                out = out * 31 + outline.hashCode()
                out = out * 31 + outlineWidth.hashCode()
                out = out * 31 + background.hashCode()
                buildString {
                    append(((out shr 12).mod(64)).let { shortCodeChars[it] })
                    append(((out shr 6).mod(64)).let { shortCodeChars[it] })
                    append((out.mod(64)).let { shortCodeChars[it] })
                }
            }
        }"
        return copy(
            id = addedId,
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
            blurBackground = blurBackground,
            transform = transform,
            bodyTransitions = bodyTransitions,
            dialogTransitions = dialogTransitions,
            transitionDuration = transitionDuration,
            revert = revert,
            derivations = this.derivations + buildMap<Semantic, Semantic.(Theme) -> ThemeAndBack> {
                title?.let { title ->
                    put(HeaderSemantic) {
                        it.copy(
                            id = "hed",
                            font = title,
                        ).withoutBack
                    }
                }
            },
        )
    }

    companion object {
        val placeholder = Theme("placeholder")
        val shortCodeChars = "1234567890QWERTYUIOPASDFGHJKLZXCVBNMqwertyuiopasdfghjklzxcvbnm-_"
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
                    .copy(cornerRadii = CornerRadii.Constant(Random.nextDouble().rem)).randomTitleFontSettings(),
                Theme.flat(id = id, hue = hue, saturation = 0.5f)
                    .copy(cornerRadii = CornerRadii.Constant(Random.nextDouble().rem)).randomTitleFontSettings(),
            ).random(random)
        }
    }

    override fun toString(): String = id
}

@Deprecated(
    "Use the new theme derivation system",
    ReplaceWith("this[CardSemantic].theme", "com.lightningkite.kiteui.models.CardSemantic")
)
fun Theme.card() = this[CardSemantic].theme

@Deprecated(
    "Use the new theme derivation system",
    ReplaceWith("this[FieldSemantic].theme", "com.lightningkite.kiteui.models.FieldSemantic")
)
fun Theme.field() = this[FieldSemantic].theme

@Deprecated(
    "Use the new theme derivation system",
    ReplaceWith("this[ButtonSemantic].theme", "com.lightningkite.kiteui.models.ButtonSemantic")
)
fun Theme.button() = this[ButtonSemantic].theme

@Deprecated(
    "Use the new theme derivation system",
    ReplaceWith("this[HoverSemantic].theme", "com.lightningkite.kiteui.models.HoverSemantic")
)
fun Theme.hover() = this[HoverSemantic].theme

@Deprecated(
    "Use the new theme derivation system",
    ReplaceWith("this[FocusSemantic].theme", "com.lightningkite.kiteui.models.FocusSemantic")
)
fun Theme.focus() = this[FocusSemantic].theme

@Deprecated(
    "Use the new theme derivation system",
    ReplaceWith("this[DialogSemantic].theme", "com.lightningkite.kiteui.models.DialogSemantic")
)
fun Theme.dialog() = this[DialogSemantic].theme

@Deprecated(
    "Use the new theme derivation system",
    ReplaceWith("this[DownSemantic].theme", "com.lightningkite.kiteui.models.DownSemantic")
)
fun Theme.down() = this[DownSemantic].theme

@Deprecated(
    "Use the new theme derivation system",
    ReplaceWith("this[UnselectedSemantic].theme", "com.lightningkite.kiteui.models.UnselectedSemantic")
)
fun Theme.unselected() = this[UnselectedSemantic].theme

@Deprecated(
    "Use the new theme derivation system",
    ReplaceWith("this[SelectedSemantic].theme", "com.lightningkite.kiteui.models.SelectedSemantic")
)
fun Theme.selected() = this[SelectedSemantic].theme

@Deprecated(
    "Use the new theme derivation system",
    ReplaceWith("this[DisabledSemantic].theme", "com.lightningkite.kiteui.models.DisabledSemantic")
)
fun Theme.disabled() = this[DisabledSemantic].theme

@Deprecated(
    "Use the new theme derivation system",
    ReplaceWith("this[MainContentSemantic].theme", "com.lightningkite.kiteui.models.MainContentSemantic")
)
fun Theme.mainContent() = this[MainContentSemantic].theme

@Deprecated(
    "Use the new theme derivation system",
    ReplaceWith("this[BarSemantic].theme", "com.lightningkite.kiteui.models.BarSemantic")
)
fun Theme.bar() = this[BarSemantic].theme

@Deprecated(
    "Use the new theme derivation system",
    ReplaceWith("this[NavSemantic].theme", "com.lightningkite.kiteui.models.NavSemantic")
)
fun Theme.nav() = this[NavSemantic].theme

@Deprecated(
    "Use the new theme derivation system",
    ReplaceWith("this[ImportantSemantic].theme", "com.lightningkite.kiteui.models.ImportantSemantic")
)
fun Theme.important() = this[ImportantSemantic].theme

@Deprecated(
    "Use the new theme derivation system",
    ReplaceWith("this[CriticalSemantic].theme", "com.lightningkite.kiteui.models.CriticalSemantic")
)
fun Theme.critical() = this[CriticalSemantic].theme

@Deprecated(
    "Use the new theme derivation system",
    ReplaceWith("this[WarningSemantic].theme", "com.lightningkite.kiteui.models.WarningSemantic")
)
fun Theme.warning() = this[WarningSemantic].theme

@Deprecated(
    "Use the new theme derivation system",
    ReplaceWith("this[DangerSemantic].theme", "com.lightningkite.kiteui.models.DangerSemantic")
)
fun Theme.danger() = this[DangerSemantic].theme

@Deprecated(
    "Use the new theme derivation system",
    ReplaceWith("this[AffirmativeSemantic].theme", "com.lightningkite.kiteui.models.AffirmativeSemantic")
)
fun Theme.affirmative() = this[AffirmativeSemantic].theme

