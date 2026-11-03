package com.lightningkite.kiteui.models

import com.lightningkite.kiteui.views.l2.LabelSemantic
import kotlin.time.Duration.Companion.seconds

/**
 * The shadcn/ui look: a near-neutral palette, one accent spent only on primary actions, hairline
 * borders instead of shadows, and small dense type.
 *
 * Three rules produce most of it:
 *
 * - **Neutrals do the work.** Every surface is a rung on one gray ladder tinted a few percent
 *   toward [accent] - shadcn's "zinc" leans the same way, and a truly neutral gray next to a
 *   colored control reads as dead. Nothing else is colored unless it is an action or a status.
 * - **A border, not a shadow.** Cards, fields, bars and menus are separated by a 1px line in the
 *   same neutral. Elevation appears only where something genuinely floats - a popover, a dialog.
 * - **Small type, tight boxes.** Body text is 0.875rem and controls are about 2.25rem tall, which
 *   is what makes a shadcn screen feel like an application rather than a document.
 *
 * ```kotlin
 * val dark = Theme.shadCnLike("app-dark")
 * val light = Theme.shadCnLike("app-light", background = Color.fromHexString("#FAFAFA"))
 * ```
 *
 * @param id Unique identifier for the theme; must not collide with another theme in the app.
 * @param background The canvas the app sits on, and the rung every other neutral is measured from.
 *   Its brightness decides whether the theme runs light or dark. A light canvas should be
 *   *slightly* off-white (`#FAFAFA`) rather than pure white - cards are made of white, and a white
 *   page leaves them nothing to rise to.
 * @param accent The one color the theme spends: primary actions, selection, focus rings, and the
 *   trace of hue in the neutrals. The default is shadcn's own choice - near-black on a light
 *   canvas, near-white on a dark one - which is what makes stock shadcn read as monochrome. Pass a
 *   brand color to get the same theme with a colored primary.
 * @param title Font used for headings.
 * @param body Font used for everything else. Its size is replaced with shadcn's 0.875rem.
 */
public fun Theme.Companion.shadCnLike(
    id: String = "shadCnLike",
    background: Color = Color.fromHexString("#09090B"),
    accent: Color = if (background.perceivedBrightness > 0.5f) Color.fromHexString("#18181B")
    else Color.fromHexString("#FAFAFA"),
    title: FontAndStyle = FontAndStyle(),
    body: FontAndStyle = FontAndStyle(),
): Theme {
    val dark = background.perceivedBrightness < 0.5f

    // The neutrals borrow a twelfth of the accent's own chroma, so shadcn's monochrome default
    // yields true grays - its "zinc" ramp is only two or three points off neutral - while a brand
    // accent warms the page enough to notice on a full screen and not enough to read as a color.
    // The floor keeps dark surfaces from going flat gray when the accent has no chroma at all.
    val neutralSaturation = (accent.toHSP().saturation * 0.12f).coerceIn(0.012f, 0.03f)
    val neutralHue = accent.toHSP().hue

    /**
     * A neutral at a given perceived brightness. This is the only source of gray in the theme, so a
     * caller who passes a strongly colored [background] still gets neutral chrome rather than a
     * tinted-everything page.
     */
    fun neutral(brightness: Float): Color =
        HSPColor(hue = neutralHue, saturation = neutralSaturation, brightness = brightness.coerceIn(0f, 1f)).toRGB()

    /** A rung [amount] away from the canvas, always toward the middle of the range. */
    fun rung(amount: Float): Color =
        neutral(background.perceivedBrightness + if (dark) amount else -amount)

    // The ladder. Light and dark need different step sizes because equal-looking steps are larger
    // at the dark end of the range.
    val canvas = background
    // A card is made of paper. In dark mode that is a rung up from the canvas; in light mode it is
    // white, because a light page has nowhere brighter to go.
    val paper = if (dark) rung(0.06f) else Color.white
    val well = rung(if (dark) 0.145f else 0.045f)
    val panel = rung(if (dark) 0.06f else 0.025f)
    val line = rung(if (dark) 0.135f else 0.095f)
    val ink = neutral(if (dark) 0.96f else 0.09f)

    // Tailwind's own red/amber/emerald, one step apart between modes so each holds its weight
    // against the canvas behind it. shadcn draws from the same palette.
    val danger = if (dark) Color.fromHexString("#EF4444") else Color.fromHexString("#DC2626")
    val warning = if (dark) Color.fromHexString("#F59E0B") else Color.fromHexString("#D97706")
    val affirmative = if (dark) Color.fromHexString("#10B981") else Color.fromHexString("#059669")

    /**
     * True for the theme's own neutral chrome, as opposed to a strong colored fill.
     *
     * Brightness alone does not answer this. A deep blue accent on a dark canvas is darker than
     * half, but it is emphatically not chrome - reading it as chrome makes the primary button wash
     * toward the color it already is, so hover does nothing and `critical` lands back on
     * `important`. Chroma is what actually separates the two.
     */
    fun Paint.onCanvasSide(): Boolean = closestColor().let {
        val chroma = maxOf(it.red, it.green, it.blue) - minOf(it.red, it.green, it.blue)
        chroma < 0.12f && (it.perceivedBrightness > 0.5f) != dark
    }

    /**
     * Interaction feedback. A surface near the canvas washes toward the accent; a strong fill has
     * nowhere to go that way - it *is* the accent - so it fades back toward the canvas instead,
     * which is what shadcn's `hover:bg-primary/90` amounts to.
     */
    fun Paint.react(amount: Float): Color = closestColor().let {
        if (onCanvasSide()) Color.interpolate(it, accent, amount) else Color.interpolate(it, canvas, amount)
    }

    /**
     * The next sheet up. Paper is the top of the ladder, so a card stacked on paper sinks into a
     * well instead - a tray inside a card, which is the only reading that stays physical.
     */
    fun Paint.raise(): Color = if (closestColor() == paper) well else paper

    /** A surface one step down - code blocks, embedded content, inset trays. */
    fun Paint.recess(amount: Float = 0.05f): Color =
        closestColor().let { if (dark) it.toWhite(amount * 1.6f) else it.toBlack(amount) }

    /**
     * A solid, edge-to-edge fill. The primary action and every status action share this shape.
     *
     * A fill close to the canvas would vanish into it, so it takes the hairline border of shadcn's
     * outline variant instead. A strong fill needs no edge.
     */
    fun Semantic.solid(theme: Theme, fill: Color): ThemeAndBack = theme.alter(
        cascading = false,
        outlineWidth = if (fill.onCanvasSide()) 1.dp else 0.dp,
        elevation = 0.dp,
    ).withBack(
        background = fill,
        foreground = if (fill.onCanvasSide()) ink else fill.maximallyContrastingForeground,
        outline = if (fill.onCanvasSide()) line else fill,
    )

    /**
     * Headings. shadcn's display type is semibold and tightly tracked; size and weight carry the
     * hierarchy, never color, because the accent is spoken for.
     */
    fun headingFont(scale: Double): FontAndStyle = title.copy(
        weight = 600,
        size = scale.rem,
        additionalLetterSpacing = if (scale >= 1.4) (-0.022 * scale).rem else 0.px,
        lineSpacingMultiplier = if (scale >= 1.4) 1.2 else 1.35,
    )

    /** Controls are rounded to a fixed radius rather than to their container's spacing: a nav list
     *  packs its items with no gap at all, which would otherwise square them off completely. */
    val controlRadius = CornerRadii.Fixed(0.4.rem)

    return Theme(
        id = id,
        font = body.copy(size = 0.875.rem, lineSpacingMultiplier = 1.45),
        elevation = 0.dp,
        cornerRadii = CornerRadii.AdaptiveToSpacing(0.75.rem),
        cornerShape = CornerShape.Circular,
        gap = 0.5.rem,
        // Roughly shadcn's `h-9 px-4 py-2`: wider than it is tall, which is what keeps a row of
        // buttons reading as controls rather than as blocks.
        padding = Edges(horizontal = 0.85.rem, vertical = 0.55.rem),
        foreground = ink,
        background = canvas,
        outline = line,
        outlineWidth = 0.dp,
        separatorOverride = line,
        transitionDuration = 0.15.seconds,
        semanticOverrides = SemanticOverrides(

            // ---- Structure -------------------------------------------------------------------

            OuterSemantic.override {
                it.alter(
                    cascading = false,
                    cornerRadii = CornerRadii.Fixed(0.dp),
                    outlineWidth = 0.dp,
                    padding = Edges.ZERO,
                    gap = 0.dp,
                ).withBack(background = canvas, foreground = ink)
            },
            MainContentSemantic.override {
                it.alter(cascading = false, cornerRadii = CornerRadii.Fixed(0.dp), outlineWidth = 0.dp)
                    .withBack(background = canvas, foreground = ink)
            },
            // The app bar is the canvas with a line under it. A saturated bar is the single thing
            // that most reliably breaks the shadcn read, and it would spend the accent on a
            // surface you cannot press.
            BarSemantic.override {
                it.alter(
                    cascading = false,
                    cornerRadii = CornerRadii.Fixed(0.dp),
                    outlineWidth = 1.dp,
                    outline = line,
                    elevation = 0.dp,
                    padding = Edges(horizontal = 0.75.rem, vertical = 0.5.rem),
                    gap = 0.4.rem,
                ).withBack(background = canvas, foreground = ink)
            },
            // The sidebar steps one rung off the canvas, and carries padding so its items sit as
            // inset rounded rows rather than reaching the edges.
            NavSemantic.override {
                it.alter(
                    cascading = false,
                    cornerRadii = CornerRadii.Fixed(0.dp),
                    outlineWidth = 1.dp,
                    outline = line,
                    elevation = 0.dp,
                    padding = Edges(0.4.rem),
                    gap = 0.15.rem,
                ).withBack(background = panel, foreground = ink)
            },

            CardSemantic.override {
                it.alter(
                    cascading = false,
                    outlineWidth = 1.dp,
                    outline = line,
                    elevation = 0.dp,
                    padding = Edges(1.15.rem),
                    gap = 0.7.rem,
                ).withBack(background = it.background.raise(), foreground = ink)
            },
            GroupSemantic.override { it[CardSemantic] },
            InsetSemantic.override {
                it.alter(cascading = false, outlineWidth = 0.dp, elevation = 0.dp)
                    .withBack(background = it.background.recess())
            },
            EmbeddedSemantic.override {
                it.alter(cascading = false, outlineWidth = 0.dp, elevation = 0.dp)
                    .withBack(background = it.background.recess())
            },
            ListSemantic.override { it.withoutBack(cascading = false, gap = 0.25.rem) },

            // The two things that genuinely float, and the only two that cast a shadow.
            DialogSemantic.override {
                it.alter(
                    cascading = false,
                    outlineWidth = 1.dp,
                    outline = line,
                    elevation = 8.dp,
                    cornerRadii = CornerRadii.Fixed(0.75.rem),
                    padding = Edges(1.5.rem),
                    gap = 0.75.rem,
                ).withBack(background = paper, foreground = ink)
            },
            PopoverSemantic.override {
                it.alter(
                    cascading = false,
                    outlineWidth = 1.dp,
                    outline = line,
                    elevation = 4.dp,
                    cornerRadii = CornerRadii.Fixed(0.5.rem),
                    padding = Edges(0.3.rem),
                    gap = 0.1.rem,
                ).withBack(background = paper, foreground = ink)
            },
            // Ink rather than black: the scrim should read as the page dimming, not as a separate
            // gray sheet dropped over it.
            DismissSemantic.override {
                it.withBack(
                    cascading = false,
                    gap = 0.dp,
                    padding = Edges.ZERO,
                    cornerRadii = CornerRadii.Fixed(0.dp),
                    background = ink.applyAlpha(0.5f),
                )
            },

            // ---- Typography ------------------------------------------------------------------

            HeaderSemantic.override { it.withoutBack(font = headingFont(1.0)) },
            override<HeaderSizeSemantic> { it.withoutBack(font = headingFont(HeaderSizeSemantic.lookup[level - 1])) },
            // The stock 70% alpha is a fixed discount that lands below AA on light surfaces; this
            // takes as much contrast as the surface can spare and no more.
            SubtextSemantic.override {
                it.withoutBack(
                    font = it.font.copy(size = it.font.size * 0.9f),
                    foreground = Color.mutedButLegible(it.foreground.closestColor(), it.background.closestColor()),
                )
            },
            // shadcn's `<Label>` is body-sized and medium, not shrunken gray text - a form is read
            // by scanning labels, and they have to hold their own against the fields.
            LabelSemantic.override { it.withoutBack(font = it.font.copy(weight = 500)) },
            TableHeaderSemantic.override {
                it.withoutBack(
                    font = it.font.copy(weight = 500),
                    foreground = Color.mutedButLegible(it.foreground.closestColor(), it.background.closestColor()),
                )
            },

            // A rule and an indent, the way shadcn quotes - no panel, no fill.
            BlockquoteSemantic.override {
                it.withoutBack(
                    font = it.font.copy(italic = true),
                    padding = Edges(left = it.gap * 3, top = it.gap / 2, right = it.gap, bottom = it.gap / 2),
                    foreground = Color.mutedButLegible(it.foreground.closestColor(), it.background.closestColor()),
                )
            },
            CodeBlockSemantic.override {
                it.alter(
                    cascading = false,
                    outlineWidth = 1.dp,
                    outline = line,
                    elevation = 0.dp,
                    cornerRadii = CornerRadii.Fixed(0.5.rem),
                ).withBack(
                    font = it.font.copy(font = systemDefaultFixedWidthFont, size = it.font.size * 0.95f),
                    background = it.background.recess(),
                )
            },

            // ---- Emphasis and status ---------------------------------------------------------

            // Already on a strong fill - a status chip, or `important` applied twice. Stacking
            // another dark fill would erase the emphasis, so it inverts into a paper chip.
            //
            // `critical` is `important` applied twice, so it needs no override of its own: it lands
            // on that inverted chip, which is shadcn's outline variant. That is the right answer for
            // a palette with exactly one primary - inventing a second loud color to sit beside it
            // would be the thing that stops looking like shadcn.
            ImportantSemantic.override {
                if (it.background.onCanvasSide()) solid(it, accent) else solid(it, paper)
            },
            WarningSemantic.override { solid(it, warning) },
            DangerSemantic.override { solid(it, danger) },
            AffirmativeSemantic.override { solid(it, affirmative) },

            ErrorSemantic.override { it.withoutBack(foreground = Color.legibleOn(danger, it.background.closestColor())) },

            // ---- Interaction -----------------------------------------------------------------

            // shadcn's input is paper with a hairline border at every depth, so a form reads the
            // same on the canvas, on a card, and in a dialog.
            FieldSemantic.override {
                it.alter(
                    cascading = false,
                    outlineWidth = 1.dp,
                    outline = line,
                    elevation = 0.dp,
                    cornerRadii = controlRadius,
                ).withBack(background = paper, foreground = ink)
            },
            InvalidSemantic.override { it.withBack(cascading = false, outlineWidth = 2.dp, outline = danger) },
            FocusSemantic.override { it.withBack(cascading = false, outlineWidth = 2.dp, outline = accent) },

            // A nav item, a tab, a segment. Selection is a wash of accent plus the weight change
            // shadcn puts on an active item; it never draws a border, which is what turned the old
            // nav into a stack of seamed boxes.
            SelectedSemantic.override {
                val fill = it.background.react(0.09f)
                it.alter(
                    cascading = false,
                    outlineWidth = 0.dp,
                    elevation = 0.dp,
                    cornerRadii = controlRadius,
                ).withBack(
                    background = fill,
                    foreground = if (it.background.onCanvasSide()) ink else fill.maximallyContrastingForeground,
                    font = it.font.copy(weight = 500),
                    iconOverride = null,
                )
            },
            // Transparent, full-strength text: an inactive destination is still a destination.
            // Only the fill distinguishes it from the selected one.
            UnselectedSemantic.override {
                it.alter(outlineWidth = 0.dp, cornerRadii = controlRadius, iconOverride = null).withoutBackButPadding
            },

            HoverSemantic.override { it.withBack(background = it.background.react(0.05f)) },
            DownSemantic.override { it.withBack(background = it.background.react(0.12f)) },
            // shadcn disables with a flat `opacity-50`. `withoutBack` rather than `withBack` so
            // disabled *text* stays text instead of gaining a gray box it never had.
            DisabledSemantic.override {
                it.withoutBack(
                    elevation = 0.dp,
                    foreground = it.foreground.applyAlpha(0.5f),
                    background = it.background.applyAlpha(0.5f),
                    outline = it.outline.applyAlpha(0.5f),
                    iconOverride = null,
                )
            },
            // Switches and sliders tint themselves; the stock behavior falls back to body text
            // color off Apple platforms, which reads as an unstyled control.
            InteractiveSemantic.override {
                it.withoutBack(
                    foreground = if (it.background.onCanvasSide()) accent else paper,
                    iconOverride = null,
                )
            },
        ),
    )
}
