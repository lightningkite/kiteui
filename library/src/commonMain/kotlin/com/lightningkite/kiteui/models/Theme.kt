package com.lightningkite.kiteui.models

import kotlin.js.JsName
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class ThemeAndBack(val theme: Theme, val useBackground: Boolean) {
    operator fun get(semantic: Semantic): ThemeAndBack {
        val b = theme[semantic]
        return if (useBackground) b.theme.withBack
        else b
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

        inline fun selfBackground(crossinline action: Theme.() -> Theme): ThemeDerivation {
            return object : ThemeDerivation {
                override fun invoke(theme: Theme): ThemeAndBack = action(theme).withBack
            }
        }

        @Suppress("NOTHING_TO_INLINE")
        inline operator fun invoke(theme: Theme): ThemeDerivation = Set(theme)
        val none = ThemeDerivation.None
    }

    data object None : ThemeDerivation {
        override fun invoke(theme: Theme): ThemeAndBack = theme.withoutBack
        override fun plus(other: ThemeDerivation): ThemeDerivation = other
    }

    data class Set(val theme: Theme) : ThemeDerivation {
        override fun invoke(theme: Theme): ThemeAndBack = this.theme.withBack
    }

    open operator fun plus(other: ThemeDerivation): ThemeDerivation {
        return ThemeDerivation {
            if (other is Set) return@ThemeDerivation other.theme.withBack
            val a = this(it)
            val b = other(a.theme)
            if (a.useBackground) b.theme.withBack
            else b
        }
    }
}

interface Semantic : ThemeDerivation {
    val key: String
    fun default(theme: Theme): ThemeAndBack
    override fun invoke(theme: Theme): ThemeAndBack = theme[this]
}

data object LoadingSemantic : Semantic {
    override val key: String = "ld"
    override fun default(theme: Theme): ThemeAndBack = theme.withBack
}

data object WorkingSemantic : Semantic {
    override val key: String = "wrk"
    override fun default(theme: Theme): ThemeAndBack = theme.withBack
}

data object CardSemantic : Semantic {
    override val key: String = "crd"
    override fun default(theme: Theme): ThemeAndBack = theme.withBack
}

data object FieldSemantic : Semantic {
    override val key: String = "fld"
    override fun default(theme: Theme): ThemeAndBack = theme.copy(
        id = "fld",
        outlineWidth = 1.px,
//        spacing = theme.spacing / 2,
        cornerRadii = when(val base = theme.cornerRadii) {
            is CornerRadii.Constant -> CornerRadii.ForceConstant(base.value)
            is CornerRadii.ForceConstant -> base
            is CornerRadii.RatioOfSize -> base
            is CornerRadii.RatioOfSpacing -> CornerRadii.ForceConstant(theme.spacing * base.value)
        }
    ).withBack
}

data object ButtonSemantic : Semantic {
    override val key: String = "btn"
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBack
}

data object HoverSemantic : Semantic {
    override val key: String = "hov"
    override fun default(theme: Theme): ThemeAndBack = theme.copy(
        id = "hov",
        background = theme.background.map { it.highlight(0.2f) },
        outline = theme.background.map { it.highlight(0.2f).highlight(0.1f) },
        elevation = theme.elevation * 2f,
    ).withBack
}

data object DownSemantic : Semantic {
    override val key: String = "dwn"
    override fun default(theme: Theme): ThemeAndBack = theme.copy(
        id = "dwn",
        background = theme.background.map { it.highlight(0.3f) },
        outline = theme.background.map { it.highlight(0.3f).highlight(0.1f) },
        elevation = theme.elevation / 2f,
    ).withBack
}

data object FocusSemantic : Semantic {
    override val key: String = "fcs"
    override fun default(theme: Theme): ThemeAndBack = theme.copy(
        id = "fcs",
        outlineWidth = theme.outlineWidth + 2.dp,
        outline = theme.outline.map { it.highlight(1f) },
    ).withBack
}

data object DisabledSemantic : Semantic {
    override val key: String = "dis"
    override fun default(theme: Theme): ThemeAndBack = theme.copy(
        id = "dis",
        foreground = theme.foreground.applyAlpha(alpha = 0.25f),
        background = theme.background.applyAlpha(alpha = 0.5f),
        outline = theme.outline.applyAlpha(alpha = 0.25f),
    ).withBack
}

data object WorkingSemantic : Semantic {
    override val key: String = "wor"
    override fun default(theme: Theme): ThemeAndBack = theme[DisabledSemantic]
}

data object SelectedSemantic : Semantic {
    override val key: String = "sel"
    override fun default(theme: Theme): ThemeAndBack = theme[DownSemantic]
}

data object UnselectedSemantic : Semantic {
    override val key: String = "uns"
    override fun default(theme: Theme): ThemeAndBack = theme.copy(
        id = "uns",
        background = theme.background.applyAlpha(alpha = 0f),
        outline = theme.background,
        outlineWidth = 2.dp
    ).withBack
}

data object MainContentSemantic : Semantic {
    override val key: String = "cnt"
    override fun default(theme: Theme): ThemeAndBack = theme.withBack
}

data object BarSemantic : Semantic {
    override val key: String = "bar"
    override fun default(theme: Theme): ThemeAndBack = theme[ImportantSemantic]
}

data object SystemBarSemantic : Semantic {
    override val key: String = "sba"
    override fun default(theme: Theme): ThemeAndBack = theme[BarSemantic]
}

data object NavSemantic : Semantic {
    override val key: String = "nav"
    override fun default(theme: Theme): ThemeAndBack = theme[BarSemantic]
}

data object DialogSemantic : Semantic {
    override val key: String = "dlg"
    override fun default(theme: Theme): ThemeAndBack = theme.withBack
}

data object ImportantSemantic : Semantic {
    override val key: String = "imp"
    override fun default(theme: Theme): ThemeAndBack = theme.copy(
        background = theme.foreground,
        outline = theme.foreground,
        foreground = theme.background,
    ).withBack
}

data object CriticalSemantic : Semantic {
    override val key: String = "crt"
    override fun default(theme: Theme): ThemeAndBack = theme[ImportantSemantic][ImportantSemantic]
}

data object WarningSemantic : Semantic {
    override val key: String = "wrn"
    override fun default(theme: Theme): ThemeAndBack = theme.copy(
        id = "wrn",
        background = Color.fromHex(0xFFe36e24.toInt()),
        outline = Color.fromHex(0xFFe36e24.toInt()).highlight(0.1f),
        foreground = Color.white
    ).withBack
}

data object DangerSemantic : Semantic {
    override val key: String = "dgr"
    override fun default(theme: Theme): ThemeAndBack = theme.copy(
        id = "dgr",
        background = Color.fromHex(0xFFB00020.toInt()),
        outline = Color.fromHex(0xFFB00020.toInt()).highlight(0.1f),
        foreground = Color.white
    ).withBack
}

data object AffirmativeSemantic : Semantic {
    override val key: String = "afr"
    override fun default(theme: Theme): ThemeAndBack = theme.copy(
        id = "afr",
        background = Color.fromHex(0xFF20a020.toInt()),
        outline = Color.fromHex(0xFF20a020.toInt()).highlight(0.1f),
        foreground = Color.white
    ).withBack
}

data object HeaderSemantic : Semantic {
    override val key: String = "hed"
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBack
}

data class HeaderSizeSemantic(val level: Int) : Semantic {
    override val key: String = "h$level"
    override fun default(theme: Theme): ThemeAndBack = theme.copy(
        id = key,
        font = theme.font.copy(size = lookup[level - 1].rem),
    ).withoutBack

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

data object SubtextSemantic : Semantic {
    override val key: String = "sub"
    override fun default(theme: Theme): ThemeAndBack = theme.copy(
        id = key,
        font = theme.font.copy(size = 0.8.rem),
        foreground = theme.foreground.applyAlpha(0.7f)
    ).withoutBack
}

data object ErrorSemantic : Semantic {
    override val key: String = "err"
    override fun default(theme: Theme): ThemeAndBack = theme[DangerSemantic]
}

data object InvalidSemantic : Semantic {
    override val key: String = "ivd"
    override fun default(theme: Theme): ThemeAndBack = theme.copy(
        id = key,
        outlineWidth = 1.px,
        outline = Color.red
    ).withBack
}

data object EmphasizedSemantic : Semantic {
    override val key: String = "emf"
    override fun default(theme: Theme): ThemeAndBack = theme.copy(
        id = key,
        font = theme.font.copy(italic = true)
    ).withoutBack
}

data object EmbeddedSemantic : Semantic {
    override val key: String = "ebd"
    override fun default(theme: Theme): ThemeAndBack = theme.copy(
        id = key,
        background = theme.background.closestColor().highlight(-0.1f)
    ).withBack
}

data object PrintSemantic : Semantic {
    override val key: String = "print"
    override fun default(theme: Theme): ThemeAndBack = theme.copy(
        id = key,
        background = Color.white,
        foreground = Color.black,
        outline = theme.background,
        outlineWidth = 2.px,
    ).withBack
}


val H1Semantic = HeaderSizeSemantic(1)
val H2Semantic = HeaderSizeSemantic(2)
val H3Semantic = HeaderSizeSemantic(3)
val H4Semantic = HeaderSizeSemantic(4)
val H5Semantic = HeaderSizeSemantic(5)
val H6Semantic = HeaderSizeSemantic(6)

class Theme(
    val id: String,
    val font: FontAndStyle = FontAndStyle(systemDefaultFont),
    val elevation: Dimension = 1.px,
    val cornerRadii: CornerRadii = CornerRadii.RatioOfSpacing(1f),
    val spacing: Dimension = 1.rem,
    val navSpacing: Dimension = 0.rem,
    val foreground: Paint = Color.black,
    val iconOverride: Paint? = null,
    val outline: Paint = Color.black,
    val outlineWidth: Dimension = 0.px,
    val background: Paint = Color.white,
    val bodyTransitions: ScreenTransitions = ScreenTransitions.Fade,
    val dialogTransitions: ScreenTransitions = ScreenTransitions.Fade,
    val transitionDuration: Duration = 0.15.seconds,

    val derivedFrom: Theme? = null,
    val derivationId: String? = null,
    val revert: Theme? = null,

    val derivations: Map<Semantic, (theme: Theme) -> ThemeAndBack> = mapOf(),
) {
    val icon: Paint get() = iconOverride ?: foreground
    val withBack = ThemeAndBack(this, true)
    val withoutBack = ThemeAndBack(this, false)

    private val themeCache = HashMap<Semantic, ThemeAndBack>()
    operator fun get(semantic: Semantic): ThemeAndBack = themeCache.getOrPut(semantic) {
        derivations[semantic]?.invoke(this) ?: semantic.default(this)
    }

    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?): Boolean {
        return other is Theme && this.id == other.id
    }

    constructor(
        id: String,
        body: FontAndStyle = FontAndStyle(systemDefaultFont),
        title: FontAndStyle = FontAndStyle(systemDefaultFont),
        elevation: Dimension = 1.px,
        cornerRadii: CornerRadii = CornerRadii.RatioOfSpacing(1f),
        spacing: Dimension = 1.rem,
        navSpacing: Dimension = 0.rem,
        foreground: Paint = Color.black,
        iconOverride: Paint? = null,
        outline: Paint = Color.black,
        outlineWidth: Dimension = 0.px,
        background: Paint = Color.white,
        bodyTransitions: ScreenTransitions = ScreenTransitions.Fade,
        dialogTransitions: ScreenTransitions = ScreenTransitions.Fade,
        transitionDuration: Duration = 0.15.seconds,

        derivedFrom: Theme? = null,
        derivationId: String? = null,
        revert: Theme? = null,

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
        spacing = spacing,
        navSpacing = navSpacing,
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
        derivations = buildMap<Semantic, (Theme) -> ThemeAndBack> {
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

    fun copy(
        id: String,
        font: FontAndStyle = this.font,
        elevation: Dimension = this.elevation,
        cornerRadii: CornerRadii = this.cornerRadii,
        spacing: Dimension = this.spacing,
        navSpacing: Dimension = this.navSpacing,
        foreground: Paint = this.foreground,
        iconOverride: Paint? = this.iconOverride,
        outline: Paint = this.outline,
        outlineWidth: Dimension = this.outlineWidth,
        background: Paint = this.background,
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
    ): Theme = Theme(
        id = "${this.id}-$id",
        derivedFrom = this,
        derivationId = id,
        font = font,
        elevation = elevation,
        cornerRadii = cornerRadii,
        spacing = spacing,
        navSpacing = navSpacing,
        foreground = foreground,
        iconOverride = iconOverride,
        outline = outline,
        outlineWidth = outlineWidth,
        background = background,
        bodyTransitions = bodyTransitions,
        dialogTransitions = dialogTransitions,
        transitionDuration = transitionDuration,
        revert = if (revert) this else this.revert?.copy(
            id = id,
            font = font,
            elevation = elevation,
            cornerRadii = cornerRadii,
            spacing = spacing,
            navSpacing = navSpacing,
            foreground = foreground,
            iconOverride = iconOverride,
            outline = outline,
            outlineWidth = outlineWidth,
            background = background,
            bodyTransitions = bodyTransitions,
            dialogTransitions = dialogTransitions,
            transitionDuration = transitionDuration,
            card = card,
            field = field,
            button = button,
            hover = hover,
            focus = focus,
            dialog = dialog,
            down = down,
            unselected = unselected,
            selected = selected,
            disabled = disabled,
            mainContent = mainContent,
            bar = bar,
            nav = nav,
            important = important,
            critical = critical,
            warning = warning,
            danger = danger,
            affirmative = affirmative,
        ),
        derivations = this.derivations + buildMap<Semantic, (Theme) -> ThemeAndBack> {
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

    fun customize(
        newId: String,
        font: FontAndStyle = this.font,
        elevation: Dimension = this.elevation,
        cornerRadii: CornerRadii = this.cornerRadii,
        spacing: Dimension = this.spacing,
        navSpacing: Dimension = this.navSpacing,
        foreground: Paint = this.foreground,
        iconOverride: Paint? = this.iconOverride,
        outline: Paint = this.outline,
        outlineWidth: Dimension = this.outlineWidth,
        background: Paint = this.background,
        bodyTransitions: ScreenTransitions = this.bodyTransitions,
        dialogTransitions: ScreenTransitions = this.dialogTransitions,
        transitionDuration: Duration = this.transitionDuration,
        revert: Boolean = false,
        derivations: Map<Semantic, (Theme) -> ThemeAndBack> = mapOf()
    ): Theme = Theme(
        id = newId,
        font = font,
        elevation = elevation,
        cornerRadii = cornerRadii,
        spacing = spacing,
        navSpacing = navSpacing,
        foreground = foreground,
        iconOverride = iconOverride,
        outline = outline,
        outlineWidth = outlineWidth,
        background = background,
        bodyTransitions = bodyTransitions,
        dialogTransitions = dialogTransitions,
        transitionDuration = transitionDuration,
        revert = if (revert) this else this.revert?.customize(
            newId = newId,
            font = font,
            elevation = elevation,
            cornerRadii = cornerRadii,
            spacing = spacing,
            navSpacing = navSpacing,
            foreground = foreground,
            iconOverride = iconOverride,
            outline = outline,
            outlineWidth = outlineWidth,
            background = background,
            bodyTransitions = bodyTransitions,
            dialogTransitions = dialogTransitions,
            transitionDuration = transitionDuration,
            derivations = derivations,
        ),
        derivations = this.derivations + derivations
    )

    fun copy(
        id: String,
        font: FontAndStyle = this.font,
        elevation: Dimension = this.elevation,
        cornerRadii: CornerRadii = this.cornerRadii,
        spacing: Dimension = this.spacing,
        navSpacing: Dimension = this.navSpacing,
        foreground: Paint = this.foreground,
        iconOverride: Paint? = this.iconOverride,
        outline: Paint = this.outline,
        outlineWidth: Dimension = this.outlineWidth,
        background: Paint = this.background,
        bodyTransitions: ScreenTransitions = this.bodyTransitions,
        dialogTransitions: ScreenTransitions = this.dialogTransitions,
        transitionDuration: Duration = this.transitionDuration,
        revert: Boolean = false,
        derivations: Map<Semantic, (Theme) -> ThemeAndBack> = mapOf()
    ): Theme = Theme(
        id = "${this.id}-$id",
        derivedFrom = this,
        derivationId = id,
        font = font,
        elevation = elevation,
        cornerRadii = cornerRadii,
        spacing = spacing,
        navSpacing = navSpacing,
        foreground = foreground,
        iconOverride = iconOverride,
        outline = outline,
        outlineWidth = outlineWidth,
        background = background,
        bodyTransitions = bodyTransitions,
        dialogTransitions = dialogTransitions,
        transitionDuration = transitionDuration,
        revert = if (revert) this else this.revert?.copy(
            id = id,
            font = font,
            elevation = elevation,
            cornerRadii = cornerRadii,
            spacing = spacing,
            navSpacing = navSpacing,
            foreground = foreground,
            iconOverride = iconOverride,
            outline = outline,
            outlineWidth = outlineWidth,
            background = background,
            bodyTransitions = bodyTransitions,
            dialogTransitions = dialogTransitions,
            transitionDuration = transitionDuration,
            derivations = derivations,
        ),
        derivations = this.derivations + derivations,
    )

    fun copy(
        font: FontAndStyle = this.font,
        title: FontAndStyle? = null,
        body: FontAndStyle? = null,
        elevation: Dimension = this.elevation,
        cornerRadii: CornerRadii = this.cornerRadii,
        spacing: Dimension = this.spacing,
        navSpacing: Dimension = this.navSpacing,
        foreground: Paint = this.foreground,
        iconOverride: Paint? = this.iconOverride,
        outline: Paint = this.outline,
        outlineWidth: Dimension = this.outlineWidth,
        background: Paint = this.background,
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
                out = out * 31 + spacing.hashCode()
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
        return Theme(
            id = "${this.id}-$addedId",
            derivedFrom = this,
            derivationId = addedId,
            font = body ?: font,
            elevation = elevation,
            cornerRadii = cornerRadii,
            spacing = spacing,
            navSpacing = navSpacing,
            foreground = foreground,
            iconOverride = iconOverride,
            outline = outline,
            outlineWidth = outlineWidth,
            background = background,
            bodyTransitions = bodyTransitions,
            dialogTransitions = dialogTransitions,
            transitionDuration = transitionDuration,
            revert = if (revert) this else this.revert?.copy(
                font = font,
                title = title,
                body = body,
                elevation = elevation,
                cornerRadii = cornerRadii,
                spacing = spacing,
                navSpacing = navSpacing,
                foreground = foreground,
                iconOverride = iconOverride,
                outline = outline,
                outlineWidth = outlineWidth,
                background = background,
                bodyTransitions = bodyTransitions,
                dialogTransitions = dialogTransitions,
                transitionDuration = transitionDuration,
            ),
            derivations = this.derivations + buildMap<Semantic, (Theme) -> ThemeAndBack> {
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
    }

    override fun toString(): String = id
}

@Deprecated("Use the new theme derivation system", ReplaceWith("this[CardSemantic].theme", "com.lightningkite.kiteui.models.CardSemantic"))
fun Theme.card() = this[CardSemantic].theme

@Deprecated("Use the new theme derivation system", ReplaceWith("this[FieldSemantic].theme", "com.lightningkite.kiteui.models.FieldSemantic"))
fun Theme.field() = this[FieldSemantic].theme

@Deprecated("Use the new theme derivation system", ReplaceWith("this[ButtonSemantic].theme", "com.lightningkite.kiteui.models.ButtonSemantic"))
fun Theme.button() = this[ButtonSemantic].theme

@Deprecated("Use the new theme derivation system", ReplaceWith("this[HoverSemantic].theme", "com.lightningkite.kiteui.models.HoverSemantic"))
fun Theme.hover() = this[HoverSemantic].theme

@Deprecated("Use the new theme derivation system", ReplaceWith("this[FocusSemantic].theme", "com.lightningkite.kiteui.models.FocusSemantic"))
fun Theme.focus() = this[FocusSemantic].theme

@Deprecated("Use the new theme derivation system", ReplaceWith("this[DialogSemantic].theme", "com.lightningkite.kiteui.models.DialogSemantic"))
fun Theme.dialog() = this[DialogSemantic].theme

@Deprecated("Use the new theme derivation system", ReplaceWith("this[DownSemantic].theme", "com.lightningkite.kiteui.models.DownSemantic"))
fun Theme.down() = this[DownSemantic].theme

@Deprecated("Use the new theme derivation system", ReplaceWith("this[UnselectedSemantic].theme", "com.lightningkite.kiteui.models.UnselectedSemantic"))
fun Theme.unselected() = this[UnselectedSemantic].theme

@Deprecated("Use the new theme derivation system", ReplaceWith("this[SelectedSemantic].theme", "com.lightningkite.kiteui.models.SelectedSemantic"))
fun Theme.selected() = this[SelectedSemantic].theme

@Deprecated("Use the new theme derivation system", ReplaceWith("this[DisabledSemantic].theme", "com.lightningkite.kiteui.models.DisabledSemantic"))
fun Theme.disabled() = this[DisabledSemantic].theme

@Deprecated("Use the new theme derivation system", ReplaceWith("this[MainContentSemantic].theme", "com.lightningkite.kiteui.models.MainContentSemantic"))
fun Theme.mainContent() = this[MainContentSemantic].theme

@Deprecated("Use the new theme derivation system", ReplaceWith("this[BarSemantic].theme", "com.lightningkite.kiteui.models.BarSemantic"))
fun Theme.bar() = this[BarSemantic].theme

@Deprecated("Use the new theme derivation system", ReplaceWith("this[NavSemantic].theme", "com.lightningkite.kiteui.models.NavSemantic"))
fun Theme.nav() = this[NavSemantic].theme

@Deprecated("Use the new theme derivation system", ReplaceWith("this[ImportantSemantic].theme", "com.lightningkite.kiteui.models.ImportantSemantic"))
fun Theme.important() = this[ImportantSemantic].theme

@Deprecated("Use the new theme derivation system", ReplaceWith("this[CriticalSemantic].theme", "com.lightningkite.kiteui.models.CriticalSemantic"))
fun Theme.critical() = this[CriticalSemantic].theme

@Deprecated("Use the new theme derivation system", ReplaceWith("this[WarningSemantic].theme", "com.lightningkite.kiteui.models.WarningSemantic"))
fun Theme.warning() = this[WarningSemantic].theme

@Deprecated("Use the new theme derivation system", ReplaceWith("this[DangerSemantic].theme", "com.lightningkite.kiteui.models.DangerSemantic"))
fun Theme.danger() = this[DangerSemantic].theme

@Deprecated("Use the new theme derivation system", ReplaceWith("this[AffirmativeSemantic].theme", "com.lightningkite.kiteui.models.AffirmativeSemantic"))
fun Theme.affirmative() = this[AffirmativeSemantic].theme

