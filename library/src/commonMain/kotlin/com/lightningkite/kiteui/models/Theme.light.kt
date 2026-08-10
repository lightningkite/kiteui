package com.lightningkite.kiteui.models

import com.lightningkite.kiteui.views.l2.LabelSemantic
import kotlin.time.Duration.Companion.seconds

/**
 * A light theme built on one rule: **ink is content, accent is action.**
 *
 * Surfaces separate by light rather than by lines - each sheet steps brighter than the one behind
 * it and casts a soft shadow - so a 1px edge is free to mean something. In this theme an outline
 * appears only on things you can act on: a field, a selected item, a focus ring, an invalid entry.
 * Color follows the same rule. Headings are ink, not accent; the accent is reserved for primary
 * actions, selection and focus, so anything tinted is something you can do.
 *
 * The page itself is a low-chroma tint of [accent] rather than neutral gray, which carries an app's
 * identity onto the canvas instead of leaving it confined to buttons.
 *
 * ```kotlin
 * val appTheme = Theme.light(accent = Color.fromHexString("#0E5C6B"))
 * ```
 *
 * ### Why this exists
 *
 * The other built-in themes derive their states with [Color.highlight], which moves *away* from
 * whatever the surface already is - toward white on a dark surface, toward black on a light one.
 * That reads correctly in dark mode, where "raised" really does mean "lighter." In light mode it
 * inverts the depth cue: cards, hover states and code blocks all come out darker than the page,
 * and any of them that start at white have nowhere to go at all. This theme builds an explicit
 * surface ladder instead, and expresses interaction states as a wash of [accent] rather than as a
 * change in lightness.
 *
 * @param id Unique identifier for the theme; must not collide with another theme in the app.
 * @param accent The brand color, and the source of the theme's neutrals. Carries primary actions,
 *   selection and focus, so it should be dark enough to hold white text - anything too pale is
 *   deepened automatically where it has to read as text.
 * @param warning Fill for [WarningSemantic]. The defaults for this and the two below are picked to
 *   sit at roughly the same perceived brightness as [accent], which is what lets four different
 *   hues read as one set of controls rather than as safety signage stuck onto a calm page. A
 *   replacement that is much brighter than the accent will pull the eye away from primary actions.
 * @param danger Fill for [DangerSemantic], and the color of error text and invalid field outlines.
 * @param affirmative Fill for [AffirmativeSemantic].
 * @param title Font used for headings.
 * @param body Font used for everything else.
 */
public fun Theme.Companion.light(
    id: String = "light",
    accent: Color = Color.fromHexString("#0E5C6B"),
    warning: Color = Color.fromHexString("#7A4A12"),
    danger: Color = Color.fromHexString("#8E2C24"),
    affirmative: Color = Color.fromHexString("#256B2A"),
    title: FontAndStyle = FontAndStyle(),
    body: FontAndStyle = FontAndStyle(lineSpacingMultiplier = 1.5),
): Theme {
    val hue = accent.toHSP().hue

    // The surface ladder, all drawn from the accent's hue so the neutrals belong to the brand
    // rather than sitting under it. Chroma rises as surfaces recede: paper is pure white, the desk
    // it sits on carries a trace of the accent, and a well carries a little more. The trace has to
    // stay near the threshold of noticing - enough that the page is not default gray, not enough
    // to read as a colored panel.
    val paper = Color.white
    val desk = HSPColor(hue = hue, saturation = 0.045f, brightness = 0.96f).toRGB()
    val well = HSPColor(hue = hue, saturation = 0.06f, brightness = 0.935f).toRGB()

    val ink = HSPColor(hue = hue, saturation = 0.25f, brightness = 0.16f).toRGB()
    // Separators divide content, which is information, so they survive the no-lines rule. Boxes
    // do not get one.
    val rule = Color.interpolate(desk, ink, 0.18f)
    val fieldEdge = Color.interpolate(paper, ink, 0.35f)

    /** True for surfaces that take dark text - the light half of the ladder. */
    fun Paint.isLight(): Boolean = closestColor().perceivedBrightness > 0.6f

    /**
     * The next sheet up. White is the top of the ladder, so a sheet stacked on white becomes a
     * recessed well instead - a tray inside a card, which is the only reading that stays physical.
     */
    fun Paint.raise(): Color = if (closestColor() == paper) well else paper

    /** A surface one step down - inset wells, code blocks, embedded content. */
    fun Paint.recess(amount: Float = 0.07f): Color = closestColor().let {
        if (it.isLight()) Color.interpolate(it, ink, amount) else it.toBlack(amount * 2)
    }

    /**
     * Interaction feedback. On a light surface there is no room to brighten, so states read as a
     * wash of accent; on a strong fill there is no room to tint, so they lift toward white.
     */
    fun Paint.react(amount: Float): Color = closestColor().let {
        if (it.isLight()) Color.interpolate(it, accent, amount) else it.toWhite(amount)
    }

    /** A solid, edge-to-edge fill - primary and status actions both use this shape. */
    fun Semantic.solid(theme: Theme, fill: Color): ThemeAndBack = theme.alter(
        cascading = false,
        outlineWidth = 0.dp,
        elevation = 0.dp,
    ).withBack(background = fill, foreground = fill.maximallyContrastingForeground, outline = fill)

    /**
     * Headings. Weight and tracking carry the hierarchy; color does not, because color is spoken
     * for. Leaving foreground alone also means a heading stays legible on a strong fill for free.
     */
    fun headingFont(scale: Double): FontAndStyle = title.copy(
        weight = if (scale >= 1.4) 680 else 620,
        size = scale.rem,
        // Display sizes need tighter tracking and leading to hold together as a shape. Text-size
        // headings need neither, and tightening them only costs legibility.
        additionalLetterSpacing = if (scale >= 1.4) (-0.018 * scale).rem else 0.px,
        lineSpacingMultiplier = if (scale >= 1.4) 1.15 else 1.3,
    )

    return Theme(
        id = id,
        font = body.copy(lineSpacingMultiplier = 1.5),
        elevation = 0.dp,
        // Radius tracks the spacing an element was given, so dense controls come out crisp and
        // roomy surfaces come out soft without anyone choosing a radius per component.
        cornerRadii = CornerRadii.AdaptiveToSpacing(0.75.rem),
        cornerShape = CornerShape.Continuous,
        gap = 0.75.rem,
        padding = Edges(0.875.rem),
        foreground = ink,
        background = desk,
        outline = fieldEdge,
        outlineWidth = 0.dp,
        separatorOverride = rule,
        transitionDuration = 0.15.seconds,
        semanticOverrides = SemanticOverrides(

            // ---- Structure -------------------------------------------------------------------
            // Everything here sets outlineWidth = 0: a surface is never a box with a line around
            // it. The lightness step and the shadow do the work.

            OuterSemantic.override {
                it.alter(
                    cascading = false,
                    cornerRadii = CornerRadii.Fixed(0.dp),
                    outlineWidth = 0.dp,
                    padding = Edges.ZERO,
                    gap = 0.dp,
                ).withBack(background = desk, foreground = ink)
            },
            MainContentSemantic.override {
                it.alter(cascading = false, cornerRadii = CornerRadii.Fixed(0.dp), outlineWidth = 0.dp)
                    .withBack(background = desk, foreground = ink)
            },
            // Chrome is paper lifted off the desk. A saturated app bar is the single thing that
            // most reliably dates a light theme, and it would also break the accent-means-action
            // rule by spending the brand color on a surface you cannot press.
            BarSemantic.override {
                it.alter(
                    cascading = false,
                    cornerRadii = CornerRadii.Fixed(0.dp),
                    outlineWidth = 0.dp,
                    elevation = 2.dp,
                ).withBack(background = paper, foreground = ink)
            },
            NavSemantic.override {
                it.alter(
                    cascading = false,
                    cornerRadii = CornerRadii.Fixed(0.dp),
                    outlineWidth = 0.dp,
                    elevation = 0.dp,
                    padding = Edges.ZERO,
                    gap = 0.dp,
                ).withBack(background = paper, foreground = ink)
            },

            // Elevation is non-cascading so one card casts one shadow, not every child that
            // happens to paint a background. A card that had to recess instead of rise casts none
            // at all - a tray sunk into a sheet does not float above it.
            CardSemantic.override {
                val rising = it.background.closestColor() != paper
                it.alter(
                    cascading = false,
                    outlineWidth = 0.dp,
                    elevation = if (rising) 1.dp else 0.dp,
                ).withBack(background = it.background.raise(), foreground = ink)
            },
            InsetSemantic.override {
                it.alter(cascading = false, outlineWidth = 0.dp, elevation = 0.dp)
                    .withBack(background = it.background.recess())
            },
            EmbeddedSemantic.override {
                it.alter(cascading = false, outlineWidth = 0.dp, elevation = 0.dp)
                    .withBack(background = it.background.recess())
            },
            ListSemantic.override { it.withoutBack(cascading = false, gap = 0.25.rem) },

            DialogSemantic.override {
                it.alter(
                    cascading = false,
                    outlineWidth = 0.dp,
                    elevation = 6.dp,
                    cornerRadii = CornerRadii.Fixed(1.rem),
                    padding = Edges(1.5.rem),
                ).withBack(background = paper, foreground = ink)
            },
            PopoverSemantic.override {
                it.alter(cascading = false, outlineWidth = 0.dp, elevation = 4.dp)
                    .withBack(background = paper, foreground = ink)
            },
            // Ink at 45% rather than black: the scrim should read as the page dimming, not as a
            // separate gray sheet dropped over it.
            DismissSemantic.override {
                it.withBack(
                    cascading = false,
                    gap = 0.dp,
                    cornerRadii = CornerRadii.Fixed(0.dp),
                    background = ink.applyAlpha(0.45f),
                )
            },

            // ---- Typography ------------------------------------------------------------------

            HeaderSemantic.override { it.withoutBack(font = headingFont(1.0)) },
            override<HeaderSizeSemantic> { it.withoutBack(font = headingFont(HeaderSizeSemantic.lookup[level - 1])) },
            // The stock 70% alpha is a fixed discount that lands below AA on light surfaces; this
            // takes as much contrast as the surface can spare and no more.
            SubtextSemantic.override {
                it.withoutBack(
                    font = it.font.copy(size = it.font.size * 0.85f),
                    foreground = Color.mutedButLegible(it.foreground.closestColor(), it.background.closestColor()),
                )
            },
            // A form is read by scanning labels, so labels get their own weight rather than being
            // shrunken body text.
            LabelSemantic.override {
                it.withoutBack(
                    font = it.font.copy(size = 0.8.rem, weight = 600, additionalLetterSpacing = 0.01.rem),
                    foreground = Color.mutedButLegible(it.foreground.closestColor(), it.background.closestColor()),
                )
            },
            // Small caps with open tracking is the one place a rule is worth more than a weight
            // change: a column head has to be distinguishable from the data at a glance, and at
            // this size weight alone does not carry.
            TableHeaderSemantic.override {
                it.withoutBack(
                    font = it.font.copy(
                        size = 0.75.rem,
                        weight = 650,
                        allCaps = true,
                        additionalLetterSpacing = 0.045.rem,
                    ),
                    foreground = Color.mutedButLegible(it.foreground.closestColor(), it.background.closestColor()),
                )
            },

            BlockquoteSemantic.override {
                it.alter(cascading = false, outlineWidth = 0.dp, elevation = 0.dp)
                    .withBack(
                        background = it.background.recess(0.04f),
                        padding = Edges(left = it.gap * 2, top = it.gap, right = it.gap, bottom = it.gap),
                    )
            },
            CodeBlockSemantic.override {
                it.alter(
                    cascading = false,
                    outlineWidth = 0.dp,
                    elevation = 0.dp,
                    cornerRadii = CornerRadii.Fixed(0.4.rem),
                ).withBack(
                    font = it.font.copy(font = systemDefaultFixedWidthFont, size = it.font.size * 0.9f),
                    background = it.background.recess(),
                )
            },

            // ---- Emphasis and status ---------------------------------------------------------

            ImportantSemantic.override {
                if (it.background.isLight()) {
                    solid(it, accent)
                } else {
                    // Already on a strong fill - a bar, or `important` applied twice. Stacking
                    // another dark fill would erase the emphasis, so invert into a light chip.
                    solid(it, paper)
                }
            },
            CriticalSemantic.override { solid(it, accent.toBlack(0.35f)) },
            WarningSemantic.override { solid(it, warning) },
            DangerSemantic.override { solid(it, danger) },
            AffirmativeSemantic.override { solid(it, affirmative) },

            ErrorSemantic.override {
                it.withoutBack(foreground = Color.legibleOn(danger, it.background.closestColor()))
            },

            // ---- Interaction -----------------------------------------------------------------
            // The only semantics in the theme that draw an outline. Each one marks something the
            // person can act on, which is what an outline is reserved to mean here.

            FieldSemantic.override {
                it.alter(
                    cascading = false,
                    outlineWidth = 1.dp,
                    outline = fieldEdge,
                    elevation = 0.dp,
                    cornerRadii = CornerRadii.Fixed(0.5.rem),
                ).withBack(background = paper, foreground = ink)
            },
            InvalidSemantic.override {
                it.withBack(cascading = false, outlineWidth = 2.dp, outline = danger)
            },
            FocusSemantic.override {
                it.withBack(
                    cascading = false,
                    outlineWidth = 2.dp,
                    outline = if (it.background.isLight()) accent else paper,
                )
            },
            SelectedSemantic.override {
                val surface = it.background.react(0.14f)
                it.alter(
                    cascading = false,
                    outlineWidth = 1.dp,
                    outline = if (it.background.isLight()) accent.applyAlpha(0.45f) else paper.applyAlpha(0.5f),
                ).withBack(
                    background = surface,
                    // Selection on a light surface is a wash of accent, so accent text names it.
                    // On a strong fill the wash *is* the accent already; accent-on-accent would
                    // have to darken to near-black to stay legible, so take white or black instead.
                    foreground = if (it.background.isLight()) Color.legibleOn(accent, surface)
                    else surface.maximallyContrastingForeground,
                    iconOverride = null,
                )
            },
            UnselectedSemantic.override {
                it.alter(
                    foreground = Color.mutedButLegible(it.foreground.closestColor(), it.background.closestColor()),
                    iconOverride = null,
                ).withoutBackButPadding
            },
            // Switches, sliders and other controls that tint themselves; the stock behavior falls
            // back to body text color off Apple platforms, which reads as an unstyled control.
            InteractiveSemantic.override {
                it.withoutBack(
                    foreground = if (it.background.isLight()) accent else paper,
                    iconOverride = null,
                )
            },

            HoverSemantic.override { it.withBack(background = it.background.react(0.07f)) },
            DownSemantic.override { it.withBack(background = it.background.react(0.15f)) },
            // `withoutBack` rather than `withBack`: the muted background still reaches anything
            // that was already painting one (a disabled primary button), while disabled *text*
            // stays text instead of gaining a gray box it never had.
            DisabledSemantic.override {
                val muted = Color.interpolate(it.background.closestColor(), desk, 0.6f)
                it.withoutBack(
                    elevation = 0.dp,
                    background = muted,
                    foreground = Color.interpolate(it.foreground.closestColor(), muted, 0.55f),
                    iconOverride = null,
                    outline = Color.interpolate(it.outline.closestColor(), muted, 0.6f),
                )
            },
        ),
    )
}
