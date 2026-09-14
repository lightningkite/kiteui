package com.lightningkite.kiteui.models

import com.lightningkite.kiteui.views.l2.LabelSemantic
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

/**
 * Material Design 2: a primary-colored app bar, shadows that mean height, and buttons that shout.
 *
 * The parts that make a screen read as Material 2 rather than as generic flat design:
 *
 * - **Elevation is the layout.** Every surface sits at a height in dp, and height is what separates
 *   it from what is behind. In light mode that is a shadow. In dark mode a shadow on near-black is
 *   invisible, so Material 2 substitutes a white overlay whose opacity rises with height - 5% at
 *   1dp through 16% at 24dp - and this theme implements that table.
 * - **The bar carries the brand.** A primary-colored app bar is the single most recognizable
 *   Material 2 element. Dark mode drops it, exactly as the spec does: a saturated bar against a
 *   near-black UI is where Material 2 itself stopped.
 * - **Actions are set in caps.** 14px, medium, and tracked out. It is the trait people recognize
 *   first, so a Material 2 theme that leaves it out does not read as one. Destinations opt back
 *   out - a nav item is a place, not an action.
 *
 * ```kotlin
 * val light = Theme.material()
 * val dark = Theme.material("app-dark", background = Color.fromHexString("#121212"))
 * ```
 *
 * @param id Unique identifier for the theme; must not collide with another theme in the app.
 * @param background The canvas the app sits on. Its brightness decides whether the theme runs light
 *   or dark. Cards are made of `surface` and rise off this, so a light canvas should be grey rather
 *   than white; the default is Material's grey 50.
 * @param accent The primary color - the app bar, contained buttons, selection and focus. A primary
 *   too dark to sit on a dark canvas is lightened toward its 200 tone automatically, which is what
 *   the spec asks for and why Material's own `#6200EE` becomes a lilac in dark mode.
 * @param secondary The secondary color, used for [CriticalSemantic] and for emphasis stacked on top
 *   of the primary. Given the same rescue treatment on a dark canvas.
 * @param title Font used for headings.
 * @param body Font used for everything else.
 */
public fun Theme.Companion.material(
    id: String = "material",
    background: Color = Color.fromHexString("#FAFAFA"),
    accent: Color = Color.fromHexString("#6200EE"),
    secondary: Color = Color.fromHexString("#03DAC6"),
    title: FontAndStyle = FontAndStyle(),
    body: FontAndStyle = FontAndStyle(),
): Theme {
    val dark = background.perceivedBrightness < 0.5f

    /**
     * Material 2's dark-theme elevation overlay. A shadow cast onto near-black cannot be seen, so
     * the spec raises the surface's own lightness instead, on this exact table.
     */
    fun overlayFor(elevation: Float): Float = when {
        elevation < 1f -> 0.00f
        elevation < 2f -> 0.05f
        elevation < 3f -> 0.07f
        elevation < 4f -> 0.08f
        elevation < 6f -> 0.09f
        elevation < 8f -> 0.11f
        elevation < 12f -> 0.12f
        elevation < 16f -> 0.14f
        elevation < 24f -> 0.15f
        else -> 0.16f
    }

    /** The canvas lifted to a height. In light mode height is a shadow, so the color does not move. */
    fun liftedTo(elevation: Float): Color =
        if (dark) background.toWhite(overlayFor(elevation)) else Color.white

    /**
     * A color bright enough to carry a dark surface. Material 2 asks for the 200 tone of the
     * primary in dark mode: a saturated 500 tone vibrates against near-black and is too dark to
     * read as text drawn in it. Colors that already sit high enough are left alone.
     */
    fun Color.forCanvas(): Color = toHSP().let {
        if (!dark || it.brightness >= 0.62f) this
        else HSPColor(hue = it.hue, saturation = it.saturation * 0.5f, brightness = 0.66f).toRGB()
    }

    val canvas = background
    val surface = liftedTo(1f)
    // Where a second surface has to stack on the first. Light mode has nowhere brighter to go, so
    // it sinks instead; dark mode simply keeps climbing the overlay table.
    val well = if (dark) liftedTo(8f) else Color.interpolate(Color.white, Color.black, 0.06f)

    val primary = accent.forCanvas()
    val onSecondary = secondary.forCanvas()
    val ink = if (dark) Color.white else Color.black
    // Material 2 states its text emphases as alphas over the surface. Resolving them to solid
    // colors keeps contrast predictable when they land on a fill rather than on the canvas.
    val error = (if (dark) Color.fromHexString("#CF6679") else Color.fromHexString("#B00020")).forCanvas()
    val warning = (if (dark) Color.fromHexString("#FFB300") else Color.fromHexString("#FF6F00")).forCanvas()
    val affirmative = (if (dark) Color.fromHexString("#00C853") else Color.fromHexString("#2E7D32")).forCanvas()

    /**
     * Material 2's medium-emphasis text: 60% of the ink, resolved to a solid color against what it
     * sits on so contrast stays predictable when it lands on a fill rather than on the canvas.
     *
     * The spec's 60% assumes a neutral surface. On a saturated container - the primary app bar,
     * where the inactive destinations live - it lands around 2.9:1, so it is clamped to the most
     * muting the surface can actually afford. An unreadable nav label is not worth the fidelity.
     */
    fun mediumEmphasis(foreground: Paint, background: Paint): Color {
        val fg = foreground.closestColor()
        val bg = background.closestColor()
        val perSpec = Color.interpolate(fg, bg, 0.4f)
        return if (perSpec contrastAgainst bg >= Color.WCAG_AA_NORMAL_TEXT_CONTRAST_RATIO) perSpec
        else Color.mutedButLegible(fg, bg)
    }

    /**
     * A state layer: the overlay Material 2 paints over a container to show interaction. The
     * overlay color is the container's own text color, which is what makes one set of opacities
     * work on a white card and on a purple button alike.
     *
     * [on] is passed rather than made a receiver on purpose. As an extension on `Theme` the bare
     * name `background` inside this body binds to the *factory's* `background` parameter, not to
     * the theme's, and every state layer silently reads off the canvas instead of the surface it
     * is drawn on.
     */
    fun stateLayer(on: Theme, opacity: Float, tint: Color? = null): Color = Color.interpolate(
        on.background.closestColor(),
        tint ?: on.foreground.closestColor(),
        opacity,
    )

    /** A filled container - contained buttons and status chips share this shape. */
    fun Semantic.contained(theme: Theme, fill: Color): ThemeAndBack = theme.alter(
        cascading = false,
        outlineWidth = 0.dp,
        cornerRadii = CornerRadii.Fixed(4.dp),
        elevation = 2.dp,
    ).withBack(
        background = fill,
        foreground = fill.maximallyContrastingForeground,
        outline = fill,
    )

    /**
     * The Material 2 type scale, compressed to the range an application actually uses. The spec's
     * headline 1 is 96px, which is display copy for a marketing page, not a screen title; the
     * scale here starts at headline 4 and keeps the spec's weights and tracking from there.
     */
    fun headingFont(level: Int): FontAndStyle = when (level) {
        1 -> title.copy(size = 2.125.rem, weight = 400, additionalLetterSpacing = 0.016.rem, lineSpacingMultiplier = 1.2)
        2 -> title.copy(size = 1.5.rem, weight = 400, lineSpacingMultiplier = 1.25)
        3 -> title.copy(size = 1.25.rem, weight = 500, additionalLetterSpacing = 0.009.rem, lineSpacingMultiplier = 1.3)
        4 -> title.copy(size = 1.125.rem, weight = 500, additionalLetterSpacing = 0.009.rem, lineSpacingMultiplier = 1.35)
        5 -> title.copy(size = 1.rem, weight = 500, additionalLetterSpacing = 0.009.rem, lineSpacingMultiplier = 1.4)
        else -> title.copy(size = 0.875.rem, weight = 500, additionalLetterSpacing = 0.006.rem, lineSpacingMultiplier = 1.4)
    }

    /** The spec's "button" style: 14px, medium, all caps, tracked out by 1.25px. */
    fun FontAndStyle.asAction(): FontAndStyle =
        copy(size = 0.875.rem, weight = 500, allCaps = true, additionalLetterSpacing = 0.078.rem)

    return Theme(
        id = id,
        font = body.copy(size = 1.rem, lineSpacingMultiplier = 1.5),
        elevation = 0.dp,
        cornerRadii = CornerRadii.Fixed(4.dp),
        cornerShape = CornerShape.Circular,
        // Material 2 lays out on an 8dp grid with a 16dp screen margin. A contained button is 36dp
        // tall with 16dp of horizontal padding, which is where the asymmetry comes from.
        gap = 1.rem,
        padding = Edges(horizontal = 1.rem, vertical = 0.5.rem),
        foreground = ink,
        background = canvas,
        outline = Color.interpolate(ink, canvas, 0.88f),
        outlineWidth = 0.dp,
        separatorOverride = Color.interpolate(ink, canvas, 0.88f),
        transitionDuration = 0.2.seconds,
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
                ).withBack(background = canvas, foreground = ink)
            },
            MainContentSemantic.override {
                it.alter(cascading = false, cornerRadii = CornerRadii.Fixed(0.dp), outlineWidth = 0.dp, elevation = 0.dp)
                    .withBack(background = canvas, foreground = ink)
            },
            // The signature element in light mode, and deliberately not in dark: the spec drops the
            // primary bar there because a saturated field that size overwhelms a near-black UI.
            BarSemantic.override {
                val fill = if (dark) liftedTo(4f) else primary
                it.alter(
                    cascading = false,
                    cornerRadii = CornerRadii.Fixed(0.dp),
                    outlineWidth = 0.dp,
                    elevation = 4.dp,
                    padding = Edges(horizontal = 1.rem, vertical = 0.5.rem),
                ).withBack(background = fill, foreground = fill.maximallyContrastingForeground)
            },
            // A navigation drawer is a surface at 16dp, never the brand color - Material 2 puts the
            // brand in the bar above it and leaves the destinations neutral.
            NavSemantic.override {
                it.alter(
                    cascading = false,
                    cornerRadii = CornerRadii.Fixed(0.dp),
                    outlineWidth = 0.dp,
                    elevation = 0.dp,
                    padding = Edges(0.5.rem),
                    gap = 0.dp,
                ).withBack(background = liftedTo(16f), foreground = ink)
            },

            CardSemantic.override {
                it.alter(
                    cascading = false,
                    outlineWidth = 0.dp,
                    elevation = 1.dp,
                    padding = Edges(1.rem),
                ).withBack(
                    background = if (it.background.closestColor() == surface) well else surface,
                    foreground = ink,
                )
            },
            GroupSemantic.override { it[CardSemantic] },
            InsetSemantic.override {
                it.alter(cascading = false, outlineWidth = 0.dp, elevation = 0.dp)
                    .withBack(background = stateLayer(it, 0.06f))
            },
            EmbeddedSemantic.override {
                it.alter(cascading = false, outlineWidth = 0.dp, elevation = 0.dp)
                    .withBack(background = stateLayer(it, 0.06f))
            },
            ListSemantic.override { it.withoutBack(cascading = false, gap = 0.dp) },

            DialogSemantic.override {
                it.alter(
                    cascading = false,
                    outlineWidth = 0.dp,
                    elevation = 24.dp,
                    cornerRadii = CornerRadii.Fixed(4.dp),
                    padding = Edges(1.5.rem),
                ).withBack(background = liftedTo(24f), foreground = ink)
            },
            PopoverSemantic.override {
                it.alter(
                    cascading = false,
                    outlineWidth = 0.dp,
                    elevation = 8.dp,
                    cornerRadii = CornerRadii.Fixed(4.dp),
                    padding = Edges(vertical = 0.5.rem, horizontal = 0.dp),
                    gap = 0.dp,
                ).withBack(background = liftedTo(8f), foreground = ink)
            },
            DismissSemantic.override {
                it.withBack(
                    cascading = false,
                    gap = 0.dp,
                    padding = Edges.ZERO,
                    cornerRadii = CornerRadii.Fixed(0.dp),
                    // The spec's scrim is 32% black in both polarities.
                    background = Color.black.applyAlpha(0.32f),
                )
            },

            // ---- Typography ------------------------------------------------------------------

            HeaderSemantic.override { it.withoutBack(font = headingFont(5)) },
            override<HeaderSizeSemantic> { it.withoutBack(font = headingFont(level)) },
            SubtextSemantic.override {
                it.withoutBack(
                    font = it.font.copy(size = 0.75.rem, additionalLetterSpacing = 0.025.rem),
                    foreground = mediumEmphasis(it.foreground, it.background),
                )
            },
            LabelSemantic.override {
                it.withoutBack(
                    font = it.font.copy(size = 0.875.rem, weight = 500, additionalLetterSpacing = 0.006.rem),
                    foreground = mediumEmphasis(it.foreground, it.background),
                )
            },
            // The spec's "overline" style, which is what a column head is.
            TableHeaderSemantic.override {
                it.withoutBack(
                    font = it.font.copy(
                        size = 0.75.rem,
                        weight = 500,
                        allCaps = true,
                        additionalLetterSpacing = 0.094.rem,
                    ),
                    foreground = mediumEmphasis(it.foreground, it.background),
                )
            },
            BlockquoteSemantic.override {
                it.withoutBack(
                    font = it.font.copy(italic = true),
                    padding = Edges(left = it.gap, top = it.gap / 2, right = it.gap, bottom = it.gap / 2),
                    foreground = mediumEmphasis(it.foreground, it.background),
                )
            },
            CodeBlockSemantic.override {
                it.alter(cascading = false, outlineWidth = 0.dp, elevation = 0.dp, cornerRadii = CornerRadii.Fixed(4.dp))
                    .withBack(
                        font = it.font.copy(font = systemDefaultFixedWidthFont, size = it.font.size * 0.9f),
                        background = stateLayer(it, 0.08f),
                    )
            },

            // ---- Emphasis and status ---------------------------------------------------------

            // Stacking emphasis moves to the secondary color rather than piling another primary
            // fill on a primary fill, which is also what makes `critical` land on the secondary.
            ImportantSemantic.override {
                contained(it, if (it.background.closestColor() == primary) onSecondary else primary)
            },
            WarningSemantic.override { contained(it, warning) },
            DangerSemantic.override { contained(it, error) },
            AffirmativeSemantic.override { contained(it, affirmative) },
            ErrorSemantic.override { it.withoutBack(foreground = Color.legibleOn(error, it.background.closestColor())) },

            // ---- Interaction -----------------------------------------------------------------

            // Every tappable thing takes the spec's caps-and-tracking action style. Nav
            // destinations undo it below - a place is not an action.
            ClickableSemantic.override { it.alter(font = it.font.asAction()).withoutBackButPadding },

            // The filled text field: a tinted container with only its top corners rounded, which is
            // the silhouette the underline variant leaves behind.
            FieldSemantic.override {
                it.alter(
                    cascading = false,
                    outlineWidth = 0.dp,
                    elevation = 0.dp,
                    cornerRadii = CornerRadii.PerCorner(4.dp, topLeft = true, topRight = true),
                    padding = Edges(horizontal = 0.75.rem, vertical = 0.75.rem),
                ).withBack(background = stateLayer(it, if (dark) 0.09f else 0.05f), foreground = ink)
            },
            InvalidSemantic.override { it.withBack(cascading = false, outlineWidth = 2.dp, outline = error) },
            FocusSemantic.override { it.withBack(cascading = false, outlineWidth = 2.dp, outline = primary) },

            // A drawer's active destination: the primary at 12%, with the primary carried into the
            // text and icon. Caps are dropped again here.
            SelectedSemantic.override {
                // Marking the primary with the primary is a no-op, which is what a destination
                // sitting in the app bar would otherwise get. Deepening is the move there rather
                // than the usual overlay: the bar's text is white, so lightening the fill walks the
                // container toward its own label and takes the contrast with it.
                val onPrimary = it.background.closestColor() == primary
                it.alter(
                    cascading = false,
                    outlineWidth = 0.dp,
                    elevation = 0.dp,
                    cornerRadii = CornerRadii.Fixed(4.dp),
                ).withBack(
                    background = if (onPrimary) Color.interpolate(primary, Color.black, 0.22f)
                    else stateLayer(it, 0.12f, tint = primary),
                    // Primary text on a 12% primary wash is a hair under AA for mid-tone brand
                    // colors, so it takes the least deepening that clears it.
                    foreground = if (onPrimary) it.foreground
                    else Color.legibleOn(primary, stateLayer(it, 0.12f, tint = primary)),
                    font = it.font.copy(allCaps = false, weight = 500, additionalLetterSpacing = 0.dp),
                    iconOverride = null,
                )
            },
            UnselectedSemantic.override {
                it.alter(
                    outlineWidth = 0.dp,
                    cornerRadii = CornerRadii.Fixed(4.dp),
                    foreground = mediumEmphasis(it.foreground, it.background),
                    font = it.font.copy(allCaps = false, weight = 500, additionalLetterSpacing = 0.dp),
                    iconOverride = null,
                ).withoutBackButPadding
            },

            HoverSemantic.override { it.withBack(background = stateLayer(it, 0.04f)) },
            DownSemantic.override { it.withBack(background = stateLayer(it, 0.10f)) },
            // The spec's disabled pair: text drops to 38%, containers to 12%.
            DisabledSemantic.override {
                it.withoutBack(
                    elevation = 0.dp,
                    foreground = it.foreground.applyAlpha(0.38f),
                    background = it.background.applyAlpha(0.12f),
                    outline = it.outline.applyAlpha(0.12f),
                    iconOverride = null,
                )
            },
            InteractiveSemantic.override {
                it.withoutBack(
                    foreground = if (it.background.closestColor() == primary) ink else primary,
                    iconOverride = null,
                )
            },
        ),
    )
}

@Deprecated(
    "Renamed and restyled. Theme.material now takes the standard (id, background, accent, secondary, " +
        "title, body) parameters; reach for Theme.copy for the rest.",
    ReplaceWith("Theme.material(id = id, accent = primary, secondary = secondary)")
)
public object MaterialLikeTheme {
    public operator fun invoke(
        id: String,
        primary: Color = Color.fromHex(0xFF6200EE.toInt()),
        secondary: Color = Color.fromHex(0xFF03DAC6.toInt()),
    ): Theme = Theme.material(id = id, accent = primary, secondary = secondary)

    public fun randomLight(): Theme = Theme.material(
        id = "materialRandomLight-${Random.nextInt()}",
        accent = randomAccent(),
        secondary = randomAccent(),
    )

    public fun randomDark(): Theme = Theme.material(
        id = "materialRandomDark-${Random.nextInt()}",
        background = Color.fromHexString("#121212"),
        accent = randomAccent(),
        secondary = randomAccent(),
    )

    public fun random(): Theme = if (Random.nextBoolean()) randomLight() else randomDark()
}

/** A saturated mid-tone in a random hue - the shape of a usable brand color. */
internal fun randomAccent(random: Random = Random): Color = HSVColor(
    hue = random.nextFloat().turns,
    saturation = random.nextFloat() * 0.4f + 0.55f,
    value = random.nextFloat() * 0.35f + 0.5f,
).toRGB()

public fun Theme.randomTitleFontSettings(): Theme = copy(
    id = "${Random.nextInt()}",
    semanticOverrides = SemanticOverrides(
        HeaderSemantic.override {
            val old = this@randomTitleFontSettings[HeaderSemantic]
            old.theme.copy(
                id = id,
                font = font.copy(
                    font = systemDefaultFont,
                    weight = if (Random.nextBoolean()) 700 else 500,
                    allCaps = Random.nextBoolean()
                )
            ).withoutBack
        }
    )
)

public fun Theme.randomElevationAndCorners(): Theme = when (Random.nextInt(0, 3)) {
    0 -> copy(
        id = "${Random.nextInt()}",
        elevation = Random.nextInt(2, 4).dp,
        cornerRadii = CornerRadii.RatioOfSpacing(Random.nextFloat())
    )

    1 -> copy(
        id = "${Random.nextInt()}",
        outlineWidth = Random.nextInt(1, 4).dp,
        cornerRadii = CornerRadii.RatioOfSpacing(Random.nextFloat())
    )

    else -> copy(
        id = "${Random.nextInt()}",
        outlineWidth = Random.nextInt(1, 4).dp,
        elevation = Random.nextInt(2, 4).dp,
        cornerRadii = CornerRadii.RatioOfSpacing(Random.nextFloat())
    )
}
