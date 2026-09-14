package com.lightningkite.kiteui.models

import com.lightningkite.kiteui.views.l2.LabelSemantic
import kotlin.time.Duration.Companion.seconds

/**
 * The Apple look: grouped white cells on a gray page, one tint color, hairline rules, and chrome
 * you can see through.
 *
 * The three things doing the work:
 *
 * - **Content sits in cells, not on the page.** The page is a gray Apple calls
 *   `systemGroupedBackground` and nothing is drawn directly on it; content lives in white cells with
 *   a 10pt continuous radius. That inversion - gray behind, white in front - is the opposite of what
 *   most themes do and is most of why this reads as iOS.
 * - **One tint, and it means "tap me".** Selection, focus, switches and the prominent button all
 *   take the tint; nothing else is colored. Apple ships red, green and orange too, and those are
 *   used here for the status roles and nowhere else.
 * - **Bars are material, not paint.** The nav and tab bars are translucent and blurred, so content
 *   scrolling under them stays faintly visible. A solid bar is the detail that makes an
 *   Apple-styled interface look like a screenshot of one.
 *
 * ```kotlin
 * val light = Theme.clean()
 * val dark = Theme.clean("app-dark", background = Color.black)
 * ```
 *
 * @param id Unique identifier for the theme; must not collide with another theme in the app.
 * @param background The page. Its brightness decides whether the theme runs light or dark. The
 *   defaults are Apple's own grouped backgrounds - `#F2F2F7` and, in dark, true black.
 * @param accent The tint color: selection, focus, switches and prominent buttons. Defaults to
 *   system blue, adjusted per polarity the way Apple's own dynamic colors are.
 * @param title Font used for headings.
 * @param body Font used for everything else, at Apple's 17pt body size.
 */
public fun Theme.Companion.clean(
    id: String = "clean",
    background: Color = Color.fromHexString("#F2F2F7"),
    accent: Color = if (background.perceivedBrightness < 0.5f) Color.fromHexString("#0A84FF")
    else Color.fromHexString("#007AFF"),
    title: FontAndStyle = FontAndStyle(),
    body: FontAndStyle = FontAndStyle(),
): Theme {
    val dark = background.perceivedBrightness < 0.5f

    // Apple's system palette. The grouped ladder runs page -> cell -> nested, and in dark mode it
    // starts at true black so OLED panels switch those pixels off entirely.
    val page = background
    val cell = if (dark) Color.fromHexString("#1C1C1E") else Color.white
    val nested = if (dark) Color.fromHexString("#2C2C2E") else Color.fromHexString("#F2F2F7")
    val label = if (dark) Color.white else Color.black
    val secondaryLabel = if (dark) Color.fromHexString("#98989F") else Color.fromHexString("#6C6C70")
    val separator = if (dark) Color.fromHexString("#38383A") else Color.fromHexString("#C6C6C8")
    // The gray wash Apple puts behind a standalone field or an unselected segment.
    val fill = if (dark) Color.fromHexString("#2C2C2E") else Color.fromHexString("#E9E9EB")

    val danger = if (dark) Color.fromHexString("#FF453A") else Color.fromHexString("#FF3B30")
    val affirmative = if (dark) Color.fromHexString("#30D158") else Color.fromHexString("#34C759")
    val warning = if (dark) Color.fromHexString("#FF9F0A") else Color.fromHexString("#FF9500")

    /**
     * A translucent bar material. Apple's bars are a blur over whatever is behind them rather than a
     * fill, which is why content stays faintly visible as it scrolls underneath.
     */
    val barMaterial = (if (dark) Color.fromHexString("#1C1C1E") else Color.white).applyAlpha(0.82f)

    /** The next surface in the grouped ladder - a cell on the page, a nested group inside a cell. */
    fun Paint.deepen(): Color = if (closestColor() == cell) nested else cell

    /**
     * Supporting text. `secondaryLabel` is a real Apple color rather than the label color at
     * reduced opacity, so it is used wherever it holds up - which is on the theme's own neutral
     * surfaces. On a tinted fill it does not, and there the label is muted only as far as the fill
     * can afford.
     */
    fun supporting(theme: Theme): Color {
        val behind = theme.background.closestColor()
        return if (secondaryLabel contrastAgainst behind >= Color.WCAG_AA_NORMAL_TEXT_CONTRAST_RATIO) secondaryLabel
        else Color.mutedButLegible(theme.foreground.closestColor(), behind)
    }

    /** A solid tinted fill: the prominent button, and the status roles that share its shape. */
    fun Semantic.prominent(theme: Theme, tint: Color): ThemeAndBack = theme.alter(
        cascading = false,
        outlineWidth = 0.dp,
        elevation = 0.dp,
        cornerRadii = CornerRadii.Fixed(0.75.rem),
    ).withBack(background = tint, foreground = tint.maximallyContrastingForeground, outline = tint)

    /**
     * Apple's type scale, and its tracking, which is the part everyone drops. San Francisco is
     * tracked *in* at text sizes and *out* at display sizes; leaving it at zero is what makes an
     * imitation look almost right.
     */
    fun headingFont(level: Int): FontAndStyle = when (level) {
        1 -> title.copy(size = 2.125.rem, weight = 700, additionalLetterSpacing = 0.023.rem, lineSpacingMultiplier = 1.21)
        2 -> title.copy(size = 1.75.rem, weight = 700, additionalLetterSpacing = 0.022.rem, lineSpacingMultiplier = 1.21)
        3 -> title.copy(size = 1.375.rem, weight = 700, additionalLetterSpacing = 0.011.rem, lineSpacingMultiplier = 1.27)
        4 -> title.copy(size = 1.25.rem, weight = 600, additionalLetterSpacing = 0.023.rem, lineSpacingMultiplier = 1.25)
        5 -> title.copy(size = 1.0625.rem, weight = 600, additionalLetterSpacing = (-0.026).rem, lineSpacingMultiplier = 1.29)
        else -> title.copy(size = 0.9375.rem, weight = 600, additionalLetterSpacing = (-0.014).rem, lineSpacingMultiplier = 1.33)
    }

    return Theme(
        id = id,
        font = body.copy(size = 1.0625.rem, additionalLetterSpacing = (-0.026).rem, lineSpacingMultiplier = 1.29),
        elevation = 0.dp,
        // Continuous curvature is the squircle - Apple's corners are not circular arcs, and side by
        // side the difference is obvious even when the radius matches.
        cornerRadii = CornerRadii.Fixed(0.625.rem),
        cornerShape = CornerShape.Continuous,
        gap = 0.75.rem,
        padding = Edges(horizontal = 1.rem, vertical = 0.6875.rem),
        foreground = label,
        background = page,
        outline = separator,
        outlineWidth = 0.dp,
        separatorOverride = separator,
        transitionDuration = 0.25.seconds,
        semanticOverrides = SemanticOverrides(

            // ---- Structure -------------------------------------------------------------------

            OuterSemantic.override {
                it.alter(
                    cascading = false,
                    gap = 0.dp,
                    padding = Edges.ZERO,
                    cornerRadii = CornerRadii.Fixed(0.dp),
                    outlineWidth = 0.dp,
                    elevation = 0.dp,
                ).withBack(background = page, foreground = label)
            },
            MainContentSemantic.override {
                it.alter(cascading = false, cornerRadii = CornerRadii.Fixed(0.dp), outlineWidth = 0.dp, elevation = 0.dp)
                    .withBack(background = page, foreground = label)
            },
            // Translucent, blurred, and separated from the content by a hairline rather than by a
            // shadow - Apple has not used a drop shadow under a nav bar in a decade.
            BarSemantic.override {
                it.alter(
                    cascading = false,
                    cornerRadii = CornerRadii.Fixed(0.dp),
                    outlineWidth = 1.px,
                    outline = separator,
                    elevation = 0.dp,
                    blurBackground = 20.px,
                    padding = Edges(horizontal = 0.75.rem, vertical = 0.5.rem),
                    gap = 0.5.rem,
                ).withBack(background = barMaterial, foreground = label)
            },
            NavSemantic.override {
                it.alter(
                    cascading = false,
                    cornerRadii = CornerRadii.Fixed(0.dp),
                    outlineWidth = 1.px,
                    outline = separator,
                    elevation = 0.dp,
                    blurBackground = 20.px,
                    padding = Edges(0.5.rem),
                    gap = 0.125.rem,
                ).withBack(background = barMaterial, foreground = label)
            },

            // A cell. Everything the user reads sits in one of these; nothing is drawn straight onto
            // the page.
            CardSemantic.override {
                it.alter(
                    cascading = false,
                    outlineWidth = 0.dp,
                    elevation = 0.dp,
                    cornerRadii = CornerRadii.Fixed(0.625.rem),
                    padding = Edges(1.rem),
                ).withBack(background = it.background.deepen(), foreground = label)
            },
            GroupSemantic.override { it[CardSemantic] },
            InsetSemantic.override {
                it.alter(cascading = false, outlineWidth = 0.dp, elevation = 0.dp)
                    .withBack(background = it.background.deepen())
            },
            EmbeddedSemantic.override {
                it.alter(cascading = false, outlineWidth = 0.dp, elevation = 0.dp)
                    .withBack(background = it.background.deepen())
            },
            // A hairline gap, so rows that paint themselves separate the way table cells do without
            // the list itself having to draw anything.
            ListSemantic.override { it.withoutBack(cascading = false, gap = 1.px) },

            // An alert: a small blurred panel with a 14pt radius, held up by the dimming behind it
            // rather than by a shadow.
            DialogSemantic.override {
                it.alter(
                    cascading = false,
                    outlineWidth = 0.dp,
                    elevation = 0.dp,
                    blurBackground = 30.px,
                    cornerRadii = CornerRadii.Fixed(0.875.rem),
                    padding = Edges(1.25.rem),
                ).withBack(background = cell.applyAlpha(0.94f), foreground = label)
            },
            PopoverSemantic.override {
                it.alter(
                    cascading = false,
                    outlineWidth = 0.dp,
                    elevation = 2.dp,
                    blurBackground = 30.px,
                    cornerRadii = CornerRadii.Fixed(0.875.rem),
                    padding = Edges(vertical = 0.25.rem, horizontal = 0.dp),
                    gap = 0.dp,
                ).withBack(background = cell.applyAlpha(0.94f), foreground = label)
            },
            DismissSemantic.override {
                it.withBack(
                    cascading = false,
                    gap = 0.dp,
                    padding = Edges.ZERO,
                    cornerRadii = CornerRadii.Fixed(0.dp),
                    background = Color.black.applyAlpha(0.4f),
                )
            },

            // ---- Typography ------------------------------------------------------------------

            HeaderSemantic.override { it.withoutBack(font = headingFont(5)) },
            override<HeaderSizeSemantic> { it.withoutBack(font = headingFont(level)) },
            // `secondaryLabel`, Apple's own supporting-text color, rather than the body color at
            // reduced opacity.
            SubtextSemantic.override {
                it.withoutBack(
                    font = it.font.copy(size = 0.8125.rem, additionalLetterSpacing = (-0.005).rem),
                    foreground = supporting(it),
                )
            },
            // A grouped-table section header: small, caps, and tracked out.
            LabelSemantic.override {
                it.withoutBack(
                    font = it.font.copy(size = 0.8125.rem, weight = 400, additionalLetterSpacing = (-0.005).rem),
                    foreground = supporting(it),
                )
            },
            TableHeaderSemantic.override {
                it.withoutBack(
                    font = it.font.copy(size = 0.8125.rem, weight = 400, allCaps = true, additionalLetterSpacing = 0.03.rem),
                    foreground = supporting(it),
                )
            },
            BlockquoteSemantic.override {
                it.withoutBack(
                    font = it.font.copy(italic = true),
                    padding = Edges(left = it.gap * 2, top = it.gap / 2, right = it.gap, bottom = it.gap / 2),
                    foreground = supporting(it),
                )
            },
            CodeBlockSemantic.override {
                it.alter(
                    cascading = false,
                    outlineWidth = 0.dp,
                    elevation = 0.dp,
                    cornerRadii = CornerRadii.Fixed(0.625.rem),
                ).withBack(
                    font = it.font.copy(font = systemDefaultFixedWidthFont, size = it.font.size * 0.9f),
                    background = it.background.closestColor().let {
                        if (dark) it.toWhite(0.07f) else it.toBlack(0.05f)
                    },
                )
            },

            // ---- Emphasis and status ---------------------------------------------------------

            ImportantSemantic.override {
                // Already tinted - a prominent button inside a tinted container, or `important`
                // stacked twice. Apple's answer there is its gray secondary button: the neutral
                // fill with the tint carried into the label, not another tinted slab.
                if (it.background.closestColor() == accent) it.alter(
                    cascading = false,
                    outlineWidth = 0.dp,
                    elevation = 0.dp,
                    cornerRadii = CornerRadii.Fixed(0.75.rem),
                ).withBack(background = fill, foreground = Color.legibleOn(accent, fill), outline = fill)
                else prominent(it, accent)
            },
            WarningSemantic.override { prominent(it, warning) },
            DangerSemantic.override { prominent(it, danger) },
            AffirmativeSemantic.override { prominent(it, affirmative) },
            ErrorSemantic.override { it.withoutBack(foreground = Color.legibleOn(danger, it.background.closestColor())) },

            // ---- Interaction -----------------------------------------------------------------

            // A standalone field is a gray wash with no border at all; the border only appears when
            // something is wrong.
            FieldSemantic.override {
                it.alter(
                    cascading = false,
                    outlineWidth = 0.dp,
                    elevation = 0.dp,
                    cornerRadii = CornerRadii.Fixed(0.625.rem),
                    padding = Edges(horizontal = 0.75.rem, vertical = 0.6875.rem),
                ).withBack(background = fill, foreground = label)
            },
            InvalidSemantic.override { it.withBack(cascading = false, outlineWidth = 2.dp, outline = danger) },
            FocusSemantic.override { it.withBack(cascading = false, outlineWidth = 2.dp, outline = accent) },

            // Selection is the tint: a light wash of it behind text drawn in it. The tint is
            // deepened where the wash alone would leave it short of legible.
            SelectedSemantic.override {
                val wash = Color.interpolate(it.background.closestColor(), accent, 0.16f)
                it.alter(
                    cascading = false,
                    outlineWidth = 0.dp,
                    elevation = 0.dp,
                    cornerRadii = CornerRadii.Fixed(0.5.rem),
                ).withBack(
                    background = wash,
                    foreground = if (accent contrastAgainst wash >= Color.WCAG_AA_NORMAL_TEXT_CONTRAST_RATIO) accent
                    else wash.maximallyContrastingForeground,
                    font = it.font.copy(weight = 600),
                    iconOverride = null,
                )
            },
            UnselectedSemantic.override {
                it.alter(
                    outlineWidth = 0.dp,
                    cornerRadii = CornerRadii.Fixed(0.5.rem),
                    foreground = secondaryLabel,
                    iconOverride = null,
                ).withoutBackButPadding
            },

            HoverSemantic.override {
                it.withBack(background = it.background.closestColor().let {
                    if (dark) it.toWhite(0.06f) else it.toBlack(0.045f)
                })
            },
            DownSemantic.override {
                it.withBack(background = it.background.closestColor().let {
                    if (dark) it.toWhite(0.12f) else it.toBlack(0.09f)
                })
            },
            // Apple dims a disabled control to about a third rather than graying its container.
            DisabledSemantic.override {
                it.withoutBack(
                    elevation = 0.dp,
                    foreground = it.foreground.applyAlpha(0.35f),
                    background = it.background.applyAlpha(0.35f),
                    outline = it.outline.applyAlpha(0.35f),
                    iconOverride = null,
                )
            },
            // Switches, sliders and steppers are the tint by definition.
            InteractiveSemantic.override {
                it.withoutBack(
                    foreground = if (it.background.closestColor() == accent) cell else accent,
                    iconOverride = null,
                )
            },
        ),
    )
}
