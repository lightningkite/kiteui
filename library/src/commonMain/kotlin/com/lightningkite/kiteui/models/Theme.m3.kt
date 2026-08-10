package com.lightningkite.kiteui.models

import com.lightningkite.kiteui.views.l2.LabelSemantic
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

/**
 * Material 3: one source color grown into a tonal palette, surfaces that separate by tone rather
 * than by shadow, and pill-shaped everything.
 *
 * What distinguishes it from [Theme.material], beyond the corner radii:
 *
 * - **Every color is a tone of the accent.** Material 3 takes one source color and generates a
 *   ramp - the brand at tone 40 in light and tone 80 in dark, containers at 90 and 30, and even the
 *   grays as a low-chroma ramp in the same hue. That last part is why a Material 3 screen looks
 *   tinted all over rather than colored only on its controls, and this theme reproduces it.
 * - **Tone replaces elevation.** Material 2 stacked surfaces with shadows; Material 3 stacks them
 *   with a ladder of surface containers and reserves shadow for things that genuinely float. A card
 *   here casts nothing.
 * - **No caps, and no app bar color.** Material 3 sets actions in sentence case and leaves the top
 *   bar the same tone as the page. Both are direct reversals of Material 2, and both are what date
 *   a screen to one or the other.
 *
 * ```kotlin
 * val light = Theme.material3()
 * val dark = Theme.material3("app-dark", background = Color.fromHexString("#141218"))
 * ```
 *
 * @param id Unique identifier for the theme; must not collide with another theme in the app.
 * @param background Sets the polarity and the bottom rung of the surface ladder. Material 3
 *   generates its own surfaces, so what is actually painted is the neutral tone at this
 *   brightness - a page passed pure white still comes out faintly tinted toward [accent], which is
 *   the look the tonal palette exists to produce.
 * @param accent The source color the whole palette is grown from: the brand, the neutrals, the
 *   selection indicator and the focus ring. Its own lightness is discarded - only its hue and
 *   chroma survive, since the palette assigns tones itself.
 * @param secondary Source for the selection indicator and for emphasis stacked on the primary.
 *   Defaults to the accent at reduced chroma, which is how Material 3 derives it.
 * @param title Font used for headings.
 * @param body Font used for everything else.
 */
public fun Theme.Companion.material3(
    id: String = "material3",
    background: Color = Color.fromHexString("#FDF7FF"),
    accent: Color = Color.fromHexString("#6750A4"),
    secondary: Color = accent.toHSP()
        .let { HSPColor(hue = it.hue, saturation = it.saturation * 0.35f, brightness = it.brightness) }.toRGB(),
    title: FontAndStyle = FontAndStyle(),
    body: FontAndStyle = FontAndStyle(),
): Theme {
    val dark = background.perceivedBrightness < 0.5f
    val sourceHue = accent.toHSP().hue

    /**
     * A tone of an accent palette. Material 3 measures tone as perceptual lightness and keeps the
     * source's hue and chroma, so a "tone 40 primary" is the same color family at a fixed
     * lightness - which is what lets one source color produce a whole coherent set.
     */
    fun Color.tone(tone: Float): Color = toHSP()
        .let { HSPColor(hue = it.hue, saturation = it.saturation, brightness = (tone / 100f).coerceIn(0f, 1f)) }.toRGB()

    /** The neutral palette: the source hue at the trace of chroma Material 3 keeps in its grays. */
    fun neutral(tone: Float): Color =
        HSPColor(hue = sourceHue, saturation = 0.04f, brightness = (tone / 100f).coerceIn(0f, 1f)).toRGB()

    /** The neutral-variant palette - twice the chroma, used for outlines and secondary text. */
    fun neutralVariant(tone: Float): Color =
        HSPColor(hue = sourceHue, saturation = 0.09f, brightness = (tone / 100f).coerceIn(0f, 1f)).toRGB()

    // The surface-container ladder, anchored at whatever tone the caller's canvas sits at. Material
    // 3's own ladder steps down 2 tones at a time in light and up 4 at a time in dark, because
    // equal-looking steps are wider at the dark end.
    val baseTone = background.perceivedBrightness * 100f
    fun surfaceAt(step: Int): Color = neutral(baseTone + step * if (dark) 4f else -2f)

    val surface = surfaceAt(0)
    val surfaceContainerLow = surfaceAt(1)
    val surfaceContainer = surfaceAt(2)
    val surfaceContainerHigh = surfaceAt(3)

    val onSurface = neutral(if (dark) 90f else 10f)
    val onSurfaceVariant = neutralVariant(if (dark) 80f else 30f)
    val outline = neutralVariant(if (dark) 60f else 50f)
    val outlineVariant = neutralVariant(if (dark) 30f else 80f)

    val primary = accent.tone(if (dark) 80f else 40f)
    val onPrimary = accent.tone(if (dark) 20f else 100f)
    // Material 3's hierarchy runs filled (primary) -> tonal (secondary container) -> outlined, so
    // stacked emphasis steps down to the secondary container rather than to the primary one.
    val secondaryContainer = secondary.tone(if (dark) 30f else 90f)
    val onSecondaryContainer = secondary.tone(if (dark) 90f else 10f)

    val error = if (dark) Color.fromHexString("#F2B8B5") else Color.fromHexString("#B3261E")
    val warning = if (dark) Color.fromHexString("#F5C26B") else Color.fromHexString("#7A5900")
    val affirmative = if (dark) Color.fromHexString("#7ADFA0") else Color.fromHexString("#146C2E")

    /**
     * A state layer: the overlay Material 3 paints over a container to show interaction, using that
     * container's own "on" color at a fixed opacity.
     *
     * [on] is passed rather than made a receiver on purpose. As an extension on `Theme` the bare
     * name `background` inside this body binds to the *factory's* `background` parameter, not to
     * the theme's, and every state layer silently reads off the canvas instead of the surface it
     * is drawn on.
     */
    fun stateLayer(on: Theme, opacity: Float): Color =
        Color.interpolate(on.background.closestColor(), on.foreground.closestColor(), opacity)

    /**
     * Supporting text. On one of the palette's own surfaces this is the real `onSurfaceVariant`
     * role rather than the body color at reduced opacity - that distinction is a genuine Material 3
     * one. On anything else (a filled button, a status chip) there is no such role, so it falls
     * back to taking as much contrast off the foreground as the fill can spare.
     */
    fun supporting(theme: Theme): Color {
        val behind = theme.background.closestColor()
        return if (onSurfaceVariant contrastAgainst behind >= Color.WCAG_AA_NORMAL_TEXT_CONTRAST_RATIO) onSurfaceVariant
        else Color.mutedButLegible(theme.foreground.closestColor(), behind)
    }

    /** A filled container - the filled button, and every status role that borrows its shape. */
    fun Semantic.filled(theme: Theme, fill: Color, on: Color = fill.maximallyContrastingForeground): ThemeAndBack =
        theme.alter(
            cascading = false,
            outlineWidth = 0.dp,
            elevation = 0.dp,
            // Material 3's fully-rounded button is as recognizable as Material 2's caps were.
            cornerRadii = CornerRadii.Fixed(20.dp),
        ).withBack(background = fill, foreground = on, outline = fill)

    /**
     * The Material 3 type scale. Headlines are regular weight, not bold - Material 3 builds
     * hierarchy from size and lets weight mark labels instead, which is the opposite of Material 2.
     */
    fun headingFont(level: Int): FontAndStyle = when (level) {
        1 -> title.copy(size = 2.rem, weight = 400, lineSpacingMultiplier = 1.25)
        2 -> title.copy(size = 1.75.rem, weight = 400, lineSpacingMultiplier = 1.29)
        3 -> title.copy(size = 1.5.rem, weight = 400, lineSpacingMultiplier = 1.33)
        4 -> title.copy(size = 1.375.rem, weight = 400, lineSpacingMultiplier = 1.27)
        5 -> title.copy(size = 1.rem, weight = 500, additionalLetterSpacing = 0.009.rem, lineSpacingMultiplier = 1.5)
        else -> title.copy(size = 0.875.rem, weight = 500, additionalLetterSpacing = 0.006.rem, lineSpacingMultiplier = 1.43)
    }

    return Theme(
        id = id,
        // bodyLarge, tracking included - Material 3 tracks its body copy out slightly, which is part
        // of why its screens read as airy.
        font = body.copy(size = 1.rem, additionalLetterSpacing = 0.031.rem, lineSpacingMultiplier = 1.5),
        elevation = 0.dp,
        cornerRadii = CornerRadii.Fixed(12.dp),
        cornerShape = CornerShape.Circular,
        gap = 0.75.rem,
        padding = Edges(horizontal = 1.5.rem, vertical = 0.625.rem),
        foreground = onSurface,
        background = surface,
        outline = outlineVariant,
        outlineWidth = 0.dp,
        separatorOverride = outlineVariant,
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
                ).withBack(background = surface, foreground = onSurface)
            },
            MainContentSemantic.override {
                it.alter(cascading = false, cornerRadii = CornerRadii.Fixed(0.dp), outlineWidth = 0.dp, elevation = 0.dp)
                    .withBack(background = surface, foreground = onSurface)
            },
            // A small top app bar is the page's own tone. Material 3 only tints it once content
            // scrolls beneath, and never with the brand color.
            BarSemantic.override {
                it.alter(
                    cascading = false,
                    cornerRadii = CornerRadii.Fixed(0.dp),
                    outlineWidth = 0.dp,
                    elevation = 0.dp,
                    padding = Edges(horizontal = 0.75.rem, vertical = 0.5.rem),
                    gap = 0.5.rem,
                ).withBack(background = surface, foreground = onSurface)
            },
            NavSemantic.override {
                it.alter(
                    cascading = false,
                    cornerRadii = CornerRadii.Fixed(0.dp),
                    outlineWidth = 0.dp,
                    elevation = 0.dp,
                    padding = Edges(0.75.rem),
                    gap = 0.25.rem,
                ).withBack(background = surfaceContainerLow, foreground = onSurface)
            },

            // A filled card: no shadow, no border, separated purely by tone. Nesting keeps climbing
            // the container ladder rather than stacking shadows.
            CardSemantic.override {
                val next = when (it.background.closestColor()) {
                    surfaceContainerHigh -> surfaceContainerLow
                    surfaceContainer -> surfaceContainerHigh
                    surfaceContainerLow -> surfaceContainer
                    else -> surfaceContainerLow
                }
                it.alter(cascading = false, outlineWidth = 0.dp, elevation = 0.dp, padding = Edges(1.rem))
                    .withBack(background = next, foreground = onSurface)
            },
            GroupSemantic.override { it[CardSemantic] },
            InsetSemantic.override {
                it.alter(cascading = false, outlineWidth = 0.dp, elevation = 0.dp)
                    .withBack(background = stateLayer(it, 0.07f))
            },
            EmbeddedSemantic.override {
                it.alter(cascading = false, outlineWidth = 0.dp, elevation = 0.dp)
                    .withBack(background = stateLayer(it, 0.07f))
            },
            ListSemantic.override { it.withoutBack(cascading = false, gap = 0.25.rem) },

            // The two shapes Material 3 still lifts, and the only place its extra-large radius
            // appears.
            DialogSemantic.override {
                it.alter(
                    cascading = false,
                    outlineWidth = 0.dp,
                    elevation = 6.dp,
                    cornerRadii = CornerRadii.Fixed(28.dp),
                    padding = Edges(1.5.rem),
                ).withBack(background = surfaceContainerHigh, foreground = onSurface)
            },
            PopoverSemantic.override {
                it.alter(
                    cascading = false,
                    outlineWidth = 0.dp,
                    elevation = 3.dp,
                    cornerRadii = CornerRadii.Fixed(4.dp),
                    padding = Edges(vertical = 0.5.rem, horizontal = 0.dp),
                    gap = 0.dp,
                ).withBack(background = surfaceContainer, foreground = onSurface)
            },
            DismissSemantic.override {
                it.withBack(
                    cascading = false,
                    gap = 0.dp,
                    padding = Edges.ZERO,
                    cornerRadii = CornerRadii.Fixed(0.dp),
                    background = neutral(if (dark) 4f else 10f).applyAlpha(0.4f),
                )
            },

            // ---- Typography ------------------------------------------------------------------

            HeaderSemantic.override { it.withoutBack(font = headingFont(5)) },
            override<HeaderSizeSemantic> { it.withoutBack(font = headingFont(level)) },
            // onSurfaceVariant is Material 3's supporting-text role; it is a real palette color
            // rather than the body color at reduced opacity.
            SubtextSemantic.override {
                it.withoutBack(
                    font = it.font.copy(size = 0.75.rem, additionalLetterSpacing = 0.025.rem),
                    foreground = supporting(it),
                )
            },
            LabelSemantic.override {
                it.withoutBack(
                    font = it.font.copy(size = 0.75.rem, weight = 500, additionalLetterSpacing = 0.031.rem),
                    foreground = supporting(it),
                )
            },
            TableHeaderSemantic.override {
                it.withoutBack(
                    font = it.font.copy(size = 0.875.rem, weight = 500, additionalLetterSpacing = 0.006.rem),
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
                it.alter(cascading = false, outlineWidth = 0.dp, elevation = 0.dp, cornerRadii = CornerRadii.Fixed(8.dp))
                    .withBack(
                        font = it.font.copy(font = systemDefaultFixedWidthFont, size = it.font.size * 0.9f),
                        background = stateLayer(it, 0.08f),
                    )
            },

            // ---- Emphasis and status ---------------------------------------------------------

            ImportantSemantic.override {
                if (it.background.closestColor() == primary) filled(it, secondaryContainer, onSecondaryContainer)
                else filled(it, primary, onPrimary)
            },
            WarningSemantic.override { filled(it, warning) },
            DangerSemantic.override { filled(it, error) },
            AffirmativeSemantic.override { filled(it, affirmative) },
            ErrorSemantic.override { it.withoutBack(foreground = Color.legibleOn(error, it.background.closestColor())) },

            // ---- Interaction -----------------------------------------------------------------

            // The outlined text field: Material 3's default, a hairline box in the outline role
            // that thickens to the primary on focus.
            FieldSemantic.override {
                it.alter(
                    cascading = false,
                    outlineWidth = 1.dp,
                    outline = outline,
                    elevation = 0.dp,
                    cornerRadii = CornerRadii.Fixed(4.dp),
                    padding = Edges(horizontal = 1.rem, vertical = 0.75.rem),
                ).withBack(background = it.background, foreground = onSurface)
            },
            InvalidSemantic.override { it.withBack(cascading = false, outlineWidth = 2.dp, outline = error) },
            FocusSemantic.override { it.withBack(cascading = false, outlineWidth = 2.dp, outline = primary) },

            // The navigation indicator: a fully-rounded secondary-container pill behind the active
            // destination. Nothing else in the system says "Material 3" as loudly.
            SelectedSemantic.override {
                it.alter(
                    cascading = false,
                    outlineWidth = 0.dp,
                    elevation = 0.dp,
                    cornerRadii = CornerRadii.Fixed(20.dp),
                ).withBack(
                    background = secondaryContainer,
                    foreground = onSecondaryContainer,
                    font = it.font.copy(weight = 500),
                    iconOverride = null,
                )
            },
            UnselectedSemantic.override {
                it.alter(
                    outlineWidth = 0.dp,
                    cornerRadii = CornerRadii.Fixed(20.dp),
                    foreground = onSurfaceVariant,
                    iconOverride = null,
                ).withoutBackButPadding
            },

            HoverSemantic.override { it.withBack(background = stateLayer(it, 0.08f)) },
            DownSemantic.override { it.withBack(background = stateLayer(it, 0.12f)) },
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
                    foreground = if (it.background.closestColor() == primary) onPrimary else primary,
                    iconOverride = null,
                )
            },
        ),
    )
}

@Deprecated(
    "Use Theme.material3, which now takes the standard (id, background, accent, secondary, title, " +
        "body) parameters; reach for Theme.copy for the rest.",
    ReplaceWith("Theme.material3(id = id, accent = primary, secondary = secondary)")
)
public object M3Theme {
    public operator fun invoke(
        id: String,
        primary: Color = Color.fromHex(0xFF6200EE.toInt()),
        secondary: Color = Color.fromHex(0xFF03DAC6.toInt()),
    ): Theme = Theme.material3(id = id, accent = primary, secondary = secondary)

    public fun randomLight(): Theme =
        Theme.material3(id = "m3RandomLight-${Random.nextInt()}", accent = randomAccent())

    public fun randomDark(): Theme = Theme.material3(
        id = "m3RandomDark-${Random.nextInt()}",
        background = Color.fromHexString("#141218"),
        accent = randomAccent(),
    )

    public fun random(): Theme = if (Random.nextBoolean()) randomLight() else randomDark()
}
